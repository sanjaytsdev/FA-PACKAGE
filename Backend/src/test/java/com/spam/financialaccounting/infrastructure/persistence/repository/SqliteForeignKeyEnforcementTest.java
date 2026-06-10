package com.spam.financialaccounting.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Integration tests showing SQLite actually enforces the foreign keys from
 * schema.sql at the DB level.
 *
 * WHY A REAL SQLITE DB (not @JdbcTest/H2):
 *   The rest of the suite runs on H2, which always enforces foreign keys. That
 *   tells us nothing about SQLite, where enforcement is OFF per-connection by
 *   default and has to be switched on with {@code PRAGMA foreign_keys = ON}.
 *   These tests use an in-memory SQLite DB so we're testing the real production
 *   behaviour.
 *
 * We use a {@link SingleConnectionDataSource} so the in-memory DB (which only
 * lives as long as its connection) survives across statements, and so the
 * per-connection pragma covers every query in the test.
 *
 * The DDL below copies the foreign keys from src/main/resources/schema.sql
 * (FASubGroup -> FAGroup, JournalDetail -> { JournalMaster, FASubGroup },
 * JournalMaster.REVERSES/REVERSED_BY -> JournalMaster).
 */
class SqliteForeignKeyEnforcementTest {

    private SingleConnectionDataSource dataSource;

    private JdbcTemplate withForeignKeys(boolean enabled) {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.execute("PRAGMA foreign_keys = " + (enabled ? "ON" : "OFF"));
        createSchema(jdbc);
        seedValidParents(jdbc);
        return jdbc;
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("""
                CREATE TABLE FAGroup (
                    A_CODE  CHAR(2) PRIMARY KEY,
                    A_DESC  CHAR(50) NOT NULL,
                    A_TYPE  CHAR(1)  NOT NULL,
                    A_CURRB DECIMAL(18,3) NOT NULL DEFAULT 0
                )""");
        jdbc.execute("""
                CREATE TABLE FASubGroup (
                    S_CODE  CHAR(5) PRIMARY KEY,
                    S_DESC  CHAR(50) NOT NULL,
                    A_CODE  CHAR(2)  NOT NULL,
                    S_TYPE  CHAR(2)  NOT NULL,
                    S_OPBAL DECIMAL(18,3) NOT NULL DEFAULT 0,
                    S_DRCR  CHAR(2)  NOT NULL CHECK (S_DRCR IN ('DR','CR')),
                    S_FLAG  CHAR(1)  NOT NULL DEFAULT 'T',
                    FOREIGN KEY (A_CODE) REFERENCES FAGroup(A_CODE)
                )""");
        jdbc.execute("""
                CREATE TABLE JournalMaster (
                    J_ID        CHAR(10) PRIMARY KEY,
                    J_DOC       CHAR(2)  NOT NULL DEFAULT 'JV',
                    J_DATE      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    J_AMOUNT    DECIMAL(18,3) NOT NULL DEFAULT 0,
                    J_NARR      CHAR(100),
                    REVERSED_BY CHAR(10) REFERENCES JournalMaster(J_ID),
                    REVERSES    CHAR(10) REFERENCES JournalMaster(J_ID)
                )""");
        jdbc.execute("""
                CREATE TABLE JournalDetail (
                    J_ID     CHAR(10),
                    J_CODE   CHAR(5),
                    J_DRCR   CHAR(2) NOT NULL,
                    J_AMOUNT DECIMAL(18,3) NOT NULL,
                    PRIMARY KEY (J_ID, J_CODE, J_DRCR),
                    FOREIGN KEY (J_ID)   REFERENCES JournalMaster(J_ID),
                    FOREIGN KEY (J_CODE) REFERENCES FASubGroup(S_CODE)
                )""");
    }

    /** A full, valid parent chain so each test breaks just ONE FK at a time. */
    private void seedValidParents(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE) VALUES ('01', 'Asset', '0')");
        jdbc.update("INSERT INTO FASubGroup (S_CODE, S_DESC, A_CODE, S_TYPE, S_DRCR) "
                + "VALUES ('10001', 'Cash', '01', '00', 'DR')");
        jdbc.update("INSERT INTO JournalMaster (J_ID, J_DOC, J_DATE, J_AMOUNT) "
                + "VALUES ('JV00000001', 'JV', '2024-01-15T10:00', 100)");
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    @DisplayName("[FK01] Pragma is reported as ON for the connection")
    void foreignKeysPragma_isOn() {
        JdbcTemplate jdbc = withForeignKeys(true);

        Integer state = jdbc.queryForObject("PRAGMA foreign_keys", Integer.class);

        assertThat(state).isEqualTo(1);
    }

    @Test
    @DisplayName("[FK02] Invalid JournalMaster reference (JournalDetail.J_ID) is rejected")
    void invalidJournalMasterReference_isRejected() {
        JdbcTemplate jdbc = withForeignKeys(true);

        // J_CODE is valid ('10001'); only J_ID points at a non-existent JournalMaster.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) "
                        + "VALUES ('JV99999999', '10001', 'DR', 100)"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");
    }

    @Test
    @DisplayName("[FK03] Invalid Account reference (JournalDetail.J_CODE) is rejected")
    void invalidAccountReference_isRejected() {
        JdbcTemplate jdbc = withForeignKeys(true);

        // J_ID is valid; only J_CODE points at a non-existent FASubGroup (ledger account).
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) "
                        + "VALUES ('JV00000001', '99999', 'DR', 100)"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");
    }

    @Test
    @DisplayName("[FK04] Invalid Account-group reference (FASubGroup.A_CODE) is rejected")
    void invalidAccountGroupReference_isRejected() {
        JdbcTemplate jdbc = withForeignKeys(true);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO FASubGroup (S_CODE, S_DESC, A_CODE, S_TYPE, S_DRCR) "
                        + "VALUES ('20002', 'Ghost', '99', '00', 'DR')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");
    }

    @Test
    @DisplayName("[FK05] Invalid Reversal reference (JournalMaster.REVERSES) is rejected")
    void invalidReversalReference_isRejected() {
        JdbcTemplate jdbc = withForeignKeys(true);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO JournalMaster (J_ID, J_DOC, J_DATE, J_AMOUNT, REVERSES) "
                        + "VALUES ('JV00000002', 'JV', '2024-01-16T10:00', 100, 'JVGHOST000')"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");
    }

    @Test
    @DisplayName("[FK06] Valid references are accepted when enforcement is ON")
    void validReferences_areAccepted() {
        JdbcTemplate jdbc = withForeignKeys(true);

        assertThatCode(() -> {
            jdbc.update("INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) "
                    + "VALUES ('JV00000001', '10001', 'DR', 100)");
            jdbc.update("INSERT INTO JournalMaster (J_ID, J_DOC, J_DATE, J_AMOUNT, REVERSES) "
                    + "VALUES ('JV00000002', 'JV', '2024-01-16T10:00', 100, 'JV00000001')");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("[FK07] CONTROL: with the pragma OFF, an invalid reference is silently accepted")
    void invalidReference_isAccepted_whenPragmaOff() {
        // Shows why the pragma matters: the same insert FK02 rejects goes through
        // fine when foreign keys aren't enforced, which is SQLite's default.
        JdbcTemplate jdbc = withForeignKeys(false);

        assertThatCode(() -> jdbc.update(
                "INSERT INTO JournalDetail (J_ID, J_CODE, J_DRCR, J_AMOUNT) "
                        + "VALUES ('JV99999999', '10001', 'DR', 100)"))
                .doesNotThrowAnyException();
    }
}
