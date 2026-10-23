CREATE TABLE orders (
    order_id         VARCHAR(64)  PRIMARY KEY,
    request_id       VARCHAR(128) NOT NULL UNIQUE,
    customer_id      VARCHAR(64)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    lines            TEXT         NOT NULL,
    subtotal_cents   BIGINT       NOT NULL,
    discount_cents   BIGINT       NOT NULL,
    shipping_cents   BIGINT       NOT NULL,
    total_cents      BIGINT       NOT NULL,
    reservation_id   VARCHAR(64),
    authorization_id VARCHAR(64),
    shipment_id      VARCHAR(64),
    failure_code     VARCHAR(32),
    failure_reason   VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Module-local outbox: written in the same transaction as the order state change, drained by OutboxRelay.
CREATE TABLE outbox (
    id              VARCHAR(64)  PRIMARY KEY,
    order_id        VARCHAR(64)  NOT NULL,
    target          VARCHAR(64)  NOT NULL,
    request_id      VARCHAR(160) NOT NULL UNIQUE,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX outbox_due ON outbox (status, next_attempt_at);
