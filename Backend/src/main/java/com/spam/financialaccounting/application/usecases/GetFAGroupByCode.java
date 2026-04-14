package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;

@Service
public class GetFAGroupByCode {

    private final FAGroupRepository repository;

    public GetFAGroupByCode(FAGroupRepository repository) {
        this.repository = repository;
    }

    public FAGroup execute(String code) {
        return repository.findByCode(code).orElseThrow(()->new FAGroupNotFoundException("FAGroup not found with code: "+code));
    }
}
