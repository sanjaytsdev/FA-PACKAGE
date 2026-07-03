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
 * A few notes on the setup:
 * - @JdbcTest runs schema.sql, which creates all 4 tables (FAGroup, FASubGroup,
 * JournalMaster, JournalDetail) with their FK constraints.
 * - Don't drop/recreate JournalMaster, since JournalDetail has a FK on it.
 * - We DELETE FROM to clear data between tests instead (fast, no DDL).
 * - Clear JournalDetail before JournalMaster so the FK is happy.
 * - J_DATE is a DATETIME column in schema.sql, so we hand LocalDateTime straight
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

    // Clear data, not DDL; schema.sql already made the tables.
    // Clear JournalDetail before JournalMaster because of the FK.
    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("DELETE FROM JournalDetail");
        jdbcTemplate.execute("DELETE FROM JournalMaster");
        jdbcTemplate.execute("DELETE FROM JournalSequence");
    }

    // HELPER: insert a row directly, skipping save()'s guard logic.
    private void insertRow(String jId, String jDoc, LocalDateTime jDate, BigDecimal amount, String jNarr) {
        // J_DATE is VARCHAR(30) in the test schema, so store an ISO-8601 string
        // (matches SQLite's TEXT behaviour)
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
        // J_NARR is VARCHAR, so it comes back exactly, no CHAR padding to trim.
        assertThat(result.get().getJNarr()).isEqualTo("Entry");
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
        // J_NARR is VARCHAR, so it comes back exactly, no CHAR padding to trim.
        assertThat(fromDB.get().getJNarr()).isEqualTo("Updated");
    }

    // Regression: a narration shorter than the column must come back with no
    // trailing spaces, so API responses never need .trim(). A CHAR(100) column
    // would pad it out to 100 chars and fail both assertions.
    @Test
    @DisplayName("[T-whitespace] J_NARR must not be space-padded on read")
    void findById_ShouldReturnNarrationWithoutTrailingPadding() {
        insertRow("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Cash");

        Optional<JournalMaster> result = repository.findById("JV00000001");

        assertThat(result).isPresent();
        String narration = result.get().getJNarr();
        assertThat(narration).isEqualTo("Cash");
        assertThat(narration).hasSize(4);
        assertThat(narration).doesNotEndWith(" ");
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
    @DisplayName("[T15] generateNextId() should return JV{year}000001 (12 chars) when table is empty")
    void generateNextId_ShouldReturnFirstId_WhenTableIsEmpty() {
        int year = LocalDateTime.now().getYear();
        String expected = String.format("JV%d000001", year);

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(expected);
        assertThat(result).hasSize(12);
    }

    @Test
    @DisplayName("[T16] generateNextId() should issue sequential ids on consecutive calls")
    void generateNextId_ShouldIncrementSequence_OnConsecutiveCalls() {
        int year = LocalDateTime.now().getYear();

        assertThat(repository.generateNextId()).isEqualTo(String.format("JV%d000001", year));
        assertThat(repository.generateNextId()).isEqualTo(String.format("JV%d000002", year));
        assertThat(repository.generateNextId()).isEqualTo(String.format("JV%d000003", year));
    }

    @Test
    @DisplayName("[T17] generateNextId() draws from the sequence counter, not existing JournalMaster rows")
    void generateNextId_ShouldBeIndependentOfJournalMasterRows() {
        int year = LocalDateTime.now().getYear();
        // A manually inserted legacy-format row shouldn't affect allocation:
        // the counter is what counts, so the first id is still 000001.
        insertRow(String.format("JV%d0099", year), "JV", FIXED_DATE, BigDecimal.ONE, "Manual");

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(String.format("JV%d000001", year));
    }

    // HELPER: preset the per-year counter so we can push generateNextId() up to a
    // boundary (e.g. 9999) without handing out thousands of ids first.
    private void setSequence(int year, int lastVal) {
        int updated = jdbcTemplate.update(
                "UPDATE JournalSequence SET LAST_VAL = ? WHERE SEQ_YEAR = ?", lastVal, year);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO JournalSequence (SEQ_YEAR, LAST_VAL) VALUES (?, ?)", year, lastVal);
        }
    }

    @Test
    @DisplayName("[T18] generateNextId() at sequence 9999 yields a 12-char id, no overflow")
    void generateNextId_AtSequence9999() {
        int year = LocalDateTime.now().getYear();
        setSequence(year, 9998); // next allocation is 9999

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(String.format("JV%d009999", year));
        assertThat(result).hasSize(12);
    }

    @Test
    @DisplayName("[T19] generateNextId() at sequence 10000 stays 12 chars and persists as a valid PK")
    void generateNextId_AtSequence10000_DoesNotOverflowPrimaryKey() {
        int year = LocalDateTime.now().getYear();
        setSequence(year, 9999); // next allocation is 10000 — the old format's failure point

        String result = repository.generateNextId();

        assertThat(result).isEqualTo(String.format("JV%d010000", year));
        assertThat(result).hasSize(12);

        // Check the id works as a primary key at this width: insert and read it back.
        insertRow(result, "JV", FIXED_DATE, new BigDecimal("1.00"), "Boundary");
        assertThat(repository.findById(result)).isPresent();
    }

    @Test
    @DisplayName("[T20] high-volume generation across the 9999→10000 boundary stays unique and 12 chars")
    void generateNextId_HighVolume_RemainsUniqueAndFixedWidth() {
        int year = LocalDateTime.now().getYear();
        setSequence(year, 9990); // start just below the old overflow point

        java.util.Set<String> ids = new java.util.HashSet<>();
        String at9999 = null;
        String at10000 = null;
        for (int seq = 9991; seq <= 10500; seq++) {
            String id = repository.generateNextId();
            assertThat(ids.add(id)).as("id %s must be unique", id).isTrue(); // no collisions
            assertThat(id).hasSize(12).startsWith("JV" + year);
            if (seq == 9999) {
                at9999 = id;
            }
            if (seq == 10000) {
                at10000 = id;
            }
        }

        assertThat(ids).hasSize(510); // every allocation distinct
        assertThat(at9999).isEqualTo(String.format("JV%d009999", year));
        assertThat(at10000).isEqualTo(String.format("JV%d010000", year));
    }
}
