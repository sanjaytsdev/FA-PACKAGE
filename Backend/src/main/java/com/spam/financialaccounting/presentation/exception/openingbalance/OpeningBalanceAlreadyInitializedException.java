package com.spam.financialaccounting.presentation.exception.openingbalance;

/**
 * Thrown when opening balances are imported a second time. The books get set up
 * once and only once; a second import would post duplicate opening balances, so
 * we reject it.
 */
public class OpeningBalanceAlreadyInitializedException extends RuntimeException {

    public OpeningBalanceAlreadyInitializedException(String message) {
        super(message);
    }
}
