package com.spam.financialaccounting.application.usecases.openingbalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.application.usecases.journalvoucher.PostJournalVoucher;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceAlreadyInitializedException;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceImbalanceException;

@ExtendWith(MockitoExtension.class)
public class ImportOpeningBalancesTest {

    @Mock
    private PostJournalVoucher postJournalVoucher;

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private ImportOpeningBalances importOpeningBalances;

    @Test
    @DisplayName("Posts a balanced set as an 'OB' voucher and returns the created header")
    void postsBalancedOpeningBalances() {
        List<OpeningBalanceEntry> entries = List.of(
                new OpeningBalanceEntry("10001", "DR", new BigDecimal("1000.00")),
                new OpeningBalanceEntry("30001", "CR", new BigDecimal("1000.00")));
        JournalMaster posted = new JournalMaster("JV20260001", "OB", null, new BigDecimal("1000.00"), "Opening Balance");

        when(journalMasterRepository.existsByDoc("OB")).thenReturn(false);
        when(postJournalVoucher.execute(any(JournalMaster.class), anyList())).thenReturn(posted);

        JournalMaster result = importOpeningBalances.execute(entries, null, null);

        assertThat(result).isSameAs(posted);

        ArgumentCaptor<JournalMaster> headerCaptor = ArgumentCaptor.forClass(JournalMaster.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(postJournalVoucher).execute(headerCaptor.capture(), linesCaptor.capture());

        assertThat(headerCaptor.getValue().getJDoc()).isEqualTo("OB");
        assertThat(headerCaptor.getValue().getJNarr()).isEqualTo("Opening Balance");
        assertThat(linesCaptor.getValue()).hasSize(2);
        assertThat(linesCaptor.getValue().get(0).getJCode()).isEqualTo("10001");
        assertThat(linesCaptor.getValue().get(0).getJDrCr()).isEqualTo("DR");
        assertThat(linesCaptor.getValue().get(0).getJAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Rejects an unbalanced set with OpeningBalanceImbalanceException and posts nothing")
    void rejectsUnbalancedOpeningBalances() {
        List<OpeningBalanceEntry> entries = List.of(
                new OpeningBalanceEntry("10001", "DR", new BigDecimal("5000")),
                new OpeningBalanceEntry("30001", "CR", new BigDecimal("4000")));

        when(journalMasterRepository.existsByDoc("OB")).thenReturn(false);

        assertThatThrownBy(() -> importOpeningBalances.execute(entries, null, null))
                .isInstanceOf(OpeningBalanceImbalanceException.class)
                .hasMessageContaining("out of balance")
                .hasMessageContaining("5000")
                .hasMessageContaining("4000")
                .hasMessageContaining("1000")
                .hasMessageContaining("DR");

        verify(postJournalVoucher, never()).execute(any(), any());
    }

    @Test
    @DisplayName("Rejects a second import once opening balances already exist")
    void rejectsReinitialization() {
        List<OpeningBalanceEntry> entries = List.of(
                new OpeningBalanceEntry("10001", "DR", new BigDecimal("1000")),
                new OpeningBalanceEntry("30001", "CR", new BigDecimal("1000")));

        when(journalMasterRepository.existsByDoc("OB")).thenReturn(true);

        assertThatThrownBy(() -> importOpeningBalances.execute(entries, null, null))
                .isInstanceOf(OpeningBalanceAlreadyInitializedException.class)
                .hasMessageContaining("already been imported");

        verify(postJournalVoucher, never()).execute(any(), any());
    }

    @Test
    @DisplayName("Rejects an empty set before touching the repository")
    void rejectsEmptySet() {
        assertThatThrownBy(() -> importOpeningBalances.execute(List.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one opening balance");

        verify(journalMasterRepository, never()).existsByDoc(eq("OB"));
        verify(postJournalVoucher, never()).execute(any(), any());
    }
}
