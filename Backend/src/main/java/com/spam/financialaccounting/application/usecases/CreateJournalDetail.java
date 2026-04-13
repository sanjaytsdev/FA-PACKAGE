package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;

@Service
public class CreateJournalDetail {

    private final JournalDetailRepository repository;

    public CreateJournalDetail(JournalDetailRepository repository) {
        this.repository = repository;
    }

    public JournalDetail execute(JournalDetail detail) {
        //basic validation
        if(detail.getJAmount()==null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        repository.save(detail);
        return detail;
    }
}
