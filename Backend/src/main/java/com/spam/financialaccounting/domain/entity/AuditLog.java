package com.spam.financialaccounting.domain.entity;

/**
 * One append-only audit trail entry. {@code eventTime} is the ISO-8601 timestamp
 * of the action. It's stored and returned as text to match how we handle dates
 * elsewhere (see JournalMaster.J_DATE).
 */
public record AuditLog(
        long id,
        String actor,
        String action,
        String entityType,
        String entityId,
        String eventTime,
        String detail) {
}
