package com.spam.financialaccounting.presentation.exception.periodlock;

public abstract class PeriodLockException extends RuntimeException {

    public PeriodLockException(String message) {
        super(message);
    }
}
