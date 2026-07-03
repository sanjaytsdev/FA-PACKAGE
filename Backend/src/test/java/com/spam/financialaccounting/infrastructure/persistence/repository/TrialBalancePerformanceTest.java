package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.spam.financialaccounting.domain.repository.AccountPostingTotals;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailRowMapper;

/**
 * Big-dataset benchmark for the trial-balance rollup (sumPostingsAsOf).
 *
 * Seeds a large, balanced ledger and checks the date-scoped rollup:
 *  (a) stays correct at scale,
 *  (b) collapses every detail line to one row per account in the DB
 *      (never pulls individual detail rows into memory), and
 *  (c) finishes well inside a generous time budget, so we'd catch a slide back
 *      to full-table scans now that JournalMaster(J_DATE) and
 *      JournalDetail(J_CODE) are indexed.
 *
 * Dataset size is tunable: -Dtb.benchmark.vouchers=1000000 pushes it to two
 * million detail lines for local runs. The default keeps CI fast.
 */
@JdbcTest
@Import({ JournalDetailRepositoryJDBC.class, JournalDetailRowMapper.class })
public class TrialBalancePerformanceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalDetailRepositoryJDBC repository;

    /** Each voucher = 1 DR line + 1 CR line, so detail rows = 2 x VOUCHERS. */
    private static final int VOUCHERS = Integer.getInteger("tb.benchmark.vouchers", 50_000);
    private static final int DEBIT_ACCOUNTS = 50;
    private static final BigDecimal LINE_AMOUNT = new BigDecimal("10.000");
    private static final String CREDIT_ACCT = "90000";
    /** Vouchers sit one hour apart starting here, so the dates span years. */
    private static final LocalDateTime BASE = LocalDateTime.of(2020, 1, 1, 0, 0);

    @BeforeEach
    void seedLargeLedger() {
        jdbcTemplate.execute("DELETE FROM JournalDetail");
        jdbcTemplate.execute("DELETE FROM JournalMaster");
        jdbcTemplate.execute("DELETE FROM FASubGroup");
        jdbcTemplate.execute("DELETE FROM FAGroup");

        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES ('01','Assets','0',0)");
        for (int a = 0; a < DEBIT_ACCOUNTS; a++) {
            insertAccount(debitAccount(a), "DR");
        }
        insertAccount(CREDIT_ACCT, "CR");

        long start = System.nanoTime();
        bulkInsertVouchers();
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("[trial-balance benchmark] seeded %,d vouchers (%,d detail lines) in %,d ms%n",
                VOUCHERS, 2L * VOUCHERS, ms);
    }

    private String debitAccount(int a) {
        return String.format("D%04d", a); // 5 chars, fits S_CODE VARCHAR(5)
    }

    private void insertAccount(String code, String drCr) {
        jdbcTemplate.update(
                "INSERT INTO FASubGroup (S_CODE,S_DESC,A_CODE,S_TYPE,S_OPBAL,S_DRCR,S_FLAG) "
                        + "VALUES (?,?, '01','00',0,?, 'T')",
                code, code, drCr);
    }

    private void bulkInsertVouchers() {
        final int chunk = 5_000;
        List<Object[]> masters = new ArrayList<>(chunk);
        List<Object[]> details = new ArrayList<>(2 * chunk);
        for (int i = 0; i < VOUCHERS; i++) {
            String jId = String.format("V%08d", i);
            String date = BASE.plusHours(i).toString();
            masters.add(new Object[] { jId, date, LINE_AMOUNT });
            details.add(new Object[] { jId, debitAccount(i % DEBIT_ACCOUNTS), "DR", LINE_AMOUNT });
            details.add(new Object[] { jId, CREDIT_ACCT, "CR", LINE_AMOUNT });
            if (masters.size() == chunk || i == VOUCHERS - 1) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO JournalMaster (J_ID,J_DOC,J_DATE,J_AMOUNT,J_NARR) VALUES (?,'JV',?,?,'')",
                        masters);
                jdbcTemplate.batchUpdate(
                        "INSERT INTO JournalDetail (J_ID,J_CODE,J_DRCR,J_AMOUNT) VALUES (?,?,?,?)",
                        details);
                masters.clear();
                details.clear();
            }
        }
    }

    /** Count of vouchers dated before midnight after asOfDate (hourly spacing from BASE). */
    private long vouchersOnOrBefore(LocalDate asOfDate) {
        LocalDateTime upperExclusive = asOfDate.plusDays(1).atStartOfDay();
        long hours = Duration.between(BASE, upperExclusive).toHours();
        return Math.max(0, Math.min(VOUCHERS, hours));
    }

    @Test
    @DisplayName("Aggregates a large ledger correctly and quickly, one row per account")
    void largeLedger_aggregatesCorrectlyAndFast() {
        // Selective historical date: only a small slice of vouchers qualify, so the
        // J_DATE index can trim the scan instead of reading the whole ledger.
        LocalDate selective = LocalDate.of(2020, 2, 1);
        long expectedSelective = vouchersOnOrBefore(selective);

        long t0 = System.nanoTime();
        List<AccountPostingTotals> selectiveTotals = repository.sumPostingsAsOf(selective);
        long selectiveMs = (System.nanoTime() - t0) / 1_000_000;

        // Inclusive date: just past the last voucher so every voucher qualifies,
        // i.e. the full rollup. (Computed from the dataset since the hourly spacing
        // means the date range grows with VOUCHERS.)
        LocalDate inclusive = BASE.plusHours(VOUCHERS).toLocalDate().plusDays(1);
        long expectedFullCount = vouchersOnOrBefore(inclusive);
        long t1 = System.nanoTime();
        List<AccountPostingTotals> allTotals = repository.sumPostingsAsOf(inclusive);
        long fullMs = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("[trial-balance benchmark] selective(%,d vouchers)=%,d ms | full(%,d vouchers)=%,d ms%n",
                expectedSelective, selectiveMs, expectedFullCount, fullMs);

        // (a) Correct at scale: grand totals match the seeded amounts and balance.
        BigDecimal expectedSelectiveAmount = LINE_AMOUNT.multiply(BigDecimal.valueOf(expectedSelective));
        assertThat(grandDebit(selectiveTotals)).isEqualByComparingTo(expectedSelectiveAmount);
        assertThat(grandCredit(selectiveTotals)).isEqualByComparingTo(expectedSelectiveAmount);

        BigDecimal expectedFull = LINE_AMOUNT.multiply(BigDecimal.valueOf(expectedFullCount));
        assertThat(grandDebit(allTotals)).isEqualByComparingTo(expectedFull);
        assertThat(grandCredit(allTotals)).isEqualByComparingTo(expectedFull);
        assertThat(creditOf(allTotals, CREDIT_ACCT)).isEqualByComparingTo(expectedFull);

        // (b) DB-side aggregation: one row per account, NOT one per detail line.
        assertThat(allTotals).hasSize(DEBIT_ACCOUNTS + 1);

        // (c) Still fast: finishes well within budget even over 2 x VOUCHERS lines.
        assertThat(fullMs).isLessThan(10_000);
        assertThat(selectiveMs).isLessThan(10_000);
    }

    private BigDecimal grandDebit(List<AccountPostingTotals> totals) {
        return totals.stream().map(AccountPostingTotals::totalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal grandCredit(List<AccountPostingTotals> totals) {
        return totals.stream().map(AccountPostingTotals::totalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal creditOf(List<AccountPostingTotals> totals, String code) {
        return totals.stream().filter(t -> t.accountCode().equals(code))
                .map(AccountPostingTotals::totalCredit).findFirst().orElse(BigDecimal.ZERO);
    }
}
