package com.spam.financialaccounting.application.usecases;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@Service
public class UpdateJournalDetail {

    private final JournalDetailRepository journalDetailRepository;

    public UpdateJournalDetail(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    public JournalDetail execute(String jId, String jCode, String jDrCr, BigDecimal newAmount) {
        //1.validate amount
        if(newAmount==null || newAmount.compareTo(BigDecimal.ZERO)<=0) {
             throw new JournalDetailValidationException("Amount must be greater than zero");
        }

        //2.check if details exists
        JournalDetail existingDetail=journalDetailRepository.findByCompositeKey(jId, jCode, jDrCr)
        .orElseThrow(()->new JournalDetailNotFoundException(
                "Journal detail not found for voucher " + jId +
                ", account " + jCode + ", type " + jDrCr
            ));

        //3. update amount
        existingDetail.setJAmount(newAmount);

        //4.save update
        return journalDetailRepository.update(existingDetail);
    }

    

    
    
}
