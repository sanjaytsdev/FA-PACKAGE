package com.spam.financialaccounting.application.usecases;

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
            throw new IllegalArgumentException("sCode cannot be null or empty");
        }
        return repository.findByCode(sCode).orElseThrow(() ->new FASubGroupNotFoundException("FASubGroup not found with code: "+sCode));
    }


}
