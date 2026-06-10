package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.AuditLog;

/**
 * Append-only audit trail. Every successful write in the system adds one row
 * here; there's deliberately no update or delete method.
 *
 * Timestamps are stored as ISO-8601 strings, same as the J_DATE convention used
 * elsewhere (SQLite stores DATETIME as TEXT).
 *
 * The event timestamp column is EVENT_TIME, not TIMESTAMP. TIMESTAMP is a
 * reserved word in MySQL and we'd have to back-quote it everywhere; EVENT_TIME
 * avoids that on every engine we support.
 */
@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void append(String actor, String action, String entityType, String entityId, String detail) {
        String sql = "INSERT INTO audit_log (ACTOR, ACTION, ENTITY_TYPE, ENTITY_ID, EVENT_TIME, DETAIL) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, actor, action, entityType, entityId, LocalDateTime.now().toString(), detail);
    }

    /** All audit entries in insertion order (ascending auto-increment ID). */
    public List<AuditLog> findAll() {
        String sql = "SELECT ID, ACTOR, ACTION, ENTITY_TYPE, ENTITY_ID, EVENT_TIME, DETAIL "
                + "FROM audit_log ORDER BY ID";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuditLog(
                rs.getLong("ID"),
                rs.getString("ACTOR"),
                rs.getString("ACTION"),
                rs.getString("ENTITY_TYPE"),
                rs.getString("ENTITY_ID"),
                rs.getString("EVENT_TIME"),
                rs.getString("DETAIL")));
    }
}
