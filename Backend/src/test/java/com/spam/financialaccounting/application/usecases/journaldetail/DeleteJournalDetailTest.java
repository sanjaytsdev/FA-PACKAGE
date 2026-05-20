package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@ExtendWith(MockitoExtension.class)
public class DeleteJournalDetailTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private DeleteJournalDetail deleteJournalDetail;

    // Happy Path
    @Test
    @DisplayName("Should return true and call deleteByCompositeKey when detail exists")
    void shouldDelete_WhenDetailExists() {
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "DR")).thenReturn(true);
        when(journalDetailRepository.deleteByCompositeKey("V001", "SG01", "DR")).thenReturn(true);

        boolean result = deleteJournalDetail.execute("V001", "SG01", "DR");

        assertThat(result).isTrue();
        verify(journalDetailRepository).deleteByCompositeKey("V001", "SG01", "DR");
    }

    @Test
    @DisplayName("Should work for a CR type detail")
    void shouldDelete_WhenDetailIsCRType() {
        when(journalDetailRepository.existsByCompositeKey("V002", "SG02", "CR")).thenReturn(true);
        when(journalDetailRepository.deleteByCompositeKey("V002", "SG02", "CR")).thenReturn(true);

        boolean result = deleteJournalDetail.execute("V002", "SG02", "CR");

        assertThat(result).isTrue();
    }

    // Not Found
    @Test
    @DisplayName("Should throw JournalDetailNotFoundException when composite key does not exist")
    void shouldThrow_WhenDetailNotFound() {
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "DR")).thenReturn(false);

        assertThatThrownBy(() -> deleteJournalDetail.execute("V001", "SG01", "DR"))
                .isInstanceOf(JournalDetailNotFoundException.class)
                .hasMessageContaining("Journal detail not found for voucher V001")
                .hasMessageContaining("account SG01")
                .hasMessageContaining("type DR");

        // CRITICAL: deleteByCompositeKey must NEVER be called if not found
        verify(journalDetailRepository, never()).deleteByCompositeKey(any(), any(), any());
    }

    // Interaction Verification
    @Test
    @DisplayName("Should call existsByCompositeKey before calling deleteByCompositeKey")
    void shouldCheckExistenceFirst_ThenDelete() {
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "DR")).thenReturn(true);
        when(journalDetailRepository.deleteByCompositeKey("V001", "SG01", "DR")).thenReturn(true);

        deleteJournalDetail.execute("V001", "SG01", "DR");

        // Both methods should be called exactly once with the correct arguments
        verify(journalDetailRepository).existsByCompositeKey("V001", "SG01", "DR");
        verify(journalDetailRepository).deleteByCompositeKey("V001", "SG01", "DR");
    }
}
