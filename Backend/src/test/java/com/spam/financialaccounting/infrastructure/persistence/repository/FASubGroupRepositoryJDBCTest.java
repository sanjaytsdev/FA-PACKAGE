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

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupRowMapper;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({ FASubGroupRepositoryJDBC.class, FASubGroupRowMapper.class })
public class FASubGroupRepositoryJDBCTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FASubGroupRepositoryJDBC repository;

    @BeforeEach
    void setUpTable() {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        
        jdbcTemplate.execute("""
            CREATE TABLE FAGroup (
                A_CODE  VARCHAR(2)        NOT NULL PRIMARY KEY,
                A_DESC  VARCHAR(50)       NOT NULL,
                A_TYPE  VARCHAR(1)        NOT NULL,
                A_CURRB DECIMAL(18, 3)    NOT NULL DEFAULT 0
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE FASubGroup (
                S_CODE  VARCHAR(5)        NOT NULL PRIMARY KEY,
                S_DESC  VARCHAR(50)       NOT NULL,
                A_CODE  VARCHAR(2)        NOT NULL,
                S_TYPE  VARCHAR(2)        NOT NULL,
                S_OPBAL DECIMAL(18, 3)    NOT NULL DEFAULT 0,
                S_DRCR  VARCHAR(2)        NOT NULL,
                S_FLAG  VARCHAR(1)        NOT NULL DEFAULT 'T',
                FOREIGN KEY (A_CODE) REFERENCES FAGroup (A_CODE)
            )
        """);

        // Insert a default group so foreign key doesn't fail
        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?, ?, ?, ?)", 
                "01", "Asset", "0", BigDecimal.ZERO);
    }

    private void insertRow(String sCode, String sDesc, String aCode, String sType, BigDecimal sOpbal, String sDrCr, String sFlag) {
        jdbcTemplate.update(
                "INSERT INTO FASubGroup (S_CODE, S_DESC, A_CODE, S_TYPE, S_OPBAL, S_DRCR, S_FLAG) VALUES (?,?,?,?,?,?,?)",
                sCode, sDesc, aCode, sType, sOpbal, sDrCr, sFlag);
    }

    @Test
    @DisplayName("[T01] save() should insert a row and return the saved FASubGroup")
    void save_ShouldInsertRowAndReturnFASubGroup() {
        // ARRANGE
        FASubGroup subGroup = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");

        // ACT
        FASubGroup result = repository.save(subGroup);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getSCode()).isEqualTo("10001");
        assertThat(result.getSDesc()).isEqualTo("Cash");
        assertThat(result.getACode()).isEqualTo("01");
        assertThat(result.getSType()).isEqualTo("00");
        assertThat(result.getSOpbal()).isEqualByComparingTo("1000.00");
        assertThat(result.getSDrCr()).isEqualTo("DR");
        assertThat(result.getSFlag()).isEqualTo("T");

        // Check DB row actually exists
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FASubGroup WHERE S_CODE = '10001'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("[T02] save() then findByCode() should return the persisted data")
    void save_ThenFindByCode_ShouldReturnSavedData() {
        // ARRANGE
        FASubGroup subGroup = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");

        // ACT
        repository.save(subGroup);
        Optional<FASubGroup> found = repository.findByCode("10001");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getSDesc()).isEqualTo("Cash");
        assertThat(found.get().getSOpbal()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("[T03] findByCode() should return the FASubGroup when code exists")
    void findByCode_ShouldReturnFASubGroup_WhenCodeExists() {
        // ARRANGE
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");

        // ACT
        Optional<FASubGroup> result = repository.findByCode("10001");

        // ASSERT
        assertThat(result).isPresent();
        assertThat(result.get().getSCode()).isEqualTo("10001");
        assertThat(result.get().getSDesc()).isEqualTo("Cash");
        assertThat(result.get().getACode()).isEqualTo("01");
        assertThat(result.get().getSType()).isEqualTo("00");
        assertThat(result.get().getSOpbal()).isEqualByComparingTo("1000.00");
        assertThat(result.get().getSDrCr()).isEqualTo("DR");
        assertThat(result.get().getSFlag()).isEqualTo("T");
    }

    @Test
    @DisplayName("[T04] findByCode() should return Optional.empty() when code does not exist")
    void findByCode_ShouldReturnEmpty_WhenCodeNotFound() {
        // ACT
        Optional<FASubGroup> result = repository.findByCode("99999");

        // ASSERT
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T05] findAll() should return all rows when multiple exist")
    void findAll_ShouldReturnAllSubGroups() {
        // ARRANGE
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        insertRow("10002", "Bank", "01", "00", new BigDecimal("2000.00"), "DR", "T");

        // ACT
        List<FASubGroup> result = repository.findAll();

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(FASubGroup::getSCode)
                .containsExactlyInAnyOrder("10001", "10002");
    }

    @Test
    @DisplayName("[T06] findAll() should return empty list when table is empty")
    void findAll_ShouldReturnEmptyList_WhenNoSubGroupsExist() {
        // ACT
        List<FASubGroup> result = repository.findAll();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T07] findByACode() should return matching subGroups")
    void findByACode_ShouldReturnMatchingSubGroups() {
        // ARRANGE - Insert another FAGroup to verify filtering
        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?, ?, ?, ?)", 
                "02", "Liability", "1", BigDecimal.ZERO);
        
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        insertRow("10002", "Bank", "01", "00", new BigDecimal("2000.00"), "DR", "T");
        insertRow("20001", "Loan", "02", "01", new BigDecimal("500.00"), "CR", "T");

        // ACT
        List<FASubGroup> group1List = repository.findByACode("01");
        List<FASubGroup> group2List = repository.findByACode("02");

        // ASSERT
        assertThat(group1List).hasSize(2);
        assertThat(group1List).extracting(FASubGroup::getSCode).containsExactlyInAnyOrder("10001", "10002");
        assertThat(group2List).hasSize(1);
        assertThat(group2List).extracting(FASubGroup::getSCode).containsExactly("20001");
    }

    @Test
    @DisplayName("[T08] findByACode() should return empty list when no group matches")
    void findByACode_ShouldReturnEmptyList_WhenNoMatch() {
        // ACT
        List<FASubGroup> result = repository.findByACode("99");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[T09] update() should persist modifications to an existing FASubGroup")
    void update_ShouldPersistChanges_WhenCodeExists() {
        // ARRANGE
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        FASubGroup updated = new FASubGroup("10001", "Petty Cash", "01", "00", new BigDecimal("1500.00"), "DR", "F");

        // ACT
        FASubGroup result = repository.update(updated);

        // ASSERT
        assertThat(result.getSDesc()).isEqualTo("Petty Cash");
        assertThat(result.getSOpbal()).isEqualByComparingTo("1500.00");
        assertThat(result.getSFlag()).isEqualTo("F");

        // Verify database
        Optional<FASubGroup> fromDB = repository.findByCode("10001");
        assertThat(fromDB).isPresent();
        assertThat(fromDB.get().getSDesc()).isEqualTo("Petty Cash");
        assertThat(fromDB.get().getSOpbal()).isEqualByComparingTo("1500.00");
        assertThat(fromDB.get().getSFlag()).isEqualTo("F");
    }

    @Test
    @DisplayName("[T10] update() should return updated entity but do nothing in database if code does not exist")
    void update_ShouldNotThrow_WhenCodeNotFound() {
        // ARRANGE
        FASubGroup ghost = new FASubGroup("99999", "Ghost", "01", "00", BigDecimal.ZERO, "DR", "T");

        // ACT
        FASubGroup result = repository.update(ghost);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(repository.findByCode("99999")).isEmpty();
    }

    @Test
    @DisplayName("[T11] delete() should return true and remove row when code exists")
    void delete_ShouldReturnTrueAndRemoveRow_WhenCodeExists() {
        // ARRANGE
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");

        // ACT
        boolean result = repository.delete("10001");

        // ASSERT
        assertThat(result).isTrue();
        assertThat(repository.findByCode("10001")).isEmpty();
    }

    @Test
    @DisplayName("[T12] delete() should return false when code does not exist")
    void delete_ShouldReturnFalse_WhenCodeNotFound() {
        // ACT
        boolean result = repository.delete("99999");

        // ASSERT
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("[T13] existsByCode() should return true when code exists")
    void existsByCode_ShouldReturnTrue_WhenExists() {
        // ARRANGE
        insertRow("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");

        // ACT & ASSERT
        assertThat(repository.existsByCode("10001")).isTrue();
    }

    @Test
    @DisplayName("[T14] existsByCode() should return false when code does not exist")
    void existsByCode_ShouldReturnFalse_WhenNotFound() {
        // ACT & ASSERT
        assertThat(repository.existsByCode("99999")).isFalse();
    }
}
