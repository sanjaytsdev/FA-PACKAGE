package com.spam.financialaccounting.application.usecases.journalmaster;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@Service
public class GetJournalMasterById {

    private final JournalMasterRepository journalMasterRepository;

    public GetJournalMasterById(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public JournalMaster execute(String jId) {
        Optional<JournalMaster> journalMaster = journalMasterRepository.findById(jId);
        return journalMaster
                .orElseThrow(() -> new JournalMasterNotFoundException("Journal voucher with ID " + jId + " not found"));
    }
}
