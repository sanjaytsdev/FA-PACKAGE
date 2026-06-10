-- ============================================================================
-- MySQL schema (Docker / production)
--
-- WHY A SEPARATE FILE
--   schema.sql is for SQLite and uses syntax MySQL won't take:
--     * INTEGER PRIMARY KEY AUTOINCREMENT   -> MySQL needs BIGINT AUTO_INCREMENT
--     * CREATE INDEX IF NOT EXISTS          -> MySQL doesn't have it
--   so the MySQL profiles load this file instead (see application-docker.properties).
--
-- DATE COLUMNS ARE VARCHAR, NOT DATETIME
--   The app writes every timestamp with LocalDateTime.toString(), i.e. ISO-8601
--   with a 'T' separator (e.g. 2026-06-07T10:15:30). MySQL DATETIME only takes a
--   space-separated literal and rejects the 'T', so we keep the date columns as
--   VARCHAR to store exactly what the app writes. Same trick the H2 test schema
--   uses, and the Java RowMappers work unchanged on every engine.
--
-- Indexes go inline so that with CREATE TABLE IF NOT EXISTS, re-running this
-- script on an existing DB does nothing (MySQL has no CREATE INDEX IF NOT EXISTS).
-- ============================================================================

-- FAGroup table (account groups)
CREATE TABLE IF NOT EXISTS FAGroup (
    A_CODE  VARCHAR(2)     NOT NULL PRIMARY KEY,
    A_DESC  VARCHAR(50)    NOT NULL,
    A_TYPE  CHAR(1)        NOT NULL,
    A_CURRB DECIMAL(18, 3) NOT NULL DEFAULT 0
);

-- FASubGroup table (chart of accounts)
CREATE TABLE IF NOT EXISTS FASubGroup (
    S_CODE  VARCHAR(5)     NOT NULL PRIMARY KEY,
    S_DESC  VARCHAR(50)    NOT NULL,
    A_CODE  VARCHAR(2)     NOT NULL,
    S_TYPE  VARCHAR(2)     NOT NULL,
    S_OPBAL DECIMAL(18, 3) NOT NULL DEFAULT 0,
    S_DRCR  CHAR(2)        NOT NULL CHECK (S_DRCR IN ('DR', 'CR')),
    S_FLAG  CHAR(1)        NOT NULL DEFAULT 'T',
    CONSTRAINT fk_fasubgroup_group FOREIGN KEY (A_CODE) REFERENCES FAGroup (A_CODE)
);

-- JournalMaster table (voucher header)
-- J_DATE is VARCHAR(30): the app writes it as an ISO-8601 string (with the 'T').
CREATE TABLE IF NOT EXISTS JournalMaster (
    J_ID        VARCHAR(12)    NOT NULL PRIMARY KEY,
    J_DOC       CHAR(2)        NOT NULL DEFAULT 'JV',
    J_DATE      VARCHAR(30)    NOT NULL,
    J_AMOUNT    DECIMAL(18, 3) NOT NULL DEFAULT 0,
    J_NARR      VARCHAR(100)   NOT NULL,
    REVERSED_BY VARCHAR(12),
    REVERSES    VARCHAR(12),
    CONSTRAINT fk_jm_reversed_by FOREIGN KEY (REVERSED_BY) REFERENCES JournalMaster (J_ID),
    CONSTRAINT fk_jm_reverses    FOREIGN KEY (REVERSES)    REFERENCES JournalMaster (J_ID),
    INDEX idx_journalmaster_jdate (J_DATE)
);

-- JournalDetail table (voucher lines)
CREATE TABLE IF NOT EXISTS JournalDetail (
    J_ID     VARCHAR(12)    NOT NULL,
    J_CODE   VARCHAR(5)     NOT NULL,
    J_DRCR   CHAR(2)        NOT NULL,
    J_AMOUNT DECIMAL(18, 3) NOT NULL,
    PRIMARY KEY (J_ID, J_CODE, J_DRCR),
    CONSTRAINT fk_jd_master  FOREIGN KEY (J_ID)   REFERENCES JournalMaster (J_ID),
    CONSTRAINT fk_jd_account FOREIGN KEY (J_CODE) REFERENCES FASubGroup (S_CODE),
    INDEX idx_journaldetail_jcode (J_CODE)
);

-- JournalSequence table: per-year counter for generating J_IDs
CREATE TABLE IF NOT EXISTS JournalSequence (
    SEQ_YEAR INT NOT NULL PRIMARY KEY,
    LAST_VAL INT NOT NULL
);

-- AuditLog table: append-only record of every write that succeeded.
-- ID is BIGINT AUTO_INCREMENT on MySQL; EVENT_TIME stands in for the reserved word TIMESTAMP.
CREATE TABLE IF NOT EXISTS audit_log (
    ID          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ACTOR       VARCHAR(255)  NOT NULL,
    ACTION      VARCHAR(100)  NOT NULL,
    ENTITY_TYPE VARCHAR(100)  NOT NULL,
    ENTITY_ID   VARCHAR(100)  NOT NULL,
    EVENT_TIME  VARCHAR(30)   NOT NULL,
    DETAIL      VARCHAR(1000)
);

-- PeriodLock table: closed periods that reject new postings
CREATE TABLE IF NOT EXISTS period_lock (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    PERIOD_START VARCHAR(30)  NOT NULL,
    PERIOD_END   VARCHAR(30)  NOT NULL,
    LOCKED_AT    VARCHAR(30)  NOT NULL,
    LOCKED_BY    VARCHAR(255) NOT NULL
);
