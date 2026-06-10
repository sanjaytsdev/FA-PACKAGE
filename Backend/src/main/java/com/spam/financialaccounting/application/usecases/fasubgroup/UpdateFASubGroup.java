package com.spam.financialaccounting.application.usecases.fasubgroup;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.spam.financialaccounting.domain.entity.DrCr;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupHasTransactionsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@Service
public class UpdateFASubGroup {
    private final FASubGroupRepository subGroupRepository;
    private final FAGroupRepository groupRepository;
    private final JournalDetailRepository journalDetailRepository;

    public UpdateFASubGroup(FASubGroupRepository subGroupRepository, FAGroupRepository groupRepository,
            JournalDetailRepository journalDetailRepository){
        this.subGroupRepository = subGroupRepository;
        this.groupRepository = groupRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    public FASubGroup execute(FASubGroup subGroup){

        // 1. Existence Check
        FASubGroup existing = subGroupRepository.findByCode(subGroup.getSCode())
            .orElseThrow(() ->
                new FASubGroupNotFoundException(
                    "Ledger Account with code " + subGroup.getSCode() + " not found."));

        // 2. Parent Group Check
        FAGroup parent = groupRepository.findByCode(subGroup.getACode())
            .orElseThrow(() ->
                new FAGroupNotFoundException(
                    "Parent Group with code " + subGroup.getACode() + " does not exist."
                ));

        // 2b. Classification check: the subtype must be in the same class as its
        // parent group (first digit of S_TYPE must equal the group's A_TYPE).
        AccountTypeRules.assertSubTypeMatchesGroup(subGroup.getSType(), parent);

        // 3. Code Format
        if(subGroup.getSCode() == null || subGroup.getSCode().length() != 5) {
            throw new FASubGroupValidationException("Ledger account code must be exactly 5 characters.");
        }
        
        // 4. Description Validation
        if(subGroup.getSDesc() == null || subGroup.getSDesc().isBlank()) {
            throw new FASubGroupValidationException("Description cannot be null or empty.");
        }

        if(subGroup.getSDesc().length() >50) {
            throw new FASubGroupValidationException("Description cannot exceed 50 characters.");
        }

        // 5. S_DRCR validation — take it case-insensitively, store uppercase
        if(subGroup.getSDrCr() != null){
            String drCr = DrCr.normalizeOrNull(subGroup.getSDrCr());
            if(drCr == null) {
                throw new FASubGroupValidationException("Normal balance side must be either 'DR' or 'CR'.");
            }
            subGroup.setSDrCr(drCr);
        }

        // 6. S_FLAG Validation
        if(subGroup.getSFlag() != null) {
            String flag = subGroup.getSFlag().toUpperCase();
            if(!flag.equals("T") && !flag.equals("F")) {
                throw new FASubGroupValidationException("Status must be either 'T' (active) or 'F' (inactive).");
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

        // 9. Immutability guard:
        // Once this account has journal transactions, the critical master fields
        // can't change, or historical statements would silently get restated.
        // Description and active flag stay editable.
        if (journalDetailRepository.existsByAccountCode(subGroup.getSCode())) {
            assertCriticalFieldsUnchanged(existing, subGroup);
        }

        // 10. Normal balance rule: Asset/Expense must be DR, Liability/Equity/
        // Revenue must be CR. Checked after the immutability guard so that flipping
        // the side on a posted account is reported as a locked-field change; here it
        // catches a wrong side on accounts that don't have postings yet.
        if (subGroup.getSDrCr() != null) {
            AccountTypeRules.assertNormalBalanceMatchesType(subGroup.getSDrCr(), parent);
        }

        return subGroupRepository.update(subGroup);

    }

    private void assertCriticalFieldsUnchanged(FASubGroup existing, FASubGroup incoming) {
        List<String> lockedChanges = new ArrayList<>();

        // A_CODE is the parent group link (FK to FAGroup).
        if (!sameText(existing.getACode(), incoming.getACode())) {
            lockedChanges.add("parent group");
        }
        if (!sameText(existing.getSType(), incoming.getSType())) {
            lockedChanges.add("account type");
        }
        if (!sameText(existing.getSDrCr(), incoming.getSDrCr())) {
            lockedChanges.add("normal balance side");
        }
        if (!sameAmount(existing.getSOpbal(), incoming.getSOpbal())) {
            lockedChanges.add("opening balance");
        }

        if (!lockedChanges.isEmpty()) {
            throw new FASubGroupHasTransactionsException(
                "Cannot modify " + String.join(", ", lockedChanges) + " of ledger account "
                + existing.getSCode() + " because journal transactions already exist for it. "
                + "These fields are locked to preserve historical financial statements.");
        }
    }

    private boolean sameText(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private boolean sameAmount(BigDecimal a, BigDecimal b) {
        BigDecimal left = (a == null) ? BigDecimal.ZERO : a;
        BigDecimal right = (b == null) ? BigDecimal.ZERO : b;
        return left.compareTo(right) == 0;
    }

}
