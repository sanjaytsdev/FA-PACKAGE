package com.spam.financialaccounting.application.usecases.journalmaster;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalVoucherPostedException;

@Service
public class UpdateJournalMaster {

    private final JournalMasterRepository journalMasterRepository;

    public UpdateJournalMaster(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public JournalMaster execute(JournalMaster journalMaster) {
        String jId = journalMaster.getJId();

        // make sure the journal exists
        if (!journalMasterRepository.existsById(jId)) {
            throw new JournalMasterNotFoundException(
                    "Journal voucher with ID " + jId + " not found");
        }

        // If it's in the ledger, it's already posted. Posted vouchers are
        // immutable; correct them with a new voucher, not by editing the header.
        throw new JournalVoucherPostedException(
                "Journal voucher " + jId + " is already posted and cannot be modified");
    }
}
