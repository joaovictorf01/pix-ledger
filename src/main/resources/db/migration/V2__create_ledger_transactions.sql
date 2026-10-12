CREATE TABLE ledger_transactions (
    id              UUID          PRIMARY KEY,
    idempotency_key VARCHAR(100)  NOT NULL,
    ledger          VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_ledger_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_ledger_transactions_ledger CHECK (ledger IN ('CUSTOMER', 'INTERNAL'))
);

-- Every entry must belong to a recorded transaction
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_transaction
    FOREIGN KEY (transaction_id) REFERENCES ledger_transactions (id);
