package com.spam.financialaccounting.application.usecases;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;

public class CreateFAGroup {
    private FAGroupRepository faGroupRepository;

    public CreateFAGroup(FAGroupRepository faGroupRepository) {
        this.faGroupRepository = faGroupRepository;
    }

    public void execute(FAGroup faGroup) {
        faGroupRepository.save(faGroup);  
    }
}
