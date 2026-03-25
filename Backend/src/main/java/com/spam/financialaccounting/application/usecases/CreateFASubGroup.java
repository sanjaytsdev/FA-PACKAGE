package com.spam.financialaccounting.application.usecases;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;

@Service
public class CreateFASubGroup {

    private final FASubGroupRepository subGroupRepository;
    private final FAGroupRepository groupRepository;

    public CreateFASubGroup(FASubGroupRepository subGroupRepository, FAGroupRepository groupRepository) {
        this.subGroupRepository = subGroupRepository;
        this.groupRepository = groupRepository;
    }

    public FASubGroup execute(FASubGroup subGroup) {
        FAGroup parent = groupRepository.findByCode(subGroup.getACode());
        if (parent == null) {
            throw new IllegalArgumentException("Parent Group with code " + subGroup.getACode() + " does not exist.");
        }

        if (subGroup.getSOpbal() == null) {
            subGroup.setSOpbal(BigDecimal.ZERO);
        }

        if (subGroup.getSFlag() == null) {
            subGroup.setSFlag("T");
        }

        subGroupRepository.save(subGroup);
        return subGroup;
    }

}
