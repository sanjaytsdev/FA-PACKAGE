package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.spam.financialaccounting.domain.entity.AuditLog;

/**
 * Checks audit-log persistence against the production MySQL DDL
 * (schema-mysql.sql), not the SQLite schema. The datasource is H2 in MySQL
 * compatibility mode, so the MySQL-only bits actually run: BIGINT AUTO_INCREMENT
 * for the primary key and the EVENT_TIME column that replaced the reserved word
 * TIMESTAMP.
 *
 * Walks the whole audit lifecycle (create table, insert, read back) for the
 * three audited write actions: POST and REVERSE (JournalVoucher) and DELETE
 * (LedgerAccount).
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditLogRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_mysql;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // The dev profile sets a SQLite-only PRAGMA as the Hikari
        // connection-init-sql; clear it so it doesn't run on the H2 connection.
        "spring.datasource.hikari.connection-init-sql=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-mysql.sql"
})
class AuditLogRepositoryMySqlIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("audit_log is created from the MySQL DDL and starts empty")
    void schemaCreatesEmptyAuditLog() {
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("append persists POST, REVERSE and DELETE entries; findAll retrieves them in order")
    void appendsAndRetrievesPostReverseDelete() {
        auditLogRepository.append("SYSTEM", "POST", "JournalVoucher", "JV2026000001", "Posted voucher");
        auditLogRepository.append("SYSTEM", "REVERSE", "JournalVoucher", "JV2026000002", "Reversal of JV2026000001");
        auditLogRepository.append("SYSTEM", "DELETE", "LedgerAccount", "10001", "Deleted ledger account");

        List<AuditLog> entries = auditLogRepository.findAll();

        assertThat(entries).hasSize(3);
        assertThat(entries).extracting(AuditLog::action)
                .containsExactly("POST", "REVERSE", "DELETE");
        assertThat(entries).extracting(AuditLog::entityType)
                .containsExactly("JournalVoucher", "JournalVoucher", "LedgerAccount");
        assertThat(entries).extracting(AuditLog::entityId)
                .containsExactly("JV2026000001", "JV2026000002", "10001");
        assertThat(entries).extracting(AuditLog::detail)
                .containsExactly("Posted voucher", "Reversal of JV2026000001", "Deleted ledger account");

        // AUTO_INCREMENT gave each row a unique, increasing id.
        assertThat(entries).extracting(AuditLog::id).doesNotHaveDuplicates();
        assertThat(entries.get(0).id()).isLessThan(entries.get(1).id());
        assertThat(entries.get(1).id()).isLessThan(entries.get(2).id());

        // EVENT_TIME round-trips the ISO-8601 timestamp the app wrote (the 'T' gives it away).
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.actor()).isEqualTo("SYSTEM");
            assertThat(e.eventTime()).contains("T");
        });
    }
}
