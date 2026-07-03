package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailRowMapper;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@JdbcTest
@Import({ JournalDetailRepositoryJDBC.class, JournalDetailRowMapper.class })
public class JournalDetailRepositoryJDBCTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JournalDetailRepositoryJDBC repository;

    // ─────────────────────────────────────────────────────────
    // DDL: Create the JournalDetail table before each test
    // H2 in-memory DB is used — clean state guaranteed per test
    // ─────────────────────────────────────────────────────────
    @BeforeEach
    void setUpTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS JournalDetail");
        jdbcTemplate.execute("""
                CREATE TABLE JournalDetail (
                    J_ID     VARCHAR(10)    NOT NULL,
                    J_CODE   VARCHAR(5)     NOT NULL,
                    J_DRCR   VARCHAR(2)     NOT NULL,
                    J_AMOUNT DECIMAL(15, 2) NOT NULL,
                    PRIMARY KEY (J_ID, J_CODE, J_DRCR)
                )
                """);
    }

    // ─────────────────────────────────────────────────────────
    // HELPER: Insert a row directly without depending on save()
    // ─────────────────────────────────────────────────────────
    private void insertRow(String jId, String jCode, String jDrCr, BigDecimal amount) {
        jdbcTemplate.update(
                "INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) VALUES (?, ?, ?, ?)",
                jId, jCode, jDrCr, amount);
    }

    // ═══════════════════════════════════════════════════════════
    // save(JournalDetail)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T01] save() should insert a row into the database")
    void save_ShouldInsertRow() {
        // ARRANGE
        JournalDetail detail = new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        // ACT
        repository.save(detail);

        // ASSERT — row physically exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM JournalDetail WHERE J_ID='V001000001' AND J_CODE='SG001' AND J_DRCR='DR'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("[T02] save() then findByCompositeKey() should return the persisted data")
    void save_ThenFind_ShouldReturnSavedData() {
        // ARRANGE + ACT
        repository.save(new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00")));
        Optional<JournalDetail> result = repository.findByCompositeKey("V001000001", "SG001", "DR");

        // ASSERT
        assertThat(result).isPresent();
        assertThat(result.get().getJAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("[T03] save() should throw JournalDetailAlreadyExistsException for duplicate composite key")
    void save_ShouldThrow_WhenDuplicateCompositeKey() {
        // ARRANGE — insert the row first
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        JournalDetail duplicate = new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("300.00"));

        // ACT + ASSERT — save() checks existsByCompositeKey internally and throws
        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(JournalDetailAlreadyExistsException.class)
                .hasMessageContaining("This journal line already exists.");
    }

    // ═══════════════════════════════════════════════════════════
    // findByCompositeKey(String, String, String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T04] findByCompositeKey() should return the detail when key exists")
    void findByCompositeKey_ShouldReturnDetail_WhenExists() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        // ACT
        Optional<JournalDetail> result = repository.findByCompositeKey("V001000001", "SG001", "DR");

        // ASSERT — all four fields correctly mapped
        assertThat(result).isPresent();
        assertThat(result.get().getJId()).isEqualTo("V001000001");
        assertThat(result.get().getJCode()).isEqualTo("SG001");
        assertThat(result.get().getJDrCr()).isEqualTo("DR");
        assertThat(result.get().getJAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("[T05] findByCompositeKey() should return Optional.empty() when key does not exist")
    void findByCompositeKey_ShouldReturnEmpty_WhenNotFound() {
        // ACT — table is empty
        Optional<JournalDetail> result = repository.findByCompositeKey("V999999999", "SG999", "DR");

        // ASSERT
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T06] findByCompositeKey() should return empty if only part of the key matches")
    void findByCompositeKey_ShouldReturnEmpty_WhenPartialKeyMatch() {
        // ARRANGE — row for "DR" exists but we query "CR"
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        // ACT
        Optional<JournalDetail> result = repository.findByCompositeKey("V001000001", "SG001", "CR");

        // ASSERT
        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // findByJournalId(String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T07] findByJournalId() should return all rows for a given voucher ID")
    void findByJournalId_ShouldReturnAllMatchingRows() {
        // ARRANGE — two lines for V001, one for V002
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V001000001", "SG002", "CR", new BigDecimal("500.00"));
        insertRow("V002000001", "SG001", "DR", new BigDecimal("100.00"));

        // ACT
        List<JournalDetail> result = repository.findByJournalId("V001000001");

        // ASSERT — only the two V001 rows
        assertThat(result).hasSize(2);
        assertThat(result).extracting(JournalDetail::getJId).containsOnly("V001000001");
    }

    @Test
    @DisplayName("[T08] findByJournalId() should return empty list when voucher ID has no entries")
    void findByJournalId_ShouldReturnEmptyList_WhenNoneFound() {
        List<JournalDetail> result = repository.findByJournalId("V999999999");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // findByAccountCode(String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T09] findByAccountCode() should return all rows for a given account code")
    void findByAccountCode_ShouldReturnAllMatchingRows() {
        // ARRANGE — two rows for SG001, one for SG002
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V002000001", "SG001", "CR", new BigDecimal("300.00"));
        insertRow("V001000001", "SG002", "CR", new BigDecimal("200.00"));

        // ACT
        List<JournalDetail> result = repository.findByAccountCode("SG001");

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(JournalDetail::getJCode).containsOnly("SG001");
    }

    @Test
    @DisplayName("[T10] findByAccountCode() should return empty list when account code has no entries")
    void findByAccountCode_ShouldReturnEmptyList_WhenNoneFound() {
        List<JournalDetail> result = repository.findByAccountCode("SG999");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // findAll()
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T11] findAll() should return all rows in the table")
    void findAll_ShouldReturnAllRows() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V001000001", "SG002", "CR", new BigDecimal("500.00"));
        insertRow("V002000001", "SG001", "DR", new BigDecimal("100.00"));

        // ACT
        List<JournalDetail> result = repository.findAll();

        // ASSERT
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("[T12] findAll() should return empty list when table is empty")
    void findAll_ShouldReturnEmptyList_WhenTableIsEmpty() {
        List<JournalDetail> result = repository.findAll();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // update(JournalDetail)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T13] update() should persist the new amount for an existing composite key")
    void update_ShouldPersistNewAmount_WhenKeyExists() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        JournalDetail updated = new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("999.00"));

        // ACT
        JournalDetail result = repository.update(updated);

        // ASSERT — returned object matches
        assertThat(result.getJAmount()).isEqualByComparingTo("999.00");

        // ASSERT — change actually hit the database
        Optional<JournalDetail> fromDB = repository.findByCompositeKey("V001000001", "SG001", "DR");
        assertThat(fromDB).isPresent();
        assertThat(fromDB.get().getJAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    @DisplayName("[T14] update() should throw JournalDetailNotFoundException when key does not exist")
    void update_ShouldThrow_WhenKeyNotFound() {
        // ARRANGE — table is empty
        JournalDetail ghost = new JournalDetail("V999999999", "SG999", "DR", new BigDecimal("100.00"));

        // ACT + ASSERT — the impl checks rowsAffected == 0 and throws
        assertThatThrownBy(() -> repository.update(ghost))
                .isInstanceOf(JournalDetailNotFoundException.class)
                .hasMessageContaining("No journal line found to update.");
    }

    // ═══════════════════════════════════════════════════════════
    // deleteByCompositeKey(String, String, String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T15] deleteByCompositeKey() should return true and remove the row when key exists")
    void deleteByCompositeKey_ShouldReturnTrue_AndRemoveRow() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        // ACT
        boolean result = repository.deleteByCompositeKey("V001000001", "SG001", "DR");

        // ASSERT
        assertThat(result).isTrue();

        // ASSERT — row is gone
        Optional<JournalDetail> fromDB = repository.findByCompositeKey("V001000001", "SG001", "DR");
        assertThat(fromDB).isEmpty();
    }

    @Test
    @DisplayName("[T16] deleteByCompositeKey() should return false when key does not exist")
    void deleteByCompositeKey_ShouldReturnFalse_WhenKeyNotFound() {
        boolean result = repository.deleteByCompositeKey("V999999999", "SG999", "DR");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("[T17] deleteByCompositeKey() should only delete the matching row, leaving others intact")
    void deleteByCompositeKey_ShouldOnlyDeleteMatchingRow() {
        // ARRANGE — two rows for the same voucher
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V001000001", "SG002", "CR", new BigDecimal("500.00"));

        // ACT — delete only the DR line
        repository.deleteByCompositeKey("V001000001", "SG001", "DR");

        // ASSERT — CR line still exists
        List<JournalDetail> remaining = repository.findByJournalId("V001000001");
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getJCode()).isEqualTo("SG002");
        assertThat(remaining.get(0).getJDrCr()).isEqualTo("CR");
    }

    // ═══════════════════════════════════════════════════════════
    // deleteByJournalId(String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T18] deleteByJournalId() should return true and remove all rows for that voucher ID")
    void deleteByJournalId_ShouldReturnTrue_AndRemoveAllRows() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V001000001", "SG002", "CR", new BigDecimal("500.00"));

        // ACT
        boolean result = repository.deleteByJournalId("V001000001");

        // ASSERT
        assertThat(result).isTrue();
        assertThat(repository.findByJournalId("V001000001")).isEmpty();
    }

    @Test
    @DisplayName("[T19] deleteByJournalId() should return false when voucher ID has no entries")
    void deleteByJournalId_ShouldReturnFalse_WhenNoneFound() {
        boolean result = repository.deleteByJournalId("V999999999");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("[T20] deleteByJournalId() should only delete rows for the given ID, leaving others intact")
    void deleteByJournalId_ShouldNotAffectOtherVouchers() {
        // ARRANGE
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));
        insertRow("V002000001", "SG001", "DR", new BigDecimal("100.00"));

        // ACT — delete V001 only
        repository.deleteByJournalId("V001000001");

        // ASSERT — V002 row still exists
        assertThat(repository.findByJournalId("V002000001")).hasSize(1);
    }

    // ═══════════════════════════════════════════════════════════
    // existsByCompositeKey(String, String, String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T21] existsByCompositeKey() should return true when all three key parts match")
    void existsByCompositeKey_ShouldReturnTrue_WhenExists() {
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        assertThat(repository.existsByCompositeKey("V001000001", "SG001", "DR")).isTrue();
    }

    @Test
    @DisplayName("[T22] existsByCompositeKey() should return false when key does not exist")
    void existsByCompositeKey_ShouldReturnFalse_WhenNotFound() {
        assertThat(repository.existsByCompositeKey("V999999999", "SG999", "DR")).isFalse();
    }

    @Test
    @DisplayName("[T23] existsByCompositeKey() should return false when jDrCr does not match")
    void existsByCompositeKey_ShouldReturnFalse_WhenDrCrDiffers() {
        // Row exists for DR, but we query for CR
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        assertThat(repository.existsByCompositeKey("V001000001", "SG001", "CR")).isFalse();
    }

    // ═══════════════════════════════════════════════════════════
    // existsByAccountCode(String)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("[T24] existsByAccountCode() should return true when account code has at least one entry")
    void existsByAccountCode_ShouldReturnTrue_WhenExists() {
        insertRow("V001000001", "SG001", "DR", new BigDecimal("500.00"));

        assertThat(repository.existsByAccountCode("SG001")).isTrue();
    }

    @Test
    @DisplayName("[T25] existsByAccountCode() should return false when account code has no entries")
    void existsByAccountCode_ShouldReturnFalse_WhenNotFound() {
        assertThat(repository.existsByAccountCode("SG999")).isFalse();
    }
}
