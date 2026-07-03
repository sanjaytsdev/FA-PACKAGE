package com.spam.financialaccounting.presentation.exception.fasubgroup;

/**
 * Thrown when a ledger account's normal balance side (S_DRCR) doesn't match the
 * natural balance for its account class (A_TYPE).
 *
 * <p>In double-entry, Asset and Expense accounts have a debit (DR) normal
 * balance, while Liability, Equity and Revenue/Income accounts have a credit (CR)
 * normal balance. Storing the wrong side would quietly flip the account's sign in
 * the trial balance, so we reject the combination.
 */
public class InvalidNormalBalanceException extends FASubGroupException {

    public InvalidNormalBalanceException(String sDrCr, String aCode, String aType, String expected) {
        super(buildMessage(sDrCr, aCode, aType, expected));
    }

    private static String buildMessage(String sDrCr, String aCode, String aType, String expected) {
        String groupType = aType == null ? "" : aType.trim();
        String name = typeName(groupType);
        return "Normal balance side '" + sDrCr + "' is invalid for account '" + aCode
                + "' of type '" + groupType + "' (" + name + "). " + name
                + " accounts must have '" + expected + "' as their normal balance.";
    }

    private static String typeName(String aType) {
        switch (aType) {
            case "0":
                return "Asset";
            case "1":
                return "Liability";
            case "2":
                return "Equity";
            case "3":
                return "Revenue";
            case "4":
                return "Expense";
            default:
                return "Unknown";
        }
    }
}
