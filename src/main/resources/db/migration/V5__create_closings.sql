-- One row per business day: proof of what Customer Funds held at the closing
CREATE TABLE closings (
    business_day         DATE         PRIMARY KEY,
    closed_at            TIMESTAMPTZ  NOT NULL,
    customer_total       BIGINT       NOT NULL,
    customer_funds       BIGINT       NOT NULL,
    customer_funds_match BOOLEAN      NOT NULL
);
