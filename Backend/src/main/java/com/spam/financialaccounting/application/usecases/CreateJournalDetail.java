package com.spam.financialaccounting.application.usecases;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@Service
public class CreateJournalDetail {

    private final JournalDetailRepository journalDetailRepository;
    private final JournalMasterRepository journalMasterRepository;
    private final FASubGroupRepository faSubGroupRepository;

    public CreateJournalDetail(JournalDetailRepository journalDetailRepository,
            JournalMasterRepository journalMasterRepository, FASubGroupRepository faSubGroupRepository) {
        this.journalDetailRepository = journalDetailRepository;
        this.journalMasterRepository = journalMasterRepository;
        this.faSubGroupRepository = faSubGroupRepository;
    }

    public JournalDetail execute(JournalDetail detail) {
        // basic validation
        if (detail.getJAmount() == null || detail.getJAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (detail.getJDrCr() == null || (!detail.getJDrCr().equals("DR") && !detail.getJDrCr().equals("CR"))) {
            throw new IllegalArgumentException("Debit/Credit indicator must be either 'DR' or 'CR'");
        }

        // 2.Referential integrity - Check JournalMaster exist
        if (!journalMasterRepository.existsById(detail.getJId())) {
            throw new JournalDetailValidationException(
                    "Journal voucher with ID " + detail.getJId() + " does not exist");
        }

        // 3.Referential Integrity - Check FASubGroup exist
        if (!faSubGroupRepository.existsByCode(detail.getJCode())) {
            throw new JournalDetailValidationException(
                    "Ledger account with code " + detail.getJCode() + " does not exist");
        }

        // 4. Check for duplicate entry
        if (journalDetailRepository.existsByCompositeKey(
                detail.getJId(),
                detail.getJCode(),
                detail.getJDrCr())) {
            throw new JournalDetailAlreadyExistsException(
                    "Journal detail already exists for voucher " + detail.getJId() +
                            ", account " + detail.getJCode() + ", type " + detail.getJDrCr());
        }
        journalDetailRepository.save(detail);
        return detail;
    }
}
