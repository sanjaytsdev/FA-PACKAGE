package com.spam.financialaccounting.application.usecases.fasubgroup;

import com.spam.financialaccounting.domain.entity.DrCr;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.IncompatibleAccountTypeException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.InvalidNormalBalanceException;

/**
 * Chart-of-accounts classification rule used by both create and update: a ledger
 * account's subtype (S_TYPE) has to match its parent group's type (A_TYPE).
 *
 * <p>A_TYPE is the one-digit account class (0=Asset, 1=Liability, 2=Equity,
 * 3=Income, 4=Expense). S_TYPE is two digits: the first is that same class, the
 * second is a sub-classification. So an account fits its group only when the
 * first digit of S_TYPE equals the group's A_TYPE.
 */
final class AccountTypeRules {

    private AccountTypeRules() {
    }

    static void assertSubTypeMatchesGroup(String sType, FAGroup parent) {
        if (sType == null || sType.isBlank()) {
            throw new FASubGroupValidationException("Account subtype is required.");
        }
        String groupType = parent.getAccountType() == null ? "" : parent.getAccountType().trim();
        String subTypeClass = sType.trim().substring(0, 1);
        if (!subTypeClass.equals(groupType)) {
            throw new IncompatibleAccountTypeException(sType.trim(), parent.getAccountCode(), groupType);
        }
    }

    /**
     * Checks the normal balance side for the account's class: Asset and Expense
     * accounts are debit (DR), Liability, Equity and Revenue/Income are credit
     * (CR). The expected side comes from the parent group's A_TYPE so create and
     * update agree.
     */
    static void assertNormalBalanceMatchesType(String sDrCr, FAGroup parent) {
        String groupType = parent.getAccountType() == null ? "" : parent.getAccountType().trim();
        String expected = expectedNormalBalance(groupType);
        if (expected == null) {
            // Unknown account class. The subtype/class check already covers this,
            // so let those rules handle it instead of the normal-balance rule.
            return;
        }
        String actual = DrCr.normalizeOrNull(sDrCr);
        if (!expected.equals(actual)) {
            throw new InvalidNormalBalanceException(sDrCr, parent.getAccountCode(), groupType, expected);
        }
    }

    /**
     * Maps an account class (A_TYPE) to its normal balance side, or {@code null}
     * if the class isn't recognized.
     */
    static String expectedNormalBalance(String aType) {
        switch (aType) {
            case "0": // Asset
            case "4": // Expense
                return DrCr.DEBIT;
            case "1": // Liability
            case "2": // Equity
            case "3": // Revenue / Income
                return DrCr.CREDIT;
            default:
                return null;
        }
    }
}
