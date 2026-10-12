CREATE TABLE pix_limits (
    account_id           UUID         PRIMARY KEY REFERENCES accounts (id),
    daily_limit          BIGINT       NOT NULL,
    pending_limit        BIGINT,
    pending_effective_at TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_pix_limits_daily_positive CHECK (daily_limit > 0),
    CONSTRAINT ck_pix_limits_pending_positive CHECK (pending_limit IS NULL OR pending_limit > 0),
    -- A pending increase always has the moment it applies, and vice versa
    CONSTRAINT ck_pix_limits_pending_complete
        CHECK ((pending_limit IS NULL) = (pending_effective_at IS NULL))
);

-- Outgoing amount of the day: SUM(amount) WHERE account_id = ? AND amount < 0 AND created_at >= ?
CREATE INDEX ix_ledger_entries_account_created ON ledger_entries (account_id, created_at);
