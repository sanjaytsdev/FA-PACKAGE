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
import com.spam.financialaccounting.presentation.dto.BalanceSheetDTO;

/**
 * Builds a Balance Sheet as of a reporting date. Assets, liabilities and equity
 * each show on their natural side, counting only vouchers dated on or before
 * {@code asOfDate}.
 *
 * <p>There's no period-closing step yet, so earnings to date (revenue minus
 * expenses, i.e. the P&amp;L net result) go into equity as one "Net Income" line.
 * That keeps the accounting equation true ({@code totalAssets == totalLiabilities
 * + totalEquity}), the same way current earnings sit inside equity until they're
 * closed to retained earnings.
 */
@Service
public class GetBalanceSheet {

    /** Made-up line that carries unclosed earnings into the equity section. */
    static final String NET_INCOME_CODE = "NET_INCOME";

    private static final AccountPostingTotals EMPTY =
            new AccountPostingTotals(null, BigDecimal.ZERO, BigDecimal.ZERO);

    private final FASubGroupRepository faSubGroupRepository;
    private final JournalDetailRepository journalDetailRepository;

    public GetBalanceSheet(FASubGroupRepository faSubGroupRepository,
            JournalDetailRepository journalDetailRepository) {
        this.faSubGroupRepository = faSubGroupRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public BalanceSheetDTO execute(LocalDate asOfDate) {
        List<FASubGroup> accounts = faSubGroupRepository.findAll();
        Map<String, AccountPostingTotals> totals = indexByCode(journalDetailRepository.sumPostingsAsOf(asOfDate));

        List<BalanceSheetDTO.LineItem> assets = new ArrayList<>();
        List<BalanceSheetDTO.LineItem> liabilities = new ArrayList<>();
        List<BalanceSheetDTO.LineItem> equity = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (FASubGroup acc : accounts) {
            AccountClass cls = AccountClass.fromSType(acc.getSType());
            AccountPostingTotals t = totals.getOrDefault(acc.getSCode(), EMPTY);
            BigDecimal debitMinusCredit = t.totalDebit().subtract(t.totalCredit());
            BigDecimal creditMinusDebit = t.totalCredit().subtract(t.totalDebit());

            switch (cls) {
                case ASSET: // debit-normal
                    totalAssets = totalAssets.add(debitMinusCredit);
                    addNonZero(assets, acc, debitMinusCredit);
                    break;
                case LIABILITY: // credit-normal
                    totalLiabilities = totalLiabilities.add(creditMinusDebit);
                    addNonZero(liabilities, acc, creditMinusDebit);
                    break;
                case EQUITY: // credit-normal
                    totalEquity = totalEquity.add(creditMinusDebit);
                    addNonZero(equity, acc, creditMinusDebit);
                    break;
                case INCOME: // credit-normal — feeds net income
                    totalRevenue = totalRevenue.add(creditMinusDebit);
                    break;
                case EXPENSE: // debit-normal — feeds net income
                    totalExpenses = totalExpenses.add(debitMinusCredit);
                    break;
            }
        }

        // Unclosed earnings go in equity so the sheet balances.
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        equity.add(new BalanceSheetDTO.LineItem(NET_INCOME_CODE, "Net Income", netIncome));
        totalEquity = totalEquity.add(netIncome);

        boolean balanced = totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;
        return new BalanceSheetDTO(asOfDate, assets, liabilities, equity,
                totalAssets, totalLiabilities, totalEquity, balanced);
    }

    private void addNonZero(List<BalanceSheetDTO.LineItem> target, FASubGroup acc, BigDecimal amount) {
        if (amount.signum() != 0) {
            target.add(new BalanceSheetDTO.LineItem(acc.getSCode(), acc.getSDesc(), amount));
        }
    }

    private Map<String, AccountPostingTotals> indexByCode(List<AccountPostingTotals> postings) {
        Map<String, AccountPostingTotals> map = new HashMap<>();
        for (AccountPostingTotals p : postings) {
            map.put(p.accountCode(), p);
        }
        return map;
    }
}
