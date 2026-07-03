package com.spam.financialaccounting.application.usecases.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
import com.spam.financialaccounting.presentation.dto.ProfitAndLossDTO;

@ExtendWith(MockitoExtension.class)
public class GetProfitAndLossTest {

    @Mock
    private FASubGroupRepository faSubGroupRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private GetProfitAndLoss getProfitAndLoss;

    // S_TYPE's first digit is the account class: 0=Asset,1=Liability,2=Equity,3=Income,4=Expense.
    private FASubGroup account(String code, String desc, String sType, String drCr) {
        return new FASubGroup(code, desc, "01", sType, BigDecimal.ZERO, drCr, "T");
    }

    @Test
    @DisplayName("Revenue less expenses gives net profit; balance-sheet accounts are excluded")
    void buildsProfitAndLoss() {
        when(faSubGroupRepository.findAll()).thenReturn(List.of(
                account("10001", "Cash", "00", "DR"), // asset — must not appear
                account("40001", "Sales Income", "30", "CR"),
                account("50001", "Rent Expense", "40", "DR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
                new AccountPostingTotals("10001", new BigDecimal("1300"), BigDecimal.ZERO),
                new AccountPostingTotals("40001", BigDecimal.ZERO, new BigDecimal("500")),
                new AccountPostingTotals("50001", new BigDecimal("200"), BigDecimal.ZERO)));

        ProfitAndLossDTO pl = getProfitAndLoss.execute(LocalDate.of(2026, 3, 31));

        assertThat(pl.getAsOfDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(pl.getTotalRevenue()).isEqualByComparingTo("500");
        assertThat(pl.getTotalExpenses()).isEqualByComparingTo("200");
        assertThat(pl.getNetProfit()).isEqualByComparingTo("300");

        assertThat(pl.getRevenue()).hasSize(1);
        assertThat(pl.getRevenue().get(0).getAccountCode()).isEqualTo("40001");
        assertThat(pl.getRevenue().get(0).getAmount()).isEqualByComparingTo("500");
        assertThat(pl.getExpenses()).hasSize(1);
        assertThat(pl.getExpenses().get(0).getAccountCode()).isEqualTo("50001");

        // The asset must not leak into either P&L section.
        assertThat(pl.getRevenue()).noneMatch(r -> r.getAccountCode().equals("10001"));
        assertThat(pl.getExpenses()).noneMatch(r -> r.getAccountCode().equals("10001"));
    }

    @Test
    @DisplayName("Net profit is negative when expenses exceed revenue (a loss)")
    void reportsLoss() {
        when(faSubGroupRepository.findAll()).thenReturn(List.of(
                account("40001", "Sales Income", "30", "CR"),
                account("50001", "Rent Expense", "40", "DR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 1, 1))).thenReturn(List.of(
                new AccountPostingTotals("40001", BigDecimal.ZERO, new BigDecimal("100")),
                new AccountPostingTotals("50001", new BigDecimal("250"), BigDecimal.ZERO)));

        ProfitAndLossDTO pl = getProfitAndLoss.execute(LocalDate.of(2026, 1, 1));

        assertThat(pl.getTotalRevenue()).isEqualByComparingTo("100");
        assertThat(pl.getTotalExpenses()).isEqualByComparingTo("250");
        assertThat(pl.getNetProfit()).isEqualByComparingTo("-150");
    }

    @Test
    @DisplayName("Forwards the requested asOfDate to the repository unchanged")
    void forwardsAsOfDate() {
        LocalDate asOf = LocalDate.of(2025, 12, 31);
        when(faSubGroupRepository.findAll()).thenReturn(Collections.emptyList());
        when(journalDetailRepository.sumPostingsAsOf(asOf)).thenReturn(Collections.emptyList());

        getProfitAndLoss.execute(asOf);

        verify(journalDetailRepository).sumPostingsAsOf(asOf);
    }
}
