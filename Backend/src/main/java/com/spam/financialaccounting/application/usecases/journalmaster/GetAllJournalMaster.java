package com.spam.financialaccounting.application.usecases.journalmaster;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;

@Service
public class GetAllJournalMaster {
    
    private final JournalMasterRepository journalMasterRepository;

    public GetAllJournalMaster(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public List<JournalMaster> execute() {
        return journalMasterRepository.findAll();
    }
}
