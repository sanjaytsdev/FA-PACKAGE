package com.spam.financialaccounting.application.usecases.fagroup;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupValidationException;

@Service
public class CreateFAGroup {
    private final FAGroupRepository faGroupRepository;

    public CreateFAGroup(FAGroupRepository faGroupRepository) {
        this.faGroupRepository = faGroupRepository;
    }

    public FAGroup execute(FAGroup faGroup) {

        // 1. Account Code Validation
        if (faGroup.getAccountCode() == null || faGroup.getAccountCode().isBlank()) {
            throw new FAGroupValidationException("Account Code cannot be null or empty");
        }
        if (faGroup.getAccountCode().length() != 2) {
            throw new FAGroupValidationException("Account Code must be exactly 2 characters");
        }

        // 2. Uniqueness Check
        if (faGroupRepository.existsByCode(faGroup.getAccountCode())) {
            throw new FAGroupAlreadyExistsException(
                    "An account group with code '" + faGroup.getAccountCode() + "' already exists. Please choose a different code.");
        }

        // 3. Description Validation
        if (faGroup.getAccountDescription() == null || faGroup.getAccountDescription().isBlank()) {
            throw new FAGroupValidationException("Account Description cannot be null or empty");
        }

        if (faGroup.getAccountDescription().length() > 50) {
            throw new FAGroupValidationException("Account Description cannot exceed 50 characters");
        }

        // 4. Account Type Validation
        if (faGroup.getAccountType() == null) {
            throw new FAGroupValidationException("Account Type cannot be null");
        }

        // Allowed types (0=Asset, 1=Liability, 2=Equity, 3=Income, 4=Expense)
        List<String> allowedTypes = Arrays.asList("0", "1", "2", "3", "4");
        if (!allowedTypes.contains(faGroup.getAccountType())) {
            throw new FAGroupValidationException("Invalid Account Type. Allowed values are: " + allowedTypes);
        }

        // 5. Current Balance Default
        if (faGroup.getAccountCurrentBalance() == null) {
            faGroup.setAccountCurrentBalance(BigDecimal.ZERO);
        }

        return faGroupRepository.save(faGroup);
    }
}
