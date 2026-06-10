package com.spam.financialaccounting.presentation.exception.openingbalance;

import java.math.BigDecimal;

/**
 * Thrown when a set of opening balances breaks the basic accounting rule: total
 * opening debits must equal total opening credits.
 *
 * <p>If they don't match, the books would start out with a trial balance that
 * doesn't tie, so we reject the whole import and save nothing.
 */
public class OpeningBalanceImbalanceException extends RuntimeException {

    public OpeningBalanceImbalanceException(BigDecimal totalDebits, BigDecimal totalCredits) {
        super(buildMessage(totalDebits, totalCredits));
    }

    private static String buildMessage(BigDecimal totalDebits, BigDecimal totalCredits) {
        BigDecimal difference = totalDebits.subtract(totalCredits).abs();
        String heavierSide = totalDebits.compareTo(totalCredits) > 0 ? "DR" : "CR";
        return "Opening balances are out of balance: total debits ("
                + totalDebits.toPlainString() + ") must equal total credits ("
                + totalCredits.toPlainString() + "); difference of "
                + difference.toPlainString() + " on the " + heavierSide + " side. "
                + "Adjust the entries so total debits equal total credits before importing.";
    }
}
