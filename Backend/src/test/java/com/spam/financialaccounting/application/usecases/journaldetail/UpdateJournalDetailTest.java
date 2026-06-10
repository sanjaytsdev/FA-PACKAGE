package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalVoucherPostedException;

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
    // Posted vouchers are immutable
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw JournalVoucherPostedException when the line exists (voucher is posted)")
    void shouldThrow_WhenVoucherIsPosted() {
        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "DR"))
                .thenReturn(Optional.of(existingDetail));

        assertThatThrownBy(() -> updateJournalDetail.execute("V001", "SG01", "DR", new BigDecimal("999.00")))
                .isInstanceOf(JournalVoucherPostedException.class)
                .hasMessageContaining("Journal voucher V001 is already posted");

        // a posted voucher's line must never be persisted with the in-place update
        verify(journalDetailRepository, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────
    // Not Found
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

        verify(journalDetailRepository, never()).update(any());
    }
}
