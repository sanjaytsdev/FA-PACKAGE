package com.spam.financialaccounting.presentation.exception.journalmaster;

public abstract class JournalMasterException extends RuntimeException {

    public JournalMasterException(String message) {
        super(message);
    }

    public JournalMasterException(String message, Throwable cause) {
        super(message, cause);
    }

}
