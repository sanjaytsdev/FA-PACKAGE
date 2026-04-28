package com.spam.financialaccounting.application.usecases.journaldetail;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@Service
public class DeleteJournalDetail {

    private final JournalDetailRepository journalDetailRepository;

    public DeleteJournalDetail(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    public boolean execute(String jId, String jCode, String jDrCr) {
        // 1.check if detail exists
        if (!journalDetailRepository.existsByCompositeKey(jId, jCode, jDrCr)) {
            throw new JournalDetailNotFoundException(
                    "Journal detail not found for voucher " + jId +
                            ", account " + jCode + ", type " + jDrCr);
        }

        return journalDetailRepository.deleteByCompositeKey(jId, jCode, jDrCr);
    }

}
