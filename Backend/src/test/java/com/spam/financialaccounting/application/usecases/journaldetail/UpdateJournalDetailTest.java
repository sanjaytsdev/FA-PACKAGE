package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@ExtendWith(MockitoExtension.class)
public class UpdateJournalDetailTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private UpdateJournalDetail updateJournalDetail;

    private JournalDetail existingDetail;

    @BeforeEach
    void setUp() {
        existingDetail = new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00"));
    }

    // ─────────────────────────────────────────────────────────
    // Happy Path
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update amount and return updated JournalDetail when detail exists")
    void shouldUpdate_WhenDetailExistsAndAmountIsValid() {
        BigDecimal newAmount = new BigDecimal("999.00");
        JournalDetail updatedDetail = new JournalDetail("V001", "SG01", "DR", newAmount);

        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "DR"))
                .thenReturn(Optional.of(existingDetail));
        when(journalDetailRepository.update(existingDetail)).thenReturn(updatedDetail);

        JournalDetail result = updateJournalDetail.execute("V001", "SG01", "DR", newAmount);

        assertThat(result).isNotNull();
        assertThat(result.getJAmount()).isEqualByComparingTo("999.00");
        assertThat(result.getJId()).isEqualTo("V001");
        assertThat(result.getJCode()).isEqualTo("SG01");
    }

    @Test
    @DisplayName("Should set the new amount on the existing entity before calling update()")
    void shouldMutateExistingEntity_BeforeCallingUpdate() {
        BigDecimal newAmount = new BigDecimal("750.00");

        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "DR"))
                .thenReturn(Optional.of(existingDetail));
        when(journalDetailRepository.update(existingDetail)).thenReturn(existingDetail);

        updateJournalDetail.execute("V001", "SG01", "DR", newAmount);

        // existingDetail.jAmount must have been mutated to 750.00 before update() was called
        assertThat(existingDetail.getJAmount()).isEqualByComparingTo("750.00");
        verify(journalDetailRepository, times(1)).update(existingDetail);
    }

    // ─────────────────────────────────────────────────────────
    // Amount Validation (checked BEFORE DB lookup)
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when new amount is null")
    void shouldThrow_WhenNewAmountIsNull() {
        assertThatThrownBy(() -> updateJournalDetail.execute("V001", "SG01", "DR", null))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Amount must be greater than zero");

        // DB must never be touched if amount is invalid
        verify(journalDetailRepository, never()).findByCompositeKey(any(), any(), any());
        verify(journalDetailRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when new amount is zero")
    void shouldThrow_WhenNewAmountIsZero() {
        assertThatThrownBy(() -> updateJournalDetail.execute("V001", "SG01", "DR", BigDecimal.ZERO))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(journalDetailRepository, never()).findByCompositeKey(any(), any(), any());
        verify(journalDetailRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when new amount is negative")
    void shouldThrow_WhenNewAmountIsNegative() {
        assertThatThrownBy(() -> updateJournalDetail.execute("V001", "SG01", "DR", new BigDecimal("-1.00")))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(journalDetailRepository, never()).findByCompositeKey(any(), any(), any());
        verify(journalDetailRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Not Found (checked AFTER amount validation)
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw JournalDetailNotFoundException when composite key does not exist")
    void shouldThrow_WhenDetailNotFound() {
        when(journalDetailRepository.findByCompositeKey("V999", "SG99", "DR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateJournalDetail.execute("V999", "SG99", "DR", new BigDecimal("100.00")))
                .isInstanceOf(JournalDetailNotFoundException.class)
                .hasMessageContaining("Journal detail not found for voucher V999")
                .hasMessageContaining("account SG99")
                .hasMessageContaining("type DR");

        // update() must never be called if not found
        verify(journalDetailRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Interaction Verification
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should call findByCompositeKey and update exactly once")
    void shouldCallRepoMethodsExactlyOnce() {
        BigDecimal newAmount = new BigDecimal("300.00");
        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "DR"))
                .thenReturn(Optional.of(existingDetail));
        when(journalDetailRepository.update(existingDetail)).thenReturn(existingDetail);

        updateJournalDetail.execute("V001", "SG01", "DR", newAmount);

        verify(journalDetailRepository, times(1)).findByCompositeKey("V001", "SG01", "DR");
        verify(journalDetailRepository, times(1)).update(existingDetail);
    }
}
