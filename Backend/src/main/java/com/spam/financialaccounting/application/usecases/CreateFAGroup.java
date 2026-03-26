package com.spam.financialaccounting.application.usecases;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.FAGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.FAGroupValidationException;

@Service
public class CreateFAGroup {
    private final FAGroupRepository faGroupRepository;

    public CreateFAGroup(FAGroupRepository faGroupRepository) {
        this.faGroupRepository = faGroupRepository;
    }

    public FAGroup execute(FAGroup faGroup) {
        
        if (faGroup.getAccountCode() == null || faGroup.getAccountCode().isBlank()) {
            throw new FAGroupValidationException("FA Group code is required");
        }

        if (faGroupRepository.existsByCode(faGroup.getAccountCode())) {
            throw new FAGroupAlreadyExistsException("FA Group already exists with code: " + faGroup.getAccountCode());
        }
        
        return faGroupRepository.save(faGroup);  
    }
}
