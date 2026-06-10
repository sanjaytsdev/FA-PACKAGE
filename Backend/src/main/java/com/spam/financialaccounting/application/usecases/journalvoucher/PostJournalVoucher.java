package com.spam.financialaccounting.application.usecases.journalvoucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spam.financialaccounting.domain.entity.DrCr;
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

/**
 * Posts a whole journal voucher (header + all lines) in one go.
 *
 * Enforces the things a general ledger needs that the old header/line endpoints
 * didn't:
 *  - debits must equal credits;
 *  - the voucher total comes from the lines, not from the client;
 *  - header and lines are saved in one transaction, so you can't end up with a
 *    half-written or unbalanced voucher.
 */
@Service
public class PostJournalVoucher {

    /**
     * Shortest narration we'll accept, measured after trimming. Every voucher
     * needs one so the audit trail says why the posting happened.
     */
    static final int MIN_NARRATION_LENGTH = 5;
    static final int MAX_NARRATION_LENGTH = 100;

    private final JournalMasterRepository journalMasterRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final FASubGroupRepository faSubGroupRepository;
    private final PeriodLockRepository periodLockRepository;
    private final AuditLogRepository auditLogRepository;

    public PostJournalVoucher(JournalMasterRepository journalMasterRepository,
            JournalDetailRepository journalDetailRepository,
            FASubGroupRepository faSubGroupRepository,
            PeriodLockRepository periodLockRepository,
            AuditLogRepository auditLogRepository) {
        this.journalMasterRepository = journalMasterRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.faSubGroupRepository = faSubGroupRepository;
        this.periodLockRepository = periodLockRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public JournalMaster execute(JournalMaster header, List<JournalDetail> lines) {
        return execute(header, lines, true);
    }

    /**
     * @param recordAudit when {@code false}, skip the POST audit row so a caller
     *        doing something bigger (like a reversal) can log that instead. Stops
     *        one action from showing up as both POST and, say, REVERSE.
     */
    @Transactional
    public JournalMaster execute(JournalMaster header, List<JournalDetail> lines, boolean recordAudit) {
        // A double-entry voucher needs at least two lines.
        if (lines == null || lines.size() < 2) {
            throw new JournalMasterValidationException(
                    "A voucher must have at least two lines (one debit and one credit)");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        Set<String> seen = new HashSet<>();

        for (JournalDetail line : lines) {
            if (line.getJAmount() == null || line.getJAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new JournalMasterValidationException("Each line amount must be greater than zero");
            }

            // Take 'dr'/'cr' in any case, store uppercase.
            String side = DrCr.normalizeOrNull(line.getJDrCr());
            if (side == null) {
                throw new JournalMasterValidationException("Line debit/credit indicator must be 'DR' or 'CR'");
            }
            line.setJDrCr(side);

            FASubGroup account = faSubGroupRepository.findByCode(line.getJCode())
                    .orElseThrow(() -> new JournalMasterValidationException(
                            "Ledger account " + line.getJCode() + " does not exist"));

            // Inactive accounts (S_FLAG != 'T') can't take new postings. Old
            // transactions that already use them are read elsewhere and stay
            // visible.
            if (!"T".equals(account.getSFlag())) {
                throw new InactiveAccountException(
                        "Ledger account " + line.getJCode()
                                + " is inactive and cannot receive new postings");
            }

            // The (J_ID, J_CODE, J_DRCR) primary key won't allow the same account
            // twice on the same side; catch it here with a clearer message.
            if (!seen.add(line.getJCode() + "|" + side)) {
                throw new JournalMasterValidationException(
                        "Duplicate line for account " + line.getJCode() + " on the same side (" + side + ")");
            }

            if ("DR".equals(side)) {
                totalDebits = totalDebits.add(line.getJAmount());
            } else {
                totalCredits = totalCredits.add(line.getJAmount());
            }
        }

        if (totalDebits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new JournalMasterValidationException("Voucher total must be greater than zero");
        }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new JournalMasterValidationException(
                    "Voucher is not balanced: total debits (" + totalDebits.toPlainString()
                            + ") must equal total credits (" + totalCredits.toPlainString() + ")");
        }

        // Build the header: server-generated id, derived total, defaults.
        String jId = journalMasterRepository.generateNextId();
        header.setJId(jId);
        if (header.getJDoc() == null || header.getJDoc().isBlank()) {
            header.setJDoc("JV");
        }
        if (header.getJDate() == null) {
            header.setJDate(LocalDateTime.now());
        }

        // Narration is required so the audit trail explains why the posting was
        // made. Reject null/blank or anything too short; store the trimmed form.
        if (header.getJNarr() == null || header.getJNarr().isBlank()) {
            throw new JournalMasterValidationException("Narration is required");
        }
        String narration = header.getJNarr().trim();
        if (narration.length() < MIN_NARRATION_LENGTH) {
            throw new JournalMasterValidationException(
                    "Narration must be at least " + MIN_NARRATION_LENGTH + " characters");
        }
        if (narration.length() > MAX_NARRATION_LENGTH) {
            throw new JournalMasterValidationException(
                    "Narration must not exceed " + MAX_NARRATION_LENGTH + " characters");
        }
        header.setJNarr(narration);

        header.setJAmount(totalDebits); // from the lines, not the client value

        // No posting into a closed period.
        if (periodLockRepository.isLocked(header.getJDate().toLocalDate())) {
            throw new PeriodLockedException(
                    "Cannot post into a locked period containing " + header.getJDate().toLocalDate());
        }

        journalMasterRepository.save(header);
        for (JournalDetail line : lines) {
            line.setJId(jId);
            journalDetailRepository.save(line);
        }

        if (recordAudit) {
            // TODO: use the authenticated user once auth is wired in
            auditLogRepository.append("SYSTEM", "POST", "JournalVoucher", jId,
                    "Posted voucher " + jId + " for " + totalDebits.toPlainString()
                            + " - " + header.getJNarr());
        }
        return header;
    }
}
