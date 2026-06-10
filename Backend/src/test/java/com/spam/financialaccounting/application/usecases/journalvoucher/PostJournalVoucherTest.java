package com.spam.financialaccounting.application.usecases.journalvoucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.InactiveAccountException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;
import com.spam.financialaccounting.presentation.exception.journalmaster.PeriodLockedException;

@ExtendWith(MockitoExtension.class)
public class PostJournalVoucherTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;
    @Mock
    private JournalDetailRepository journalDetailRepository;
    @Mock
    private FASubGroupRepository faSubGroupRepository;
    @Mock
    private PeriodLockRepository periodLockRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private PostJournalVoucher postJournalVoucher;

    private JournalMaster header;
    private List<JournalDetail> lines;

    @BeforeEach
    void setUp() {
        header = new JournalMaster(null, "JV", null, null, "Test voucher");
        lines = List.of(
                new JournalDetail(null, "SG01", "DR", new BigDecimal("100.00")),
                new JournalDetail(null, "SG02", "CR", new BigDecimal("100.00")));
    }

    private FASubGroup account(String code, String flag) {
        return new FASubGroup(code, code + " desc", "A1", "00", BigDecimal.ZERO, "DR", flag);
    }

    // ─────────────────────────────────────────────────────────
    // Gap C — period locking
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should post into an unlocked period and write a POST audit row")
    void shouldPost_WhenPeriodUnlocked() {
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);

        JournalMaster result = postJournalVoucher.execute(header, lines);

        assertThat(result.getJId()).isEqualTo("JV20260001");
        assertThat(result.getJAmount()).isEqualByComparingTo("100.00");
        verify(journalMasterRepository, times(1)).save(result);
        verify(journalDetailRepository, times(2)).save(any(JournalDetail.class));
        // Gap B — audit row written after a successful post
        verify(auditLogRepository, times(1))
                .append(eq("SYSTEM"), eq("POST"), eq("JournalVoucher"), eq("JV20260001"), anyString());
    }

    @Test
    @DisplayName("Should throw PeriodLockedException when posting into a locked period")
    void shouldThrow_WhenPeriodLocked() {
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(true);

        assertThatThrownBy(() -> postJournalVoucher.execute(header, lines))
                .isInstanceOf(PeriodLockedException.class)
                .hasMessageContaining("locked period");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────
    // Inactive account guard — only S_FLAG = 'T' accounts may post
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should post when all line accounts are active (S_FLAG = 'T')")
    void shouldPost_WhenAccountsActive() {
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);

        JournalMaster result = postJournalVoucher.execute(header, lines);

        assertThat(result.getJId()).isEqualTo("JV20260001");
        verify(journalDetailRepository, times(2)).save(any(JournalDetail.class));
    }

    @Test
    @DisplayName("Should reject posting to an inactive account (S_FLAG = 'F') and persist nothing")
    void shouldThrow_WhenAccountInactive() {
        // First line's account (SG01) is archived; the guard must fire on it.
        when(faSubGroupRepository.findByCode("SG01"))
                .thenReturn(Optional.of(account("SG01", "F")));

        assertThatThrownBy(() -> postJournalVoucher.execute(header, lines))
                .isInstanceOf(InactiveAccountException.class)
                .hasMessageContaining("SG01")
                .hasMessageContaining("inactive");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────
    // Double-entry balance — only balanced vouchers may be posted
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject an unbalanced voucher (total debits != total credits) and persist nothing")
    void shouldThrow_WhenVoucherUnbalanced() {
        // 100 DR vs 60 CR — debits do not equal credits.
        List<JournalDetail> unbalanced = List.of(
                new JournalDetail(null, "SG01", "DR", new BigDecimal("100.00")),
                new JournalDetail(null, "SG02", "CR", new BigDecimal("60.00")));
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));

        assertThatThrownBy(() -> postJournalVoucher.execute(header, unbalanced))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("not balanced");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject a single-sided voucher (all lines on the same side) and persist nothing")
    void shouldThrow_WhenVoucherSingleSided() {
        // Two debit lines, no credit — credits total zero, so it can never balance.
        List<JournalDetail> singleSided = List.of(
                new JournalDetail(null, "SG01", "DR", new BigDecimal("100.00")),
                new JournalDetail(null, "SG02", "DR", new BigDecimal("100.00")));
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));

        assertThatThrownBy(() -> postJournalVoucher.execute(header, singleSided))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("not balanced");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a voucher with fewer than two lines and persist nothing")
    void shouldThrow_WhenFewerThanTwoLines() {
        List<JournalDetail> oneLine = List.of(
                new JournalDetail(null, "SG01", "DR", new BigDecimal("100.00")));

        assertThatThrownBy(() -> postJournalVoucher.execute(header, oneLine))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("at least two lines");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // Mandatory narration — every voucher must carry a meaningful description
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject a voucher with no narration and persist nothing")
    void shouldThrow_WhenNarrationMissing() {
        header.setJNarr(null);
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");

        assertThatThrownBy(() -> postJournalVoucher.execute(header, lines))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Narration is required");

        verify(journalMasterRepository, never()).save(any());
        verify(journalDetailRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject a blank (whitespace-only) narration and persist nothing")
    void shouldThrow_WhenNarrationBlank() {
        header.setJNarr("    ");
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");

        assertThatThrownBy(() -> postJournalVoucher.execute(header, lines))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Narration is required");

        verify(journalMasterRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject a narration shorter than the minimum meaningful length")
    void shouldThrow_WhenNarrationTooShort() {
        header.setJNarr("Hi"); // shorter than the 5-character minimum
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");

        assertThatThrownBy(() -> postJournalVoucher.execute(header, lines))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("at least");

        verify(journalMasterRepository, never()).save(any());
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should trim the narration and capture it in the POST audit detail")
    void shouldTrimNarration_AndCaptureInAudit() {
        header.setJNarr("  Office rent  ");
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);

        JournalMaster result = postJournalVoucher.execute(header, lines);

        assertThat(result.getJNarr()).isEqualTo("Office rent");
        // Audit trail records the (trimmed) narration alongside the amount.
        verify(auditLogRepository, times(1)).append(
                eq("SYSTEM"), eq("POST"), eq("JournalVoucher"), eq("JV20260001"),
                contains("Office rent"));
    }

    // ─────────────────────────────────────────────────────────
    // Audit suppression — recordAudit=false skips the POST row
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should persist the voucher but write no POST audit row when recordAudit is false")
    void shouldNotWriteAudit_WhenRecordAuditFalse() {
        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(account(inv.getArgument(0), "T")));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);

        postJournalVoucher.execute(header, lines, false);

        // Lines are still persisted, but no POST audit row is written — the caller
        // (e.g. a reversal) records its own action instead, so a reversal is never
        // logged as both POST and REVERSE.
        verify(journalDetailRepository, times(2)).save(any(JournalDetail.class));
        verify(auditLogRepository, never()).append(any(), any(), any(), any(), any());
    }
}
