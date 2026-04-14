package com.spam.financialaccounting.presentation.exception.journaldetail;

public abstract class JournalDetailException extends RuntimeException {

    public JournalDetailException(String message) {
        super(message);
    }

    public JournalDetailException(String message, Throwable cause) {
        super(message, cause);
    }

}
