package com.spam.financialaccounting.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;

@Service
public class GetAllFASubGroup {

    private final FASubGroupRepository repository;

    public GetAllFASubGroup(FASubGroupRepository repository) {
        this.repository = repository;
    }

    public List<FASubGroup> execute() {
        return repository.findAll();
    }
}
