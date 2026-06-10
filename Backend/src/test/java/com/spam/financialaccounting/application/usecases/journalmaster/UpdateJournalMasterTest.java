package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalVoucherPostedException;

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
    // Posted vouchers are immutable
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw JournalVoucherPostedException when the voucher exists (is posted)")
    void shouldThrow_WhenVoucherIsPosted() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);

        assertThatThrownBy(() -> updateJournalMaster.execute(existingMaster))
                .isInstanceOf(JournalVoucherPostedException.class)
                .hasMessageContaining("Journal voucher JV00000001 is already posted");

        // a posted voucher must never be persisted with the in-place update
        verify(journalMasterRepository, never()).update(any());
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
}
