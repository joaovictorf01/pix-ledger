CREATE TABLE ledger_entries (
    id             UUID         PRIMARY KEY,
    transaction_id UUID         NOT NULL,
    account_id     UUID         NOT NULL,
    ledger         VARCHAR(20)  NOT NULL,
    amount         BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_ledger_entries_amount_not_zero CHECK (amount <> 0),
    CONSTRAINT ck_ledger_entries_ledger CHECK (ledger IN ('CUSTOMER', 'INTERNAL'))
);

-- Account balance: SUM(amount) WHERE account_id = ?
CREATE INDEX ix_ledger_entries_account_id ON ledger_entries (account_id);

-- All entries of one transaction
CREATE INDEX ix_ledger_entries_transaction_id ON ledger_entries (transaction_id);
