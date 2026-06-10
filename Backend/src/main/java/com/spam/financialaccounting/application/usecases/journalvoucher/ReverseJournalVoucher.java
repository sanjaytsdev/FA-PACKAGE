package com.spam.financialaccounting.application.usecases.journalvoucher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.PeriodLockedException;
import com.spam.financialaccounting.presentation.exception.journalmaster.VoucherAlreadyReversedException;

/**
 * Reverses a posted voucher by posting a new mirror-image voucher (every DR/CR
 * flipped), instead of editing or deleting the original. It's the only way to
 * correct a posted voucher and it keeps the audit trail intact.
 */
@Service
public class ReverseJournalVoucher {

    private final JournalMasterRepository journalMasterRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final PostJournalVoucher postJournalVoucher;
    private final PeriodLockRepository periodLockRepository;
    private final AuditLogRepository auditLogRepository;

    public ReverseJournalVoucher(JournalMasterRepository journalMasterRepository,
            JournalDetailRepository journalDetailRepository,
            PostJournalVoucher postJournalVoucher,
            PeriodLockRepository periodLockRepository,
            AuditLogRepository auditLogRepository) {
        this.journalMasterRepository = journalMasterRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.postJournalVoucher = postJournalVoucher;
        this.periodLockRepository = periodLockRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public JournalMaster execute(String originalId) {
        // Only posted vouchers can be reversed. Here "exists" means "posted",
        // since every voucher in the ledger went through PostJournalVoucher.
        if (!journalMasterRepository.existsById(originalId)) {
            throw new JournalMasterNotFoundException(
                    "Journal voucher with ID " + originalId + " not found");
        }

        if (journalMasterRepository.getReversedBy(originalId) != null) {
            throw new VoucherAlreadyReversedException(
                    "Journal voucher " + originalId + " has already been reversed");
        }

        // No chains: you can't reverse a reversal.
        if (journalMasterRepository.getReverses(originalId) != null) {
            throw new VoucherAlreadyReversedException(
                    "Journal voucher " + originalId + " is itself a reversal and cannot be reversed");
        }

        // The reversal posts with today's date; reject if that period is locked.
        if (periodLockRepository.isLocked(LocalDate.now())) {
            throw new PeriodLockedException(
                    "Cannot reverse into a locked period containing " + LocalDate.now());
        }

        List<JournalDetail> flipped = journalDetailRepository.findByJournalId(originalId).stream()
                .map(line -> new JournalDetail(null, line.getJCode(), flip(line.getJDrCr()), line.getJAmount()))
                .collect(Collectors.toList());

        // J_DATE is set to today by the server, never from the client.
        JournalMaster reversalHeader = new JournalMaster(
                null, "JV", LocalDateTime.now(), null, "Reversal of " + originalId);

        // Post the mirror voucher with the POST audit turned off: this one action
        // gets logged once below as REVERSE, not as both POST and REVERSE, so the
        // audit history stays clear.
        JournalMaster reversal = postJournalVoucher.execute(reversalHeader, flipped, false);

        journalMasterRepository.linkReversal(originalId, reversal.getJId());

        // TODO: use the authenticated user once auth is wired in
        auditLogRepository.append("SYSTEM", "REVERSE", "JournalVoucher", reversal.getJId(),
                "Reversal of " + originalId);
        return reversal;
    }

    private String flip(String drCr) {
        return "DR".equals(drCr) ? "CR" : "DR";
    }
}
