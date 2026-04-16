package com.spam.financialaccounting.application.usecases;

import com.spam.financialaccounting.domain.entity.JournalDetail;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.JournalDetailRepository;

@Service
public class GetAllJournalDetails {

    private final JournalDetailRepository journalDetailRepository;

    public GetAllJournalDetails(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    public List<JournalDetail> execute() {
        return journalDetailRepository.findAll();
    }
}
