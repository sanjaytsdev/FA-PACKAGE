package com.spam.financialaccounting.application.usecases.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.AccountClass;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.AccountPostingTotals;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.dto.ProfitAndLossDTO;

/**
 * Builds a Profit &amp; Loss (income statement) as of a reporting date: revenue
 * (Income accounts) minus expenses (Expense accounts), counting only vouchers
 * dated on or before {@code asOfDate}.
 *
 * <p>Like the trial balance, this is a cumulative "as of date" view. There's no
 * period-closing step yet, so net profit is the running result from when the
 * books opened up to the report date. The database sums the postings (one row per
 * account), opening balances included since they come through the normal posting
 * path.
 */
@Service
public class GetProfitAndLoss {

    private static final AccountPostingTotals EMPTY =
            new AccountPostingTotals(null, BigDecimal.ZERO, BigDecimal.ZERO);

    private final FASubGroupRepository faSubGroupRepository;
    private final JournalDetailRepository journalDetailRepository;

    public GetProfitAndLoss(FASubGroupRepository faSubGroupRepository,
            JournalDetailRepository journalDetailRepository) {
        this.faSubGroupRepository = faSubGroupRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public ProfitAndLossDTO execute(LocalDate asOfDate) {
        List<FASubGroup> accounts = faSubGroupRepository.findAll();
        Map<String, AccountPostingTotals> totals = indexByCode(journalDetailRepository.sumPostingsAsOf(asOfDate));

        List<ProfitAndLossDTO.LineItem> revenue = new ArrayList<>();
        List<ProfitAndLossDTO.LineItem> expenses = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (FASubGroup acc : accounts) {
            AccountClass cls = AccountClass.fromSType(acc.getSType());
            if (cls != AccountClass.INCOME && cls != AccountClass.EXPENSE) {
                continue; // balance-sheet accounts don't show up in the P&L
            }
            AccountPostingTotals t = totals.getOrDefault(acc.getSCode(), EMPTY);

            if (cls == AccountClass.INCOME) {
                // Income is credit-normal: balance is credits minus debits.
                BigDecimal amount = t.totalCredit().subtract(t.totalDebit());
                totalRevenue = totalRevenue.add(amount);
                if (amount.signum() != 0) {
                    revenue.add(new ProfitAndLossDTO.LineItem(acc.getSCode(), acc.getSDesc(), amount));
                }
            } else {
                // Expense is debit-normal.
                BigDecimal amount = t.totalDebit().subtract(t.totalCredit());
                totalExpenses = totalExpenses.add(amount);
                if (amount.signum() != 0) {
                    expenses.add(new ProfitAndLossDTO.LineItem(acc.getSCode(), acc.getSDesc(), amount));
                }
            }
        }

        BigDecimal netProfit = totalRevenue.subtract(totalExpenses);
        return new ProfitAndLossDTO(asOfDate, revenue, expenses, totalRevenue, totalExpenses, netProfit);
    }

    private Map<String, AccountPostingTotals> indexByCode(List<AccountPostingTotals> postings) {
        Map<String, AccountPostingTotals> map = new HashMap<>();
        for (AccountPostingTotals p : postings) {
            map.put(p.accountCode(), p);
        }
        return map;
    }
}
