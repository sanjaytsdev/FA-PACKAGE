package com.spam.financialaccounting.application.usecases.journalvoucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.VoucherAlreadyReversedException;

@ExtendWith(MockitoExtension.class)
public class ReverseJournalVoucherTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;
    @Mock
    private JournalDetailRepository journalDetailRepository;
    @Mock
    private PostJournalVoucher postJournalVoucher;
    @Mock
    private PeriodLockRepository periodLockRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ReverseJournalVoucher reverseJournalVoucher;

    private static final String ORIGINAL_ID = "JV20260001";
    private static final String REVERSAL_ID = "JV20260002";

    // ─────────────────────────────────────────────────────────
    // Gap A — happy path (+ Gap B audit)
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should post a flipped reversal, link it to the original, and write a REVERSE audit row")
    void shouldReverse_WhenVoucherPosted() {
        when(journalMasterRepository.existsById(ORIGINAL_ID)).thenReturn(true);
        when(journalMasterRepository.getReversedBy(ORIGINAL_ID)).thenReturn(null);
        when(journalMasterRepository.getReverses(ORIGINAL_ID)).thenReturn(null);
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);
        when(journalDetailRepository.findByJournalId(ORIGINAL_ID)).thenReturn(List.of(
                new JournalDetail(ORIGINAL_ID, "SG01", "DR", new BigDecimal("100.00")),
                new JournalDetail(ORIGINAL_ID, "SG02", "CR", new BigDecimal("100.00"))));
        JournalMaster reversal = new JournalMaster(REVERSAL_ID, "JV", LocalDateTime.now(),
                new BigDecimal("100.00"), "Reversal of " + ORIGINAL_ID);
        when(postJournalVoucher.execute(any(JournalMaster.class), any(), eq(false))).thenReturn(reversal);

        JournalMaster result = reverseJournalVoucher.execute(ORIGINAL_ID);

        assertThat(result.getJId()).isEqualTo(REVERSAL_ID);
        verify(journalMasterRepository, times(1)).linkReversal(ORIGINAL_ID, REVERSAL_ID);
        // The mirror voucher is posted with the POST audit suppressed (recordAudit=false)...
        verify(postJournalVoucher, times(1)).execute(any(JournalMaster.class), any(), eq(false));
        // ...so the reversal action is recorded exactly once, as REVERSE, and never as POST.
        verify(auditLogRepository, times(1))
                .append(eq("SYSTEM"), eq("REVERSE"), eq("JournalVoucher"), eq(REVERSAL_ID), anyString());
        verify(auditLogRepository, never())
                .append(any(), eq("POST"), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────
    // Gap A — sad path
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw VoucherAlreadyReversedException when the voucher is already reversed")
    void shouldThrow_WhenAlreadyReversed() {
        when(journalMasterRepository.existsById(ORIGINAL_ID)).thenReturn(true);
        when(journalMasterRepository.getReversedBy(ORIGINAL_ID)).thenReturn(REVERSAL_ID);

        assertThatThrownBy(() -> reverseJournalVoucher.execute(ORIGINAL_ID))
                .isInstanceOf(VoucherAlreadyReversedException.class)
                .hasMessageContaining("already been reversed");

        verify(postJournalVoucher, never()).execute(any(), any());
        verify(journalMasterRepository, never()).linkReversal(anyString(), anyString());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }
}
