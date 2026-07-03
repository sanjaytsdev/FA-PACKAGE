package com.spam.financialaccounting.application.usecases.fasubgroup;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.DrCr;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@Service
public class CreateFASubGroup {

    private final FASubGroupRepository subGroupRepository;
    private final FAGroupRepository groupRepository;
    private final AuditLogRepository auditLogRepository;

    public CreateFASubGroup(FASubGroupRepository subGroupRepository, FAGroupRepository groupRepository,
            AuditLogRepository auditLogRepository) {
        this.subGroupRepository = subGroupRepository;
        this.groupRepository = groupRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public FASubGroup execute(FASubGroup subGroup) {

        // A bad S_DRCR silently flips the account's sign in the trial balance.
        // Take it case-insensitively and store uppercase, matching
        // UpdateFASubGroup so create and update behave the same.
        String drCr = DrCr.normalizeOrNull(subGroup.getSDrCr());
        if (drCr == null) {
            throw new FASubGroupValidationException("Normal balance side must be either 'DR' or 'CR'.");
        }
        subGroup.setSDrCr(drCr);

        FAGroup parent = groupRepository.findByCode(subGroup.getACode())
                .orElseThrow(() -> new FAGroupNotFoundException(
                        "Parent Group with code " + subGroup.getACode() + " does not exist."));

        // The subtype must be in the same class as its parent group, or the chart
        // of accounts ends up misclassified.
        AccountTypeRules.assertSubTypeMatchesGroup(subGroup.getSType(), parent);

        // Check the normal balance side for the class (Asset/Expense => DR,
        // Liability/Equity/Revenue => CR) so the trial balance signs stay right.
        AccountTypeRules.assertNormalBalanceMatchesType(subGroup.getSDrCr(), parent);

        if (subGroup.getSOpbal() == null) {
            subGroup.setSOpbal(BigDecimal.ZERO);
        }

        if (subGroup.getSFlag() == null) {
            subGroup.setSFlag("T");
        }

        if (subGroupRepository.existsByCode(subGroup.getSCode())) {
            throw new FASubGroupAlreadyExistsException(
                    "A ledger account with code '" + subGroup.getSCode() + "' already exists. Please choose a different code.");
        }

        subGroupRepository.save(subGroup);

        // TODO: use the authenticated user once auth is wired in
        auditLogRepository.append("SYSTEM", "CREATE", "LedgerAccount", subGroup.getSCode(),
                "Created ledger account " + subGroup.getSCode());
        return subGroup;
    }
}
