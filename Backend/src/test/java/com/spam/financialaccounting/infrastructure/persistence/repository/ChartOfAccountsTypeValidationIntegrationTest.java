package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.spam.financialaccounting.application.usecases.fasubgroup.CreateFASubGroup;
import com.spam.financialaccounting.application.usecases.fasubgroup.UpdateFASubGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupRowMapper;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupRowMapper;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailRowMapper;
import com.spam.financialaccounting.presentation.exception.fasubgroup.IncompatibleAccountTypeException;

/**
 * Integration test for the chart-of-accounts classification rule: a ledger
 * account's subtype (S_TYPE) has to be in the same class as its parent group's
 * type (A_TYPE). The real create/update use cases run against the real JDBC
 * repositories on the H2 test schema, so the rule is checked end-to-end against
 * actual persisted rows.
 */
@JdbcTest
@Import({ CreateFASubGroup.class, UpdateFASubGroup.class,
        FASubGroupRepositoryJDBC.class, FASubGroupRowMapper.class,
        FAGroupRepositoryJDBC.class, FAGroupRowMapper.class,
        JournalDetailRepositoryJDBC.class, JournalDetailRowMapper.class,
        AuditLogRepository.class })
public class ChartOfAccountsTypeValidationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreateFASubGroup createFASubGroup;

    @Autowired
    private UpdateFASubGroup updateFASubGroup;

    @BeforeEach
    void seedGroups() {
        // Asset group (A_TYPE '0') and Income group (A_TYPE '3').
        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES ('01', 'Assets', '0', 0)");
        jdbcTemplate.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES ('04', 'Income', '3', 0)");
    }

    private FASubGroup subGroup(String sCode, String aCode, String sType, String drCr) {
        return new FASubGroup(sCode, "Acct " + sCode, aCode, sType, BigDecimal.ZERO, drCr, "T");
    }

    private int countSubGroup(String sCode) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM FASubGroup WHERE S_CODE = ?", Integer.class, sCode);
    }

    @Test
    @DisplayName("create() persists the account when S_TYPE matches the Asset group's A_TYPE")
    void createSucceeds_whenSubTypeMatchesGroupType() {
        createFASubGroup.execute(subGroup("10001", "01", "00", "DR"));

        assertThat(countSubGroup("10001")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT S_TYPE FROM FASubGroup WHERE S_CODE = '10001'", String.class).trim())
                .isEqualTo("00");
    }

    @Test
    @DisplayName("create() rejects an Income subtype filed under an Asset group and persists nothing")
    void createRejects_whenSubTypeBelongsToDifferentClass() {
        // S_TYPE '30' is an Income (class 3) subtype, but group '01' is an Asset (class 0).
        assertThatThrownBy(() -> createFASubGroup.execute(subGroup("10002", "01", "30", "DR")))
                .isInstanceOf(IncompatibleAccountTypeException.class)
                .hasMessageContaining("subtype")
                .hasMessageContaining("'30'")
                .hasMessageContaining("Asset");

        assertThat(countSubGroup("10002")).isZero();
    }

    @Test
    @DisplayName("create() persists an Income subtype filed under an Income group")
    void createSucceeds_forMatchingIncomeGroup() {
        createFASubGroup.execute(subGroup("40001", "04", "30", "CR"));

        assertThat(countSubGroup("40001")).isEqualTo(1);
    }

    @Test
    @DisplayName("update() rejects changing S_TYPE to a class that no longer matches the group")
    void updateRejects_whenSubTypeBecomesIncompatible() {
        createFASubGroup.execute(subGroup("10003", "01", "00", "DR"));

        // Try to retag it as a Liability (class 1) subtype while still under the Asset group.
        assertThatThrownBy(() -> updateFASubGroup.execute(subGroup("10003", "01", "10", "DR")))
                .isInstanceOf(IncompatibleAccountTypeException.class)
                .hasMessageContaining("'10'");

        // The stored subtype is unchanged.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT S_TYPE FROM FASubGroup WHERE S_CODE = '10003'", String.class).trim())
                .isEqualTo("00");
    }

    @Test
    @DisplayName("update() allows a sub-classification change that stays within the group's class")
    void updateSucceeds_whenSubTypeStaysWithinSameClass() {
        createFASubGroup.execute(subGroup("10004", "01", "00", "DR"));

        assertThatCode(() -> updateFASubGroup.execute(subGroup("10004", "01", "01", "DR")))
                .doesNotThrowAnyException();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT S_TYPE FROM FASubGroup WHERE S_CODE = '10004'", String.class).trim())
                .isEqualTo("01");
    }
}
