package com.spam.financialaccounting.application.usecases.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
import com.spam.financialaccounting.presentation.dto.TrialBalanceDTO;

@ExtendWith(MockitoExtension.class)
public class GetTrialBalanceTest {

    @Mock
    private FASubGroupRepository faSubGroupRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private GetTrialBalance getTrialBalance;

    // S_OPBAL isn't where opening balances come from anymore (it stays ZERO).
    // They hit the trial balance as normal postings through the Opening Balance
    // voucher, so we feed them in here via sumPostingsAsOf.
    private FASubGroup account(String code, String desc, String drCr) {
        return new FASubGroup(code, desc, "01", "00", BigDecimal.ZERO, drCr, "T");
    }

    @Test
    @DisplayName("Aggregates posted DR/CR (incl. opening voucher) into a balanced trial balance")
    void buildsBalancedTrialBalance() {
        // Opening voucher: Cash DR 1000, Capital CR 1000.
        // Plus a 500 sale: Cash DR 500, Income CR 500.
        // Combined postings: Cash DR 1500, Capital CR 1000, Income CR 500.
        when(faSubGroupRepository.findAll()).thenReturn(Arrays.asList(
                account("10001", "Cash", "DR"),
                account("30001", "Capital", "CR"),
                account("40001", "Sales Income", "CR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 3, 31))).thenReturn(Arrays.asList(
                new AccountPostingTotals("10001", new BigDecimal("1500"), BigDecimal.ZERO),
                new AccountPostingTotals("30001", BigDecimal.ZERO, new BigDecimal("1000")),
                new AccountPostingTotals("40001", BigDecimal.ZERO, new BigDecimal("500"))));

        TrialBalanceDTO result = getTrialBalance.execute(LocalDate.of(2026, 3, 31));

        assertThat(result.getAsOfDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(result.isBalanced()).isTrue();
        assertThat(result.getTotalDebit()).isEqualByComparingTo("1500");
        assertThat(result.getTotalCredit()).isEqualByComparingTo("1500");

        // Cash: 1500 posted DR in the debit column.
        TrialBalanceDTO.Row cash = result.getRows().stream()
                .filter(r -> r.getAccountCode().equals("10001")).findFirst().orElseThrow();
        assertThat(cash.getDebit()).isEqualByComparingTo("1500");
        assertThat(cash.getCredit()).isEqualByComparingTo("0");

        // Income: 500 posted CR shows in the credit column.
        TrialBalanceDTO.Row income = result.getRows().stream()
                .filter(r -> r.getAccountCode().equals("40001")).findFirst().orElseThrow();
        assertThat(income.getCredit()).isEqualByComparingTo("500");
        assertThat(income.getDebit()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Forwards the requested asOfDate to the repository unchanged")
    void forwardsAsOfDateToRepository() {
        LocalDate asOf = LocalDate.of(2025, 12, 31);
        when(faSubGroupRepository.findAll()).thenReturn(Collections.emptyList());
        when(journalDetailRepository.sumPostingsAsOf(asOf)).thenReturn(Collections.emptyList());

        getTrialBalance.execute(asOf);

        verify(journalDetailRepository).sumPostingsAsOf(asOf);
    }

    @Test
    @DisplayName("Reports balanced=false when postings do not net to zero")
    void detectsImbalance() {
        // Only one side of a posting exists → books are out of balance.
        when(faSubGroupRepository.findAll()).thenReturn(List.of(
                account("10001", "Cash", "DR")));
        when(journalDetailRepository.sumPostingsAsOf(LocalDate.of(2026, 1, 1)))
                .thenReturn(List.of(new AccountPostingTotals("10001", new BigDecimal("1000"), BigDecimal.ZERO)));

        TrialBalanceDTO result = getTrialBalance.execute(LocalDate.of(2026, 1, 1));

        assertThat(result.isBalanced()).isFalse();
        assertThat(result.getTotalDebit()).isEqualByComparingTo("1000");
        assertThat(result.getTotalCredit()).isEqualByComparingTo("0");
    }
}
