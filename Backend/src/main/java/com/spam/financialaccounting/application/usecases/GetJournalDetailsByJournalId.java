package com.spam.financialaccounting.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@Service
public class GetJournalDetailsByJournalId {

    private final JournalDetailRepository journalDetailRepository;
    private final JournalMasterRepository journalMasterRepository;

    public GetJournalDetailsByJournalId(JournalDetailRepository journalDetailRepository,
            JournalMasterRepository journalMasterRepository) {
        this.journalDetailRepository = journalDetailRepository;
        this.journalMasterRepository = journalMasterRepository;
    }

    public List<JournalDetail> execute(String jId) {
        //validate that the journal voucher exists
        if(!journalMasterRepository.existsById(jId)) {
            throw new JournalDetailValidationException("Journal voucher with ID " + jId + " does not exist");
        }

        return journalDetailRepository.findByJournalId(jId);
    }
}
