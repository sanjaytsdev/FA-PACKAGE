package com.spam.financialaccounting.application.usecases.journaldetail;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@Service
public class GetJournalDetail {

    private final JournalDetailRepository journalDetailRepository;

    public GetJournalDetail(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    public JournalDetail execute(String jId, String jCode, String jDrCr) {
        return journalDetailRepository.findByCompositeKey(jId, jCode, jDrCr)
                .orElseThrow(() -> new JournalDetailNotFoundException(
                        "Journal detail not found for voucher " + jId +
                                ", account " + jCode + ", type " + jDrCr));
    }
}
