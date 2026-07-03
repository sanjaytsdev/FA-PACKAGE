package com.spam.financialaccounting.application.usecases.fasubgroup;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException; 


@Service
public class GetFASubGroupByCode {
    
    private final FASubGroupRepository repository;

    public GetFASubGroupByCode(FASubGroupRepository repository){
        this.repository = repository;
    }

    public FASubGroup execute(String sCode) {
        if (sCode == null || sCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Ledger account code is required.");
        }
        return repository.findByCode(sCode).orElseThrow(() ->new FASubGroupNotFoundException("No ledger account found with code '"+sCode+"'."));
    }


}
