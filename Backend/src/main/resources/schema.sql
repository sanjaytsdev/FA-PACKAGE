-- FAGroup Table (Account Groups)
CREATE TABLE IF NOT EXISTS FAGroup (
    A_CODE CHAR(2) PRIMARY KEY,    -- Account group code (e.g., '01' for Asset)
    A_DESC CHAR(50) NOT NULL,       -- Account group description (e.g., 'Asset')
    A_TYPE CHAR(1) NOT NULL,        -- Type (e.g., '0' for Asset, '1' for Liability)
    A_CURRB DECIMAL(18,3) NOT NULL DEFAULT 0  -- Current balance aggregate (rarely used at runtime)
);

-- FASubGroup Table (Chart of Accounts)
CREATE TABLE IF NOT EXISTS FASubGroup (
    S_CODE CHAR(5) PRIMARY KEY,     -- Ledger account code (e.g., '10001' for a specific asset)
    S_DESC CHAR(50) NOT NULL,        -- Account description (e.g., 'Cash')
    A_CODE CHAR(2) NOT NULL,         -- Account group code (foreign key to FAGroup)
    S_TYPE CHAR(2) NOT NULL,         -- Numeric value (text) linking to FAGroup (e.g., '00' for asset)
    S_OPBAL DECIMAL(18,3) NOT NULL DEFAULT 0, -- Opening balance
    S_DRCR CHAR(2) NOT NULL,         -- Normal balance side: 'DR' or 'CR'
    S_FLAG CHAR(1) NOT NULL DEFAULT 'T',  -- Active flag: 'T' for active, 'F' for inactive
    FOREIGN KEY (A_CODE) REFERENCES FAGroup(A_CODE)  -- Link to FAGroup
);

-- JournalMaster Table (Journal Voucher Header)
CREATE TABLE IF NOT EXISTS JournalMaster (
    J_ID CHAR(10) PRIMARY KEY,      -- Journal voucher ID (e.g., 'JV123456789')
    J_DOC CHAR(2) NOT NULL DEFAULT 'JV',  -- Document type (always 'JV' for Journal Voucher)
    J_DATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Voucher date
    J_AMOUNT DECIMAL(18,3) NOT NULL DEFAULT 0,  -- Total voucher amount (calculated from lines)
    J_NARR CHAR(100)  -- Voucher narration/description (optional)
);

-- JournalDetail Table (Journal Voucher Line Items)
CREATE TABLE IF NOT EXISTS JournalDetail (
    J_ID CHAR(10),                 -- Journal voucher ID (foreign key to JournalMaster)
    J_CODE CHAR(5),                -- Account code (foreign key to FASubGroup)
    J_DRCR CHAR(2) NOT NULL,       -- Debit or Credit ('DR' or 'CR')
    J_AMOUNT DECIMAL(18,3) NOT NULL,  -- Amount for this line
    PRIMARY KEY (J_ID, J_CODE, J_DRCR),  -- Composite primary key
    FOREIGN KEY (J_ID) REFERENCES JournalMaster(J_ID),
    FOREIGN KEY (J_CODE) REFERENCES FASubGroup(S_CODE)
);