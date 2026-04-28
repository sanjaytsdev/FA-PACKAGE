package com.spam.financialaccounting.application.usecases.fagroup;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;

@Service
public class UpdateFAGroup {

    private final FAGroupRepository repository;

    public UpdateFAGroup(FAGroupRepository repository) {
        this.repository = repository;
    }

    public FAGroup execute(FAGroup faGroup) {
        // 1. Check if the group exists
        repository.findByCode(faGroup.getAccountCode())
            .orElseThrow(() ->
                    new FAGroupNotFoundException(
                            "Account Group with code " + faGroup.getAccountCode() + " not found."
                    )
            );
        FAGroup updatedGroup = repository.update(faGroup);

        return updatedGroup;
    }
}
