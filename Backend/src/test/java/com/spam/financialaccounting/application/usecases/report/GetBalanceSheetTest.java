package com.spam.financialaccounting.application.usecases.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.AccountPostingTotals;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.dto.BalanceSheetDTO;

@ExtendWith(MockitoExtension.class)
public class GetBalanceSheetTest {

    @Mock
    private FASubGroupRepository faSubGroupRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private GetBalanceSheet getBalanceSheet;

    // S_TYPE's first digit is the account class: 0=Asset,1=Liability,2=Equity,3=Income,4=Expense.
    private FASubGroup account(String code, String desc, String sType, String drCr) {
        return new FASubGroup(code, desc, "01", sType, BigDecimal.ZERO, drCr, "T");
    }

    private BigDecimal amountOf(List<BalanceSheetDTO.LineItem> items, String code) {
        return items.stream().filter(i -> i.getAccountCode().equals(code))
                .map(BalanceSheetDTO.LineItem::getAmount).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Assets = liabilities + equity, with unclosed earnings folded into equity as Net Income")
    void buildsBalancedBalanceSheet() {
        // Opening: Cash DR 2000 / Capital CR 1500 / Loan CR 500
        // Sale:    Cash DR 500  / Income CR 500
        // Expense: Expense DR 200 / Cash CR 200
        when(faSubGroupRepository.findAll()).thenReturn(List.of(
                account("10001", "Cash", "00", "DR"),
                account("20001", "Bank Loan", "10", "CR"),
                account("30001", "Share Capital", "20", "CR"),
                account("40001", "Sales Income", "30", "CR"),
                account("50001", "Rent Expense", "40", "DR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
                new AccountPostingTotals("10001", new BigDecimal("2500"), new BigDecimal("200")), // asset 2300
                new AccountPostingTotals("20001", BigDecimal.ZERO, new BigDecimal("500")),         // liability 500
                new AccountPostingTotals("30001", BigDecimal.ZERO, new BigDecimal("1500")),        // equity 1500
                new AccountPostingTotals("40001", BigDecimal.ZERO, new BigDecimal("500")),         // revenue 500
                new AccountPostingTotals("50001", new BigDecimal("200"), BigDecimal.ZERO)));       // expense 200

        BalanceSheetDTO bs = getBalanceSheet.execute(LocalDate.of(2026, 3, 31));

        assertThat(bs.getAsOfDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(bs.getTotalAssets()).isEqualByComparingTo("2300");
        assertThat(bs.getTotalLiabilities()).isEqualByComparingTo("500");
        assertThat(bs.getTotalEquity()).isEqualByComparingTo("1800"); // 1500 capital + 300 net income
        assertThat(bs.isBalanced()).isTrue();
        assertThat(bs.getTotalAssets())
                .isEqualByComparingTo(bs.getTotalLiabilities().add(bs.getTotalEquity()));

        // Real accounts appear on their natural sides.
        assertThat(amountOf(bs.getAssets(), "10001")).isEqualByComparingTo("2300");
        assertThat(amountOf(bs.getLiabilities(), "20001")).isEqualByComparingTo("500");
        assertThat(amountOf(bs.getEquity(), "30001")).isEqualByComparingTo("1500");

        // Net income (revenue 500 - expenses 200 = 300) is carried into equity.
        BalanceSheetDTO.LineItem netIncome = bs.getEquity().stream()
                .filter(e -> e.getAccountCode().equals("NET_INCOME")).findFirst().orElseThrow();
        assertThat(netIncome.getDescription()).isEqualTo("Net Income");
        assertThat(netIncome.getAmount()).isEqualByComparingTo("300");

        // Income/expense accounts themselves are not listed on the balance sheet.
        assertThat(bs.getAssets()).noneMatch(i -> i.getAccountCode().equals("40001"));
        assertThat(bs.getEquity()).noneMatch(i -> i.getAccountCode().equals("40001"));
    }

    @Test
    @DisplayName("Equity always carries a Net Income line, even with no equity accounts")
    void alwaysIncludesNetIncomeLine() {
        when(faSubGroupRepository.findAll()).thenReturn(List.of(
                account("10001", "Cash", "00", "DR"),
                account("40001", "Sales Income", "30", "CR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 1, 1))).thenReturn(List.of(
                new AccountPostingTotals("10001", new BigDecimal("700"), BigDecimal.ZERO),
                new AccountPostingTotals("40001", BigDecimal.ZERO, new BigDecimal("700"))));

        BalanceSheetDTO bs = getBalanceSheet.execute(LocalDate.of(2026, 1, 1));

        assertThat(amountOf(bs.getEquity(), "NET_INCOME")).isEqualByComparingTo("700");
        assertThat(bs.getTotalAssets()).isEqualByComparingTo("700");
        assertThat(bs.getTotalEquity()).isEqualByComparingTo("700");
        assertThat(bs.isBalanced()).isTrue();
    }
}
