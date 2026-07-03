package com.spam.financialaccounting.application.usecases.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.AccountPostingTotals;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.dto.TrialBalanceDTO;

/**
 * Builds a trial balance as of a date: for each ledger account it takes posted
 * debits minus posted credits, counting only vouchers dated on or before
 * {@code asOfDate}, then puts the result in a debit or credit column. The books
 * balance only when total debits equal total credits.
 *
 * <p>Opening balances aren't read from {@code FASubGroup.S_OPBAL}; they're posted
 * as an Opening Balance Journal Voucher and so are already in the posting totals.
 * Reading S_OPBAL here too would double-count them.
 *
 * <p>The database sums the postings (one row per account), so the report doesn't
 * load every journal line into memory, and future-dated vouchers don't sneak into
 * a historical report.
 */
@Service
public class GetTrialBalance {

    private final FASubGroupRepository faSubGroupRepository;
    private final JournalDetailRepository journalDetailRepository;

    public GetTrialBalance(FASubGroupRepository faSubGroupRepository,
            JournalDetailRepository journalDetailRepository) {
        this.faSubGroupRepository = faSubGroupRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public TrialBalanceDTO execute(LocalDate asOfDate) {
        List<FASubGroup> accounts = faSubGroupRepository.findAll();
        List<AccountPostingTotals> postings = journalDetailRepository.sumPostingsAsOf(asOfDate);

        Map<String, BigDecimal> debitByAccount = new HashMap<>();
        Map<String, BigDecimal> creditByAccount = new HashMap<>();
        for (AccountPostingTotals p : postings) {
            debitByAccount.put(p.accountCode(), p.totalDebit());
            creditByAccount.put(p.accountCode(), p.totalCredit());
        }

        List<TrialBalanceDTO.Row> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (FASubGroup acc : accounts) {
            // Treat everything as a debit-positive signed balance. Opening
            // balances are already here via the Opening Balance voucher.
            BigDecimal postedDebit = debitByAccount.getOrDefault(acc.getSCode(), BigDecimal.ZERO);
            BigDecimal postedCredit = creditByAccount.getOrDefault(acc.getSCode(), BigDecimal.ZERO);
            BigDecimal balance = postedDebit.subtract(postedCredit);

            BigDecimal debitCol = BigDecimal.ZERO;
            BigDecimal creditCol = BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                debitCol = balance;
            } else {
                creditCol = balance.negate();
            }

            totalDebit = totalDebit.add(debitCol);
            totalCredit = totalCredit.add(creditCol);
            rows.add(new TrialBalanceDTO.Row(acc.getSCode(), acc.getSDesc(), debitCol, creditCol));
        }

        boolean balanced = totalDebit.compareTo(totalCredit) == 0;
        return new TrialBalanceDTO(asOfDate, rows, totalDebit, totalCredit, balanced);
    }
}
