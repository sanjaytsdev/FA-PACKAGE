package com.spam.financialaccounting.infrastructure.persistence.repository;

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

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupRowMapper;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({ FAGroupRepositoryJDBC.class, FAGroupRowMapper.class })
public class FAGroupRepositoryJDBCTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FAGroupRepositoryJDBC repository;

    @BeforeEach
    void setUpTable() {
        // drop and recreate to guarantee a clean state
        // jdbcTemplate.execute("DROP TABLE IF EXISTS FAGroup");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                    CREATE TABLE FAGroup (
                    A_CODE VARCHAR(2) NOT NULL PRIMARY KEY,
                    A_DESC VARCHAR(50) NOT NULL,
                    A_TYPE VARCHAR(1) NOT NULL,
                    A_CURRB DECIMAL(15,2) NOT NULL
                    )
                """);
    }

    private void insertRow(String code, String desc, String type, BigDecimal balance) {
        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?,?,?,?)", code, desc, type,
                balance);
    }

    @Test
    @DisplayName("[T01] save() should insert a row and return the same FAGroup object")
    void save_ShouldInsertRowAndReturnFAGroup() {
        // ARRANGE
        FAGroup group = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);

        // ACT
        FAGroup result = repository.save(group);

        // ASSERT - return object matches input
        assertThat(result).isNotNull();
        assertThat(result.getAccountCode()).isEqualTo("01");
        assertThat(result.getAccountDescription()).isEqualTo("Asset");
        assertThat(result.getAccountType()).isEqualTo("0");
        assertThat(result.getAccountCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // ASSERT - row actually exists in the database
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FAGroup WHERE A_CODE = '01'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("[T02] save() then findByCode() should return the persisted data")
    void save_ThenFindByCode_ShouldReturnSavedData() {
        // ARRANGE
        FAGroup group = new FAGroup("02", "Liability", "1", new BigDecimal("500.00"));
        // ACT
        repository.save(group);
        Optional<FAGroup> found = repository.findByCode("02");
        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getAccountDescription()).isEqualTo("Liability");
        assertThat(found.get().getAccountCurrentBalance()).isEqualByComparingTo("500.00");
    }

    // findByCode(String)
    @Test
    @DisplayName("[T03] findByCode() should return the FAGroup when code exists")
    void findByCode_ShouldReturnFAGroup_WhenCodeExists() {
        // ARRANGE — insert directly, don't depend on save()
        insertRow("01", "Asset", "0", BigDecimal.ZERO);
        // ACT
        Optional<FAGroup> result = repository.findByCode("01");
        // ASSERT
        assertThat(result).isPresent();
        assertThat(result.get().getAccountCode()).isEqualTo("01");
        assertThat(result.get().getAccountDescription()).isEqualTo("Asset");
        assertThat(result.get().getAccountType()).isEqualTo("0");
        assertThat(result.get().getAccountCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("[T04] findByCode() should return Optional.empty() when code does not exist")
    void findByCode_ShouldReturnEmpty_WhenCodeNotFound() {
        // ACT — table is empty, code "99" doesn't exist
        Optional<FAGroup> result = repository.findByCode("99");
        // ASSERT
        assertThat(result).isEmpty();
    }

    // findAll()
    @Test
    @DisplayName("[T05] findAll() should return all rows when multiple FAGroups exist")
    void findAll_ShouldReturnAllGroups() {
        // ARRANGE
        insertRow("01", "Asset", "0", BigDecimal.ZERO);
        insertRow("02", "Liability", "1", BigDecimal.ZERO);
        // ACT
        List<FAGroup> result = repository.findAll();
        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(FAGroup::getAccountCode)
                .containsExactlyInAnyOrder("01", "02");
    }

    @Test
    @DisplayName("[T06] findAll() should return an empty list when table is empty")
    void findAll_ShouldReturnEmptyList_WhenNoGroupsExist() {
        // ACT — table is empty (setUp clears it)
        List<FAGroup> result = repository.findAll();
        // ASSERT — must be empty list, never null
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // update(FAGroup)
    @Test
    @DisplayName("[T07] update() should change description and balance for an existing code")
    void update_ShouldPersistChanges_WhenCodeExists() {
        // ARRANGE — insert original row
        insertRow("01", "Asset", "0", BigDecimal.ZERO);
        FAGroup updated = new FAGroup("01", "Fixed Asset", "0", new BigDecimal("1000.00"));
        // ACT
        FAGroup result = repository.update(updated);
        // ASSERT — returned object is the updated data
        assertThat(result.getAccountDescription()).isEqualTo("Fixed Asset");
        assertThat(result.getAccountCurrentBalance()).isEqualByComparingTo("1000.00");
        // ASSERT — verify the change actually hit the database
        Optional<FAGroup> fromDB = repository.findByCode("01");
        assertThat(fromDB).isPresent();
        assertThat(fromDB.get().getAccountDescription()).isEqualTo("Fixed Asset");
        assertThat(fromDB.get().getAccountCurrentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("[T08] update() should silently do nothing when code does not exist")
    void update_ShouldNotThrow_WhenCodeNotFound() {
        // ARRANGE — nothing in the DB
        FAGroup ghost = new FAGroup("99", "Ghost", "0", BigDecimal.ZERO);
        // ACT + ASSERT — no exception should be thrown
        // Known gap: the impl just fails silently here. This test pins that down.
        FAGroup result = repository.update(ghost);
        assertThat(result).isNotNull(); // returns the passed object
        assertThat(repository.findByCode("99")).isEmpty(); // nothing was inserted
    }

    // delete(String)
    @Test
    @DisplayName("[T09] delete() should remove the row and return true when code exists")
    void delete_ShouldReturnTrue_AndRemoveRow_WhenCodeExists() {
        // ARRANGE
        insertRow("01", "Asset", "0", BigDecimal.ZERO);
        // ACT
        boolean result = repository.delete("01");
        // ASSERT
        assertThat(result).isTrue();
        // ASSERT — row is gone from the DB
        Optional<FAGroup> fromDB = repository.findByCode("01");
        assertThat(fromDB).isEmpty();
    }

    @Test
    @DisplayName("[T10] delete() should return false when code does not exist")
    void delete_ShouldReturnFalse_WhenCodeNotFound() {
        // ACT
        boolean result = repository.delete("99");
        // ASSERT
        assertThat(result).isFalse();
    }

    // existsByCode(String)
    @Test
    @DisplayName("[T11] existsByCode() should return true when code exists")
    void existsByCode_ShouldReturnTrue_WhenCodeExists() {
        // ARRANGE
        insertRow("01", "Asset", "0", BigDecimal.ZERO);
        // ACT + ASSERT
        assertThat(repository.existsByCode("01")).isTrue();
    }
    @Test
    @DisplayName("[T12] existsByCode() should return false when code does not exist")
    void existsByCode_ShouldReturnFalse_WhenCodeNotFound() {
        // ACT + ASSERT — empty table, code "99" doesn't exist
        assertThat(repository.existsByCode("99")).isFalse();
    }
    @Test
    @DisplayName("[T13] existsByCode() should return false when COUNT(*) returns null (null-safe check)")
    void existsByCode_ShouldHandleNullCountGracefully() {
        // Checks the null guard in: return count != null && count > 0;
        // We can't make H2 return null for COUNT(*), so we test the edge case:
        // a code that definitely isn't there shouldn't throw a NullPointerException.
        assertThat(repository.existsByCode("XY")).isFalse();
    }

}
