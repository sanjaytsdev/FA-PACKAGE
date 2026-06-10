-- FAGroup table (account groups)
CREATE TABLE IF NOT EXISTS FAGroup (
    A_CODE VARCHAR(2) PRIMARY KEY,    -- group code, e.g. '01' for Asset
    A_DESC VARCHAR(50) NOT NULL,       -- group name, e.g. 'Asset'
    A_TYPE CHAR(1) NOT NULL,        -- type, e.g. '0' Asset, '1' Liability
    A_CURRB DECIMAL(18,3) NOT NULL DEFAULT 0  -- running balance total, hardly used at runtime
);

-- FASubGroup table (chart of accounts)
CREATE TABLE IF NOT EXISTS FASubGroup (
    S_CODE VARCHAR(5) PRIMARY KEY,     -- ledger account code, e.g. '10001'
    S_DESC VARCHAR(50) NOT NULL,        -- account name, e.g. 'Cash'
    A_CODE VARCHAR(2) NOT NULL,         -- group code (FK to FAGroup)
    S_TYPE VARCHAR(2) NOT NULL,         -- text number tying back to FAGroup, e.g. '00' for asset
    S_OPBAL DECIMAL(18,3) NOT NULL DEFAULT 0, -- opening balance
    S_DRCR CHAR(2) NOT NULL CHECK (S_DRCR IN ('DR', 'CR')),  -- normal side, 'DR' or 'CR'
    S_FLAG CHAR(1) NOT NULL DEFAULT 'T',  -- 'T' active, 'F' inactive
    FOREIGN KEY (A_CODE) REFERENCES FAGroup(A_CODE)  -- ties to FAGroup
);

-- JournalMaster table (voucher header)
CREATE TABLE IF NOT EXISTS JournalMaster (
    J_ID VARCHAR(12) PRIMARY KEY,   -- voucher id: 'JV' + 4-digit year + 6-digit sequence, e.g. 'JV2026000001'. VARCHAR(12) fits 999,999 vouchers a year and still accepts old 10-char ids
    J_DOC CHAR(2) NOT NULL DEFAULT 'JV',  -- doc type, always 'JV'
    J_DATE DATETIME NOT NULL, -- voucher date. App always writes an ISO-8601 string (with the 'T'). No DB default on purpose, so a row can't get a space-separated CURRENT_TIMESTAMP that JournalMasterRowMapper's strict LocalDateTime.parse would choke on
    J_AMOUNT DECIMAL(18,3) NOT NULL DEFAULT 0,  -- voucher total, summed from the lines
    J_NARR VARCHAR(100) NOT NULL,  -- narration, required; min 5 chars checked in the app layer
    REVERSED_BY VARCHAR(12) REFERENCES JournalMaster(J_ID),  -- id of the voucher that reversed this one, null until reversed
    REVERSES VARCHAR(12) REFERENCES JournalMaster(J_ID)      -- id this voucher reverses, set only on reversal vouchers
);

-- JournalDetail table (voucher lines)
CREATE TABLE IF NOT EXISTS JournalDetail (
    J_ID VARCHAR(12),              -- voucher id (FK to JournalMaster)
    J_CODE VARCHAR(5),             -- account code (FK to FASubGroup)
    J_DRCR CHAR(2) NOT NULL,       -- 'DR' or 'CR'
    J_AMOUNT DECIMAL(18,3) NOT NULL,  -- line amount
    PRIMARY KEY (J_ID, J_CODE, J_DRCR),  -- composite key
    FOREIGN KEY (J_ID) REFERENCES JournalMaster(J_ID),
    FOREIGN KEY (J_CODE) REFERENCES FASubGroup(S_CODE)
);

-- JournalSequence table: per-year counter for generating J_IDs.
-- Replaces the old MAX(J_ID)+1 scan, which could hand the same id to two
-- vouchers posted at once. One row per year. We bump the last sequence number
-- inside the posting transaction; the UPDATE holds a row lock until commit, so
-- concurrent posters line up instead of colliding.
CREATE TABLE IF NOT EXISTS JournalSequence (
    SEQ_YEAR INTEGER PRIMARY KEY,    -- year, e.g. 2026
    LAST_VAL INTEGER NOT NULL        -- last sequence number used that year
);

-- AuditLog table: append-only record of every write that succeeded.
-- Times are stored as ISO-8601 text, same as J_DATE.
CREATE TABLE IF NOT EXISTS audit_log (
    ID          INTEGER PRIMARY KEY AUTOINCREMENT,
    ACTOR       VARCHAR(255) NOT NULL,   -- who did it ('SYSTEM' until auth is added)
    ACTION      VARCHAR(100) NOT NULL,   -- POST, REVERSE, CREATE, DELETE, etc.
    ENTITY_TYPE VARCHAR(100) NOT NULL,   -- JournalVoucher, LedgerAccount, etc.
    ENTITY_ID   VARCHAR(100) NOT NULL,   -- key of the affected row
    EVENT_TIME  VARCHAR(30)  NOT NULL,   -- ISO-8601 timestamp. Called EVENT_TIME because TIMESTAMP is a reserved word in MySQL
    DETAIL      TEXT                     -- optional free text
);

-- PeriodLock table: closed periods that reject new postings.
CREATE TABLE IF NOT EXISTS period_lock (
    ID           INTEGER PRIMARY KEY AUTOINCREMENT,
    PERIOD_START VARCHAR(30)  NOT NULL,  -- start date, inclusive (ISO-8601)
    PERIOD_END   VARCHAR(30)  NOT NULL,  -- end date, inclusive (ISO-8601)
    LOCKED_AT    VARCHAR(30)  NOT NULL,  -- when the lock was made (ISO-8601)
    LOCKED_BY    VARCHAR(255) NOT NULL   -- who made it ('SYSTEM' until auth is added)
);

-- Indexes for the trial-balance rollup (sumPostingsAsOf).
-- Without them the "as of date" report falls back to full-table scans and gets
-- slow as the ledger grows. With them the date filter and per-account grouping
-- stay fast even across millions of lines.
--   * JournalMaster(J_DATE): lets "WHERE m.J_DATE < ?" range-scan instead of
--     reading every voucher header. The join d.J_ID = m.J_ID is already covered
--     by JournalDetail's composite key (J_ID is its first column), so we don't
--     need a separate J_ID index.
--   * JournalDetail(J_CODE): backs "GROUP BY d.J_CODE" and per-account lookups
--     (findByAccountCode) without reading every detail row.
CREATE INDEX IF NOT EXISTS idx_journalmaster_jdate ON JournalMaster(J_DATE);
CREATE INDEX IF NOT EXISTS idx_journaldetail_jcode ON JournalDetail(J_CODE);