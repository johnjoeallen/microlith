CREATE TABLE payment_authorization (
    auth_id      VARCHAR(64)  PRIMARY KEY,
    request_id   VARCHAR(128) NOT NULL UNIQUE,
    customer_id  VARCHAR(64)  NOT NULL,
    amount_cents BIGINT       NOT NULL CHECK (amount_cents >= 0),
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
