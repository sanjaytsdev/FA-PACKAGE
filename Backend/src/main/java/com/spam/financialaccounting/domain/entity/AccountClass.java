package com.spam.financialaccounting.domain.entity;

/**
 * The five account classes, keyed by the first digit of an account's type code:
 * 0=Asset, 1=Liability, 2=Equity, 3=Income (revenue), 4=Expense. It's the same
 * class digit {@code AccountTypeRules} checks between a ledger account's S_TYPE
 * and its parent group's A_TYPE.
 *
 * <p>{@code debitNormal} says which side increases the account: assets and
 * expenses go up on the debit side; liabilities, equity and income go up on the
 * credit side. The financial statements use this to show each balance on its
 * natural side.
 */
public enum AccountClass {

    ASSET('0', true),
    LIABILITY('1', false),
    EQUITY('2', false),
    INCOME('3', false),
    EXPENSE('4', true);

    private final char classDigit;
    private final boolean debitNormal;

    AccountClass(char classDigit, boolean debitNormal) {
        this.classDigit = classDigit;
        this.debitNormal = debitNormal;
    }

    public boolean isDebitNormal() {
        return debitNormal;
    }

    /**
     * Works out a ledger account's class from its S_TYPE, whose first digit is the
     * class (AccountTypeRules makes sure of that on create/update).
     *
     * @throws IllegalArgumentException if S_TYPE is missing or its first digit
     *         isn't one of the five known classes.
     */
    public static AccountClass fromSType(String sType) {
        if (sType == null || sType.isBlank()) {
            throw new IllegalArgumentException("Account subtype is required to classify the account");
        }
        char first = sType.trim().charAt(0);
        for (AccountClass c : values()) {
            if (c.classDigit == first) {
                return c;
            }
        }
        throw new IllegalArgumentException(
                "Unrecognized account class digit '" + first + "' in subtype '" + sType + "'");
    }
}
