package com.spam.financialaccounting.application.usecases.journalmaster;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@Service
public class UpdateJournalMaster {

    private final JournalMasterRepository journalMasterRepository;

    public UpdateJournalMaster(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public JournalMaster execute(JournalMaster journalMaster) {
        // verify journal exists
        if (!journalMasterRepository.existsById(journalMaster.getJId())) {
            throw new JournalMasterNotFoundException(
                    "Journal voucher with ID " + journalMaster.getJId() + " not found");
        }

        // Validate document type
        if (journalMaster.getJDoc() != null && journalMaster.getJDoc().length() != 2) {
            throw new JournalMasterValidationException("Document type must be exactly 2 characters");
        }

        // Validate amount is non-negative
        if (journalMaster.getJAmount() != null && journalMaster.getJAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new JournalMasterValidationException("Amount cannot be negative");
        }

        // Validate narration length
        if (journalMaster.getJNarr() != null && journalMaster.getJNarr().length() > 100) {
            throw new JournalMasterValidationException("Narration must not exceed 100 characters");
        }

        return journalMasterRepository.update(journalMaster);
    }
}
