package com.spam.financialaccounting.application.usecases.journalvoucher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.PostedVoucherCannotBeDeletedException;

@ExtendWith(MockitoExtension.class)
public class DeleteJournalVoucherTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private DeleteJournalVoucher deleteJournalVoucher;

    @Test
    @DisplayName("Should throw PostedVoucherCannotBeDeletedException when deleting a posted voucher")
    void shouldThrow_WhenVoucherPosted() {
        when(journalMasterRepository.existsById("JV20260001")).thenReturn(true);

        assertThatThrownBy(() -> deleteJournalVoucher.execute("JV20260001"))
                .isInstanceOf(PostedVoucherCannotBeDeletedException.class)
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    @DisplayName("Should throw JournalMasterNotFoundException when the voucher does not exist")
    void shouldThrow_WhenNotFound() {
        when(journalMasterRepository.existsById("JV99999999")).thenReturn(false);

        assertThatThrownBy(() -> deleteJournalVoucher.execute("JV99999999"))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
