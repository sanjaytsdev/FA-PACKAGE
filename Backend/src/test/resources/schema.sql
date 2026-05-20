-- ============================================================
-- H2 Test Schema — mirrors production SQLite schema.sql
--
-- KEY DIFFERENCE FROM PRODUCTION:
--   J_DATE is VARCHAR(30) here (not DATETIME).
--   SQLite stores DATETIME as TEXT internally, and the application
--   persists LocalDateTime using toString() (ISO-8601).
--   Using VARCHAR in H2 exactly replicates that TEXT-storage
--   behaviour, so rs.getString("J_DATE") returns an ISO-8601
--   string that LocalDateTime.parse() can consume.
-- ============================================================

DROP TABLE IF EXISTS JournalDetail;
DROP TABLE IF EXISTS JournalMaster;
DROP TABLE IF EXISTS FASubGroup;
DROP TABLE IF EXISTS FAGroup;

-- FAGroup Table (Account Groups)
CREATE TABLE FAGroup (
    A_CODE  CHAR(2)        NOT NULL PRIMARY KEY,
    A_DESC  CHAR(50)       NOT NULL,
    A_TYPE  CHAR(1)        NOT NULL,
    A_CURRB DECIMAL(18, 3) NOT NULL DEFAULT 0
);

-- FASubGroup Table (Chart of Accounts)
CREATE TABLE FASubGroup (
    S_CODE  CHAR(5)        NOT NULL PRIMARY KEY,
    S_DESC  CHAR(50)       NOT NULL,
    A_CODE  CHAR(2)        NOT NULL,
    S_TYPE  CHAR(2)        NOT NULL,
    S_OPBAL DECIMAL(18, 3) NOT NULL DEFAULT 0,
    S_DRCR  CHAR(2)        NOT NULL,
    S_FLAG  CHAR(1)        NOT NULL DEFAULT 'T',
    FOREIGN KEY (A_CODE) REFERENCES FAGroup (A_CODE)
);

-- JournalMaster Table (Journal Voucher Header)
-- J_DATE is VARCHAR(30) — matches SQLite TEXT affinity for DATETIME
CREATE TABLE JournalMaster (
    J_ID     CHAR(10)       NOT NULL PRIMARY KEY,
    J_DOC    CHAR(2)        NOT NULL DEFAULT 'JV',
    J_DATE   VARCHAR(30)    NOT NULL,
    J_AMOUNT DECIMAL(18, 3) NOT NULL DEFAULT 0,
    J_NARR   CHAR(100)
);

-- JournalDetail Table (Journal Voucher Line Items)
CREATE TABLE JournalDetail (
    J_ID     CHAR(10)       NOT NULL,
    J_CODE   CHAR(5)        NOT NULL,
    J_DRCR   CHAR(2)        NOT NULL,
    J_AMOUNT DECIMAL(18, 3) NOT NULL,
    PRIMARY KEY (J_ID, J_CODE, J_DRCR),
    FOREIGN KEY (J_ID)   REFERENCES JournalMaster (J_ID),
    FOREIGN KEY (J_CODE) REFERENCES FASubGroup (S_CODE)
);
