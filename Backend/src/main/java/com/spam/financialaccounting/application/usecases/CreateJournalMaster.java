package com.spam.financialaccounting.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@Service
public class CreateJournalMaster {

    private final JournalMasterRepository journalMasterRepository;

    public CreateJournalMaster(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public JournalMaster execute(JournalMaster journalMaster) {
        // validate journal ID format(10 characters)
        if (journalMaster.getJId() == null || journalMaster.getJId().length() != 10) {
            throw new JournalMasterValidationException("Journal ID must be exactly 10 characters");
        }

        // validate document type(2 characters)
        if (journalMaster.getJDoc() == null || journalMaster.getJDoc().length() != 2) {
            throw new JournalMasterValidationException("Document type must be exactly 2 characters");
        }

        // validate date
        if (journalMaster.getJDate() == null) {
            journalMaster.setJDate(LocalDateTime.now());
        }

        // validate amount
        if (journalMaster.getJAmount() == null) {
            journalMaster.setJAmount(BigDecimal.ZERO);
        }

        // validate narration length
        if (journalMaster.getJNarr() != null && journalMaster.getJNarr().length() > 100) {
            throw new JournalMasterValidationException("Narration must not exceed 100 characters");
        }

        // Check for duplicate ID
        if (journalMasterRepository.existsById(journalMaster.getJId())) {
            throw new JournalMasterAlreadyExistsException(
                    "Journal voucher with ID " + journalMaster.getJId() + " already exists");
        }
        
        return journalMasterRepository.save(journalMaster);
    }
}
