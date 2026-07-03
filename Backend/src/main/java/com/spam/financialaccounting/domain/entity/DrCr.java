package com.spam.financialaccounting.domain.entity;

import java.util.Locale;

/**
 * The debit/credit indicator. We only ever store and compare two values,
 * {@value #DEBIT} and {@value #CREDIT}, and every entry point normalizes user
 * input to one of them before it hits the domain or the database (whose CHECK
 * constraint also only allows 'DR'/'CR').
 *
 * <p>Call {@link #normalizeOrNull(String)} at each service boundary. That way
 * input is accepted regardless of case (e.g. "dr", "Dr") but always saved in
 * uppercase. Keeps bean validation (case-insensitive pattern) and service
 * validation in sync so every endpoint behaves the same.
 */
public final class DrCr {

    public static final String DEBIT = "DR";
    public static final String CREDIT = "CR";

    private DrCr() {
    }

    /**
     * Normalizes a debit/credit indicator: trims whitespace and upper-cases,
     * returning {@value #DEBIT} or {@value #CREDIT}. Returns {@code null} if the
     * input is null or isn't a debit/credit value, so each caller can raise its
     * own validation error.
     */
    public static String normalizeOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return DEBIT.equals(value) || CREDIT.equals(value) ? value : null;
    }

    /** True if {@code raw} is a debit/credit value, ignoring case and whitespace. */
    public static boolean isValid(String raw) {
        return normalizeOrNull(raw) != null;
    }
}
