-- ============================================================
-- H2 test schema, mirrors the production SQLite schema.sql
--
-- ONE DIFFERENCE FROM PRODUCTION:
--   J_DATE is VARCHAR(30) here, not DATETIME.
--   SQLite keeps DATETIME as TEXT under the hood, and the app
--   writes LocalDateTime via toString() (ISO-8601). VARCHAR in
--   H2 reproduces that TEXT storage, so rs.getString("J_DATE")
--   gives back an ISO-8601 string that LocalDateTime.parse()
--   can read. Same VARCHAR trick for every date column
--   (audit_log.EVENT_TIME, period_lock dates).
-- ============================================================

DROP TABLE IF EXISTS period_lock;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS JournalSequence;
DROP TABLE IF EXISTS JournalDetail;
DROP TABLE IF EXISTS JournalMaster;
DROP TABLE IF EXISTS FASubGroup;
DROP TABLE IF EXISTS FAGroup;

-- FAGroup table (account groups)
CREATE TABLE FAGroup (
    A_CODE  VARCHAR(2)     NOT NULL PRIMARY KEY,
    A_DESC  VARCHAR(50)    NOT NULL,
    A_TYPE  CHAR(1)        NOT NULL,
    A_CURRB DECIMAL(18, 3) NOT NULL DEFAULT 0
);

-- FASubGroup table (chart of accounts)
CREATE TABLE FASubGroup (
    S_CODE  VARCHAR(5)     NOT NULL PRIMARY KEY,
    S_DESC  VARCHAR(50)    NOT NULL,
    A_CODE  VARCHAR(2)     NOT NULL,
    S_TYPE  VARCHAR(2)     NOT NULL,
    S_OPBAL DECIMAL(18, 3) NOT NULL DEFAULT 0,
    S_DRCR  CHAR(2)        NOT NULL CHECK (S_DRCR IN ('DR', 'CR')),
    S_FLAG  CHAR(1)        NOT NULL DEFAULT 'T',
    FOREIGN KEY (A_CODE) REFERENCES FAGroup (A_CODE)
);

-- JournalMaster table (voucher header)
-- J_DATE is VARCHAR(30) to match SQLite's TEXT affinity for DATETIME
CREATE TABLE JournalMaster (
    J_ID        VARCHAR(12)    NOT NULL PRIMARY KEY,
    J_DOC       CHAR(2)        NOT NULL DEFAULT 'JV',
    J_DATE      VARCHAR(30)    NOT NULL,
    J_AMOUNT    DECIMAL(18, 3) NOT NULL DEFAULT 0,
    J_NARR      VARCHAR(100),
    REVERSED_BY VARCHAR(12)    REFERENCES JournalMaster (J_ID),
    REVERSES    VARCHAR(12)    REFERENCES JournalMaster (J_ID)
);

-- JournalDetail table (voucher lines)
CREATE TABLE JournalDetail (
    J_ID     VARCHAR(12)    NOT NULL,
    J_CODE   VARCHAR(5)     NOT NULL,
    J_DRCR   CHAR(2)        NOT NULL,
    J_AMOUNT DECIMAL(18, 3) NOT NULL,
    PRIMARY KEY (J_ID, J_CODE, J_DRCR),
    FOREIGN KEY (J_ID)   REFERENCES JournalMaster (J_ID),
    FOREIGN KEY (J_CODE) REFERENCES FASubGroup (S_CODE)
);

-- JournalSequence table: per-year counter for generating J_IDs
CREATE TABLE JournalSequence (
    SEQ_YEAR INTEGER NOT NULL PRIMARY KEY,
    LAST_VAL INTEGER NOT NULL
);

-- AuditLog table: append-only record of every write that succeeded
CREATE TABLE audit_log (
    ID          INT AUTO_INCREMENT PRIMARY KEY,
    ACTOR       VARCHAR(255) NOT NULL,
    ACTION      VARCHAR(100) NOT NULL,
    ENTITY_TYPE VARCHAR(100) NOT NULL,
    ENTITY_ID   VARCHAR(100) NOT NULL,
    EVENT_TIME  VARCHAR(30)  NOT NULL,
    DETAIL      VARCHAR(1000)
);

-- PeriodLock table: closed periods that reject new postings
CREATE TABLE period_lock (
    ID           INT AUTO_INCREMENT PRIMARY KEY,
    PERIOD_START VARCHAR(30)  NOT NULL,
    PERIOD_END   VARCHAR(30)  NOT NULL,
    LOCKED_AT    VARCHAR(30)  NOT NULL,
    LOCKED_BY    VARCHAR(255) NOT NULL
);

-- Indexes for the trial-balance rollup (sumPostingsAsOf).
-- Same as the production schema so tests hit the same access paths.
CREATE INDEX IF NOT EXISTS idx_journalmaster_jdate ON JournalMaster(J_DATE);
CREATE INDEX IF NOT EXISTS idx_journaldetail_jcode ON JournalDetail(J_CODE);
