package com.spam.financialaccounting.presentation.exception.fasubgroup;

/**
 * Thrown when a ledger account's subtype (S_TYPE) doesn't line up with the type
 * (A_TYPE) of the parent group it's being filed under.
 *
 * <p>S_TYPE is a two-digit code. Its first digit is the account class and has to
 * match the group's single-digit A_TYPE (0=Asset, 1=Liability, 2=Equity,
 * 3=Income, 4=Expense). Filing an account under the wrong class would mess up the
 * chart-of-accounts classification, so we reject the combination.
 */
public class IncompatibleAccountTypeException extends FASubGroupException {

    public IncompatibleAccountTypeException(String sType, String aCode, String aType) {
        super(buildMessage(sType, aCode, aType));
    }

    private static String buildMessage(String sType, String aCode, String aType) {
        String groupType = aType == null ? "" : aType.trim();
        String expectedFirstDigit = groupType.isEmpty() ? "?" : groupType.substring(0, 1);
        return "Account subtype '" + sType + "' is not compatible with parent group '"
                + aCode + "' of type '" + groupType + "' (" + typeName(groupType) + "). "
                + "The first digit of the subtype must be '" + expectedFirstDigit
                + "' to match the parent group's type.";
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
                return "Income";
            case "4":
                return "Expense";
            default:
                return "Unknown";
        }
    }
}
