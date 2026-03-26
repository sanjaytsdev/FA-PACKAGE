package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;

@Service
public class UpdateFAGroup {

    private final FAGroupRepository repository;

    public UpdateFAGroup(FAGroupRepository repository) {
        this.repository = repository;
    }

    public FAGroup execute(FAGroup faGroup) {
        // 1. Check if the group exists
        FAGroup existingGroup = repository.findByCode(faGroup.getAccountCode());
        if (existingGroup == null) {
            throw new IllegalArgumentException("Account Group with code " + faGroup.getAccountCode() + " not found.");
        }
        repository.update(faGroup);

        return faGroup;
    }
}
