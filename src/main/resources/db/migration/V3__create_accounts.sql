CREATE TABLE accounts (
    id         UUID          PRIMARY KEY,
    ledger     VARCHAR(20)   NOT NULL,
    code       VARCHAR(100)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_accounts_ledger_code UNIQUE (ledger, code),
    CONSTRAINT ck_accounts_ledger CHECK (ledger IN ('CUSTOMER', 'INTERNAL'))
);

-- Every entry must point to an existing account
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_account
    FOREIGN KEY (account_id) REFERENCES accounts (id);

-- System accounts (see SystemAccount.java)
INSERT INTO accounts (id, ledger, code, created_at) VALUES
    (gen_random_uuid(), 'CUSTOMER', 'WORLD',             now()),
    (gen_random_uuid(), 'INTERNAL', 'WORLD',             now()),
    (gen_random_uuid(), 'INTERNAL', 'CUSTOMER_FUNDS',    now()),
    (gen_random_uuid(), 'INTERNAL', 'INSTANT_PAYMENTS',  now()),
    (gen_random_uuid(), 'INTERNAL', 'FINANCIAL_RESERVE', now());
