package com.spam.financialaccounting.application.usecases.journaldetail;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

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
        // The voucher in the path has to exist; a missing one is a 404, same as
        // GetJournalMasterById/GetJournalDetail.
        if(!journalMasterRepository.existsById(jId)) {
            throw new JournalMasterNotFoundException("Journal voucher with ID " + jId + " does not exist");
        }

        return journalDetailRepository.findByJournalId(jId);
    }
}
