package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@ExtendWith(MockitoExtension.class)
public class UpdateJournalMasterTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private UpdateJournalMaster updateJournalMaster;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);
    private JournalMaster existingMaster;

    @BeforeEach
    void setUp() {
        existingMaster = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Original");
    }

    // ─────────────────────────────────────────────────────────
    // Happy Path
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully update JournalMaster when it exists and inputs are valid")
    void shouldUpdate_WhenExistsAndValid() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalMasterRepository.update(existingMaster)).thenReturn(existingMaster);

        JournalMaster result = updateJournalMaster.execute(existingMaster);

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("JV00000001");
        verify(journalMasterRepository).update(existingMaster);
    }

    @Test
    @DisplayName("Should allow null jDoc (no change to doc type during update)")
    void shouldUpdate_WhenJDocIsNull() {
        JournalMaster master = new JournalMaster("JV00000001", null, FIXED_DATE, new BigDecimal("500.00"), "Note");
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalMasterRepository.update(master)).thenReturn(master);

        JournalMaster result = updateJournalMaster.execute(master);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should allow zero amount during update")
    void shouldUpdate_WhenAmountIsZero() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, "Zero entry");
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalMasterRepository.update(master)).thenReturn(master);

        JournalMaster result = updateJournalMaster.execute(master);

        assertThat(result.getJAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────
    // Not Found
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw JournalMasterNotFoundException when ID does not exist")
    void shouldThrow_WhenNotFound() {
        when(journalMasterRepository.existsById("JV99999999")).thenReturn(false);
        JournalMaster ghost = new JournalMaster("JV99999999", "JV", FIXED_DATE, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> updateJournalMaster.execute(ghost))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("Journal voucher with ID JV99999999 not found");

        verify(journalMasterRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // jDoc Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when jDoc is not exactly 2 characters")
    void shouldThrow_WhenJDocIsWrongLength() {
        JournalMaster master = new JournalMaster("JV00000001", "JVX", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);

        assertThatThrownBy(() -> updateJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Document type must be exactly 2 characters");

        verify(journalMasterRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Amount Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when amount is negative")
    void shouldThrow_WhenAmountIsNegative() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("-1.00"), null);
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);

        assertThatThrownBy(() -> updateJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Amount cannot be negative");

        verify(journalMasterRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Narration Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when narration exceeds 100 characters")
    void shouldThrow_WhenNarrationTooLong() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, "A".repeat(101));
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);

        assertThatThrownBy(() -> updateJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Narration must not exceed 100 characters");

        verify(journalMasterRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Interaction Verification
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should call existsById and update exactly once each")
    void shouldCallRepoMethodsExactlyOnce() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);
        when(journalMasterRepository.update(existingMaster)).thenReturn(existingMaster);

        updateJournalMaster.execute(existingMaster);

        verify(journalMasterRepository, times(1)).existsById("JV00000001");
        verify(journalMasterRepository, times(1)).update(existingMaster);
    }
}
