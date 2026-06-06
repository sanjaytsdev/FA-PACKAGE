package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalMasterRowMapper;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

/**
 * Integration tests for JournalMasterRepositoryJDBC.
 *
 * Design decisions:
 * - @JdbcTest auto-executes schema.sql, which creates ALL 4 tables (FAGroup,
 * FASubGroup,
 * JournalMaster, JournalDetail) including FK constraints.
 * - We must NOT drop/recreate JournalMaster because JournalDetail has a FK on
 * it.
 * - Instead we use DELETE FROM to clear data between tests (safe, fast, no
 * DDL).
 * - We also delete from JournalDetail first to satisfy the FK before clearing
 * JournalMaster.
 * - J_DATE is a DATETIME column in schema.sql — we pass LocalDateTime directly
 * to JDBC.
 */
@JdbcTest
@Import({ JournalMasterRepositoryJDBC.class, JournalMasterRowMapper.class })
public class JournalMasterRepositoryJDBCTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalMasterRepositoryJDBC repository;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);

    // @BeforeEach: clear data (not DDL) — schema.sql already created the tables.
    // JournalDetail must be cleared before JournalMaster due to FK constraint.
    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("DELETE FROM JournalDetail");
        jdbcTemplate.execute("DELETE FROM JournalMaster");
    }

    // HELPER: Insert a row directly, bypassing save() guard logic.
    private void insertRow(String jId, String jDoc, LocalDateTime jDate, BigDecimal amount, String jNarr) {
        // J_DATE is VARCHAR(30) in test schema — store as ISO-8601 string (mirrors
        // SQLite TEXT behaviour)
        jdbcTemplate.update(
                "INSERT INTO JournalMaster (J_ID, J_DOC, J_DATE, J_AMOUNT, J_NARR) VALUES (?, ?, ?, ?, ?)",
                jId, jDoc, jDate.toString(), amount, jNarr);
    }

    // save(JournalMaster)
    @Test
    @DisplayName("[T01] save() should insert a row and return the saved entity")
    void save_ShouldInsertRow() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test");

        JournalMaster result = repository.save(master);

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("JV00000001");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM JournalMaster WHERE J_ID = 'JV00000001'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("[T02] save() should throw JournalMasterAlreadyExistsException for duplicate ID")
    void save_ShouldThrow_WhenDuplicateId() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "First");
        JournalMaster duplicate = new JournalMaster("JV00000001", "PV", FIXED_DATE, new BigDecimal("500.00"), "Dup");

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(JournalMasterAlreadyExistsException.class)
                .hasMessageContaining("JV00000001");
    }

    // findById(String)
    @Test
    @DisplayName("[T03] findById() should return present Optional when ID exists")
    void findById_ShouldReturnPresent_WhenExists() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Entry");

        Optional<JournalMaster> result = repository.findById("JV00000001");

        assertThat(result).isPresent();
        assertThat(result.get().getJId()).isEqualTo("JV00000001");
        assertThat(result.get().getJDoc()).isEqualTo("JV");
        assertThat(result.get().getJAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.get().getJNarr().trim())
                .isEqualTo("Entry");
    }

    @Test
    @DisplayName("[T04] findById() should return empty Optional when ID does not exist")
    void findById_ShouldReturnEmpty_WhenNotFound() {
        Optional<JournalMaster> result = repository.findById("JV99999999");

        assertThat(result).isEmpty();
    }

    // findAll()
    @Test
    @DisplayName("[T05] findAll() should return all rows in the table")
    void findAll_ShouldReturnAllRows() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "A");
        insertRow("JV00000002", "PV", FIXED_DATE, new BigDecimal("500.00"), "B");

        List<JournalMaster> result = repository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("[T06] findAll() should return empty list when table is empty")
    void findAll_ShouldReturnEmptyList_WhenEmpty() {
        List<JournalMaster> result = repository.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // update(JournalMaster)
    @Test
    @DisplayName("[T07] update() should persist new field values when ID exists")
    void update_ShouldPersistChanges_WhenExists() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Original");
        LocalDateTime newDate = LocalDateTime.of(2025, 6, 1, 9, 0);
        JournalMaster updated = new JournalMaster("JV00000001", "PV", newDate, new BigDecimal("2000.00"), "Updated");

        JournalMaster result = repository.update(updated);

        assertThat(result.getJDoc()).isEqualTo("PV");
        assertThat(result.getJAmount()).isEqualByComparingTo("2000.00");

        // Verify the change actually hit the database
        Optional<JournalMaster> fromDB = repository.findById("JV00000001");
        assertThat(fromDB).isPresent();
        assertThat(fromDB.get().getJDoc()).isEqualTo("PV");
        assertThat(fromDB.get().getJAmount()).isEqualByComparingTo("2000.00");
        assertThat(fromDB.get().getJNarr().trim())
                .isEqualTo("Updated");
    }

    @Test
    @DisplayName("[T08] update() should throw JournalMasterNotFoundException when ID does not exist")
    void update_ShouldThrow_WhenNotFound() {
        JournalMaster ghost = new JournalMaster("JV99999999", "JV", FIXED_DATE, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> repository.update(ghost))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("JV99999999");
    }

    // delete(String)
    @Test
    @DisplayName("[T09] delete() should return true and remove the row when ID exists")
    void delete_ShouldReturnTrue_AndRemoveRow() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Entry");

        boolean result = repository.delete("JV00000001");

        assertThat(result).isTrue();
        assertThat(repository.findById("JV00000001")).isEmpty();
    }

    @Test
    @DisplayName("[T10] delete() should return false when ID does not exist")
    void delete_ShouldReturnFalse_WhenNotFound() {
        boolean result = repository.delete("JV99999999");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("[T11] delete() should only remove the matching row, leaving others intact")
    void delete_ShouldOnlyRemoveMatchingRow() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "A");
        insertRow("JV00000002", "PV", FIXED_DATE, new BigDecimal("500.00"), "B");

        repository.delete("JV00000001");

        assertThat(repository.findById("JV00000002")).isPresent();
        assertThat(repository.findAll()).hasSize(1);
    }

    // existsById(String)
    @Test
    @DisplayName("[T12] existsById() should return true when ID exists")
    void existsById_ShouldReturnTrue_WhenExists() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Entry");

        assertThat(repository.existsById("JV00000001")).isTrue();
    }

    @Test
    @DisplayName("[T13] existsById() should return false when ID does not exist")
    void existsById_ShouldReturnFalse_WhenNotFound() {
        assertThat(repository.existsById("JV99999999")).isFalse();
    }

    @Test
    @DisplayName("[T14] existsById() should return false after the row is deleted")
    void existsById_ShouldReturnFalse_AfterDeletion() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Entry");

        repository.delete("JV00000001");

        assertThat(repository.existsById("JV00000001")).isFalse();
    }

    // generateNextId()

    @Test
    @DisplayName("[T15] generateNextId() should return JV{year}0001 when table is empty")
    void generateNextId_ShouldReturnFirstId_WhenTableIsEmpty() {
        int year = LocalDateTime.now().getYear();
        String expected = String.format("JV%d0001", year);

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(expected);
        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("[T16] generateNextId() should increment sequence from existing max ID in current year")
    void generateNextId_ShouldIncrementSequence_WhenRecordsExist() {
        int year = LocalDateTime.now().getYear();
        insertRow(String.format("JV%d0005", year), "JV", FIXED_DATE, BigDecimal.ONE, "Entry");

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(String.format("JV%d0006", year));
    }

    @Test
    @DisplayName("[T17] generateNextId() should ignore records from previous years")
    void generateNextId_ShouldIgnorePreviousYearRecords() {
        int year = LocalDateTime.now().getYear();
        // Insert a high-sequence ID from the previous year
        insertRow(String.format("JV%d0099", year - 1), "JV", FIXED_DATE, BigDecimal.ONE, "Old");

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(String.format("JV%d0001", year));
    }
}
