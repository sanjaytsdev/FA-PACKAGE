package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
 * Integration tests for the date-scoped posting rollup behind the trial
 * balance. Runs the real JournalDetail↔JournalMaster join against H2 with the
 * schema.sql tables in place (we DELETE instead of dropping, to keep the FKs
 * intact). Confirms "as of date" filtering is right: past and same-day vouchers
 * are summed, future-dated ones are left out.
 */
@JdbcTest
@Import({ JournalDetailRepositoryJDBC.class, JournalDetailRowMapper.class })
public class JournalDetailRepositoryAsOfDateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalDetailRepositoryJDBC repository;

    private static final String CASH = "10001";
    private static final String CAPITAL = "30001";
    private static final String INCOME = "40001";

    @BeforeEach
    void seed() {
        // Clear in FK-safe order, then rebuild the reference data the FKs require.
        jdbcTemplate.execute("DELETE FROM JournalDetail");
        jdbcTemplate.execute("DELETE FROM JournalMaster");
        jdbcTemplate.execute("DELETE FROM FASubGroup");
        jdbcTemplate.execute("DELETE FROM FAGroup");

        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES ('01', 'Assets', '0', 0)");
        insertSubGroup(CASH, "Cash", "DR");
        insertSubGroup(CAPITAL, "Capital", "CR");
        insertSubGroup(INCOME, "Sales Income", "CR");

        // V_PAST  — 2026-01-15: opening trade, well before the report date.
        insertVoucher("V000000001", LocalDateTime.of(2026, 1, 15, 9, 0));
        insertDetail("V000000001", CASH, "DR", "1000");
        insertDetail("V000000001", CAPITAL, "CR", "1000");

        // V_ASOF  — 2026-03-31 14:30: a sale on the report date itself (later in the day).
        insertVoucher("V000000002", LocalDateTime.of(2026, 3, 31, 14, 30));
        insertDetail("V000000002", CASH, "DR", "500");
        insertDetail("V000000002", INCOME, "CR", "500");

        // V_FUTURE — 2026-04-01: must never affect a 2026-03-31 report.
        insertVoucher("V000000003", LocalDateTime.of(2026, 4, 1, 9, 0));
        insertDetail("V000000003", CASH, "DR", "999");
        insertDetail("V000000003", INCOME, "CR", "999");
    }

    private void insertSubGroup(String code, String desc, String drCr) {
        jdbcTemplate.update(
                "INSERT INTO FASubGroup (S_CODE, S_DESC, A_CODE, S_TYPE, S_OPBAL, S_DRCR, S_FLAG) "
                        + "VALUES (?, ?, '01', '00', 0, ?, 'T')",
                code, desc, drCr);
    }

    private void insertVoucher(String jId, LocalDateTime date) {
        jdbcTemplate.update(
                "INSERT INTO JournalMaster (J_ID, J_DOC, J_DATE, J_AMOUNT, J_NARR) VALUES (?, 'JV', ?, 0, '')",
                jId, date.toString());
    }

    private void insertDetail(String jId, String code, String drCr, String amount) {
        jdbcTemplate.update(
                "INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) VALUES (?, ?, ?, ?)",
                jId, code, drCr, new BigDecimal(amount));
    }

    private BigDecimal debit(List<AccountPostingTotals> totals, String code) {
        return find(totals, code).map(AccountPostingTotals::totalDebit).orElse(BigDecimal.ZERO);
    }

    private BigDecimal credit(List<AccountPostingTotals> totals, String code) {
        return find(totals, code).map(AccountPostingTotals::totalCredit).orElse(BigDecimal.ZERO);
    }

    private Optional<AccountPostingTotals> find(List<AccountPostingTotals> totals, String code) {
        return totals.stream().filter(t -> t.accountCode().equals(code)).findFirst();
    }

    @Test
    @DisplayName("[T01] Historical date sums only vouchers up to that date")
    void historicalDate_SumsOnlyEarlierVouchers() {
        // As of 2026-02-01: only V_PAST has been posted.
        List<AccountPostingTotals> totals = repository.sumPostingsAsOf(LocalDate.of(2026, 2, 1));

        assertThat(debit(totals, CASH)).isEqualByComparingTo("1000");
        assertThat(credit(totals, CAPITAL)).isEqualByComparingTo("1000");
        assertThat(find(totals, INCOME)).isEmpty(); // no income posted yet
    }

    @Test
    @DisplayName("[T02] Report date includes same-day vouchers regardless of time of day")
    void asOfDate_IncludesSameDayEntries() {
        // As of 2026-03-31: V_PAST + V_ASOF (posted 14:30 that day), V_FUTURE excluded.
        List<AccountPostingTotals> totals = repository.sumPostingsAsOf(LocalDate.of(2026, 3, 31));

        assertThat(debit(totals, CASH)).isEqualByComparingTo("1500"); // 1000 + 500
        assertThat(credit(totals, CAPITAL)).isEqualByComparingTo("1000");
        assertThat(credit(totals, INCOME)).isEqualByComparingTo("500"); // not 1499
    }

    @Test
    @DisplayName("[T03] Future-dated vouchers are excluded from a historical report")
    void futureVouchers_AreExcluded() {
        List<AccountPostingTotals> asOfReport = repository.sumPostingsAsOf(LocalDate.of(2026, 3, 31));
        List<AccountPostingTotals> afterFuture = repository.sumPostingsAsOf(LocalDate.of(2026, 12, 31));

        // The 999 future sale is gone on 2026-03-31 but shows up once the report
        // date moves past it, so the exclusion is purely about the date.
        assertThat(credit(asOfReport, INCOME)).isEqualByComparingTo("500");
        assertThat(debit(asOfReport, CASH)).isEqualByComparingTo("1500");

        assertThat(credit(afterFuture, INCOME)).isEqualByComparingTo("1499"); // 500 + 999
        assertThat(debit(afterFuture, CASH)).isEqualByComparingTo("2499"); // 1000 + 500 + 999
    }

    @Test
    @DisplayName("[T04] Aggregation returns one row per account, not per detail line")
    void aggregatesByAccount() {
        List<AccountPostingTotals> totals = repository.sumPostingsAsOf(LocalDate.of(2026, 12, 31));

        // CASH is debited by three separate vouchers but collapses to a single row.
        assertThat(totals.stream().filter(t -> t.accountCode().equals(CASH)).count()).isEqualTo(1);
    }
}
