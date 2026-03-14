
-- account groups table
CREATE TABLE account_groups (
    group_code VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL
);


-- ledger accounts table
CREATE TABLE ledger_accounts (
    account_code VARCHAR(20) PRIMARY KEY,
    group_code VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    
    CONSTRAINT fk_group FOREIGN KEY (group_code) REFERENCES account_groups(group_code)
);

-- journal vouchers table
CREATE TABLE journal_vouchers (
    voucher_id SERIAL PRIMARY KEY,
    voucher_date DATE NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    narration TEXT
);

-- journal lines table
CREATE TABLE journal_lines (
    line_id SERIAL PRIMARY KEY,
    voucher_id INT NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    dr_cr CHAR(1) NOT NULL CHECK(dr_cr IN('D', 'C')),
    amount NUMERIC(15, 2) NOT NULL,

    CONSTRAINT fk_voucher FOREIGN KEY (voucher_id) REFERENCES journal_vouchers(voucher_id) ON DELETE CASCADE,
    CONSTRAINT fk_account FOREIGN KEY (account_code) REFERENCES ledger_accounts(account_code)
)