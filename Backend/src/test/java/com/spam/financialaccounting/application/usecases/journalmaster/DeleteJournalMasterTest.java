package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@ExtendWith(MockitoExtension.class)
public class DeleteJournalMasterTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private DeleteJournalMaster deleteJournalMaster;

    @Test
    @DisplayName("Should return true and delete when journal exists and has no detail lines")
    void shouldDelete_WhenExistsAndHasNoDetails() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("JV00000001")).thenReturn(Collections.emptyList());
        when(journalMasterRepository.delete("JV00000001")).thenReturn(true);

        boolean result = deleteJournalMaster.execute("JV00000001");

        assertThat(result).isTrue();
        verify(journalMasterRepository).delete("JV00000001");
    }

    @Test
    @DisplayName("Should throw JournalMasterNotFoundException when ID does not exist")
    void shouldThrow_WhenJournalNotFound() {
        when(journalMasterRepository.existsById("JV99999999")).thenReturn(false);

        assertThatThrownBy(() -> deleteJournalMaster.execute("JV99999999"))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("Journal voucher with ID JV99999999 not found");

        verify(journalDetailRepository, never()).findByJournalId(any());
        verify(journalMasterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when journal has 1 associated detail line")
    void shouldThrow_WhenJournalHasOneDetailLine() {
        JournalDetail detail = new JournalDetail("JV00000001", "SG001", "DR", new BigDecimal("500.00"));
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("JV00000001")).thenReturn(Arrays.asList(detail));

        assertThatThrownBy(() -> deleteJournalMaster.execute("JV00000001"))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Cannot delete journal voucher with ID JV00000001")
                .hasMessageContaining("1 associated journal details")
                .hasMessageContaining("Delete the journal details first");

        verify(journalMasterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when journal has multiple detail lines")
    void shouldThrow_WhenJournalHasMultipleDetailLines() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("JV00000001")).thenReturn(Arrays.asList(
                new JournalDetail("JV00000001", "SG001", "DR", new BigDecimal("500.00")),
                new JournalDetail("JV00000001", "SG002", "CR", new BigDecimal("500.00"))));

        assertThatThrownBy(() -> deleteJournalMaster.execute("JV00000001"))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("2 associated journal details");

        verify(journalMasterRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should check existence first, then check details, then delete — in order")
    void shouldFollowCorrectOrder() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("JV00000001")).thenReturn(Collections.emptyList());
        when(journalMasterRepository.delete("JV00000001")).thenReturn(true);

        deleteJournalMaster.execute("JV00000001");

        verify(journalMasterRepository).existsById("JV00000001");
        verify(journalDetailRepository).findByJournalId("JV00000001");
        verify(journalMasterRepository).delete("JV00000001");
    }
}
