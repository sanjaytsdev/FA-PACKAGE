package com.spam.financialaccounting.application.usecases.journalmaster;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@Service
public class DeleteJournalMaster {

    private final JournalMasterRepository journalMasterRepository;
    private final JournalDetailRepository journalDetailRepository;

    public DeleteJournalMaster(JournalMasterRepository journalMasterRepository,
            JournalDetailRepository journalDetailRepository) {
        this.journalMasterRepository = journalMasterRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public boolean execute(String jId) {
        // make sure the journal exists
        if (!journalMasterRepository.existsById(jId)) {
            throw new JournalMasterNotFoundException("Journal voucher with ID " + jId + " not found");
        }

        // check for existing journal details (referential integrity)
        var existingDetails = journalDetailRepository.findByJournalId(jId);
        if (!existingDetails.isEmpty()) {
            throw new JournalMasterValidationException(
                    "Cannot delete journal voucher with ID " + jId + " because it has " + existingDetails.size()
                            + " associated journal details. " + "Delete the journal details first.");
        }

        return journalMasterRepository.delete(jId);
    }

}
