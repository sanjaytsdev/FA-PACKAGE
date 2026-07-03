package com.spam.financialaccounting.application.usecases.fagroup;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;

@Service
public class GetAllFAGroup {
    private final FAGroupRepository repository;

    public GetAllFAGroup(FAGroupRepository repository) {
        this.repository = repository;
    }

    public  List<FAGroup> execute() {
        return repository.findAll();
    }
}
