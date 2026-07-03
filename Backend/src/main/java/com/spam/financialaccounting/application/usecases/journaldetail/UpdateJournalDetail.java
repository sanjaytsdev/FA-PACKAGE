package com.spam.financialaccounting.application.usecases.journaldetail;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalVoucherPostedException;

@Service
public class UpdateJournalDetail {

    private final JournalDetailRepository journalDetailRepository;

    public UpdateJournalDetail(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    public JournalDetail execute(String jId, String jCode, String jDrCr, BigDecimal newAmount) {
        // A line only exists as part of an already-posted voucher.
        JournalDetail existingDetail = journalDetailRepository.findByCompositeKey(jId, jCode, jDrCr)
                .orElseThrow(() -> new JournalDetailNotFoundException(
                        "Journal detail not found for voucher " + jId +
                                ", account " + jCode + ", type " + jDrCr));

        // The line exists, so its voucher is posted. Posted vouchers are
        // immutable; you can't edit lines in place.
        throw new JournalVoucherPostedException(
                "Journal voucher " + existingDetail.getJId()
                        + " is already posted; its lines cannot be modified");
    }
}
