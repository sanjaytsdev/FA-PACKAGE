package com.spam.financialaccounting.application.usecases.fasubgroup;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@Service
public class UpdateFASubGroup {
    private final FASubGroupRepository subGroupRepository;
    private final FAGroupRepository groupRepository;

    public UpdateFASubGroup(FASubGroupRepository subGroupRepository, FAGroupRepository groupRepository){
        this.subGroupRepository = subGroupRepository;
        this.groupRepository = groupRepository;
    }

    public FASubGroup execute(FASubGroup subGroup){

        // 1. Existence Check
        subGroupRepository.findByCode(subGroup.getSCode())
            .orElseThrow(() ->
                new FASubGroupNotFoundException(
                    "Ledger Account with code " + subGroup.getSCode() + " not found."));

        // 2. Parent Group Check
        groupRepository.findByCode(subGroup.getACode())
            .orElseThrow(() ->
                new FAGroupNotFoundException(
                    "Parent Group with code " + subGroup.getACode() + " does not exist."
                ));

        // 3. Code Format
        if(subGroup.getSCode() == null || subGroup.getSCode().length() != 5) {
            throw new FASubGroupValidationException("sCode must be 5 characters.");
        }
        
        // 4. Description Validation
        if(subGroup.getSDesc() == null || subGroup.getSDesc().isBlank()) {
            throw new FASubGroupValidationException("Description cannot be null or empty.");
        }

        if(subGroup.getSDesc().length() >50) {
            throw new FASubGroupValidationException("Description cannot exceed 50 characters.");
        }

        // 5. S_DRCR Validation
        if(subGroup.getSDrCr() != null){
            String drCr = subGroup.getSDrCr().toUpperCase();
            if(!drCr.equals("DR") && !drCr.equals("CR")) {
                throw new FASubGroupValidationException("S_DRCR must be either 'DR' or 'CR'");
            }
            subGroup.setSDrCr(drCr);
        }

        // 6. S_FLAG Validation
        if(subGroup.getSFlag() != null) {
            String flag = subGroup.getSFlag().toUpperCase();
            if(!flag.equals("T") && !flag.equals("F")) {
                throw new FASubGroupValidationException("S_FLAG must be either 'T'(active) or 'F'(inactive)");
            }
            subGroup.setSFlag(flag);
        }

        // 7. S_OPBAL Default 
        if(subGroup.getSOpbal() == null) {
            subGroup.setSOpbal(BigDecimal.ZERO);
        }

        // 8. S_FLAG
        if (subGroup.getSFlag() == null) {
            subGroup.setSFlag("T");
        }

        return subGroupRepository.update(subGroup);
           
    }



}
