package com.spam.financialaccounting.application.usecases.journaldetail;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.domain.entity.JournalDetail;

@Service
public class GetJournalDetailsByAccountCode {

    private final JournalDetailRepository journalDetailRepository;
    private final FASubGroupRepository faSubGroupRepository;

    public GetJournalDetailsByAccountCode(JournalDetailRepository journalDetailRepository,
            FASubGroupRepository faSubGroupRepository) {
        this.journalDetailRepository = journalDetailRepository;
        this.faSubGroupRepository = faSubGroupRepository;
    }

    public List<JournalDetail> execute(String sCode) {
        // The ledger account in the path has to exist; a missing one is a 404,
        // same as the other read endpoints.
        if (!faSubGroupRepository.existsByCode(sCode)) {
            throw new FASubGroupNotFoundException(
                    "Ledger account with code " + sCode + " does not exist");
        }
        return journalDetailRepository.findByAccountCode(sCode);
    }
}
