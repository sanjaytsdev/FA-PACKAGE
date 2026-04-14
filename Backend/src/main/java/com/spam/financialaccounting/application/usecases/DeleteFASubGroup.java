package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@Service
public class DeleteFASubGroup {

    private final FASubGroupRepository subGroupRepository;
    private final JournalDetailRepository journalDetailRepository;

    public DeleteFASubGroup(FASubGroupRepository subGroupRepository, JournalDetailRepository journalDetailRepository) {
        this.subGroupRepository = subGroupRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public boolean execute(String sCode) {
        // 1.check if sub-group exists
        subGroupRepository.findByCode(sCode)
                .orElseThrow(() -> new FASubGroupNotFoundException("Ledger Account with code " + sCode + " not found"));

        // 2.Check for existing journal entries
        // note: Add a method to JournalDetailRepository to check by ledger code
        if (hasJournalEntries(sCode)) {
            throw new FASubGroupValidationException(
                    "Cannot delete ledger account with existing journal entries.");
        }
        return subGroupRepository.delete(sCode);
    }

    private boolean hasJournalEntries(String sCode) {
        // Implement this check based on your JournalDetailRepository
        return false;
    }
}
