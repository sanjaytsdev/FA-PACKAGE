package com.spam.financialaccounting.application.usecases.fasubgroup;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@Service
public class DeleteFASubGroup {

    private final FASubGroupRepository subGroupRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final AuditLogRepository auditLogRepository;

    public DeleteFASubGroup(FASubGroupRepository subGroupRepository, JournalDetailRepository journalDetailRepository,
            AuditLogRepository auditLogRepository) {
        this.subGroupRepository = subGroupRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public boolean execute(String sCode) {
        // 1.check if sub-group exists
        subGroupRepository.findByCode(sCode)
                .orElseThrow(() -> new FASubGroupNotFoundException("Ledger Account with code " + sCode + " not found"));

        // 2.Check for existing journal entries
        // (uses JournalDetailRepository to check by ledger code)
        if (hasJournalEntries(sCode)) {
            throw new FASubGroupValidationException(
                    "Cannot delete ledger account with existing journal entries.");
        }
        boolean deleted = subGroupRepository.delete(sCode);

        // Only log the deletion if a row was actually removed. A concurrent delete
        // between the check above and here returns false, and the audit trail
        // shouldn't claim a deletion that didn't happen.
        if (deleted) {
            // TODO: use the authenticated user once auth is wired in
            auditLogRepository.append("SYSTEM", "DELETE", "LedgerAccount", sCode,
                    "Deleted ledger account " + sCode);
        }
        return deleted;
    }

    private boolean hasJournalEntries(String sCode) {
        return journalDetailRepository.existsByAccountCode(sCode);
    }
}
