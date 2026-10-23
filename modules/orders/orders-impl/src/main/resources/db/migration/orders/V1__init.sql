CREATE TABLE orders (
    order_id    VARCHAR(64)  PRIMARY KEY,
    request_id  VARCHAR(128) NOT NULL UNIQUE,
    customer_id VARCHAR(64)  NOT NULL,
    sku         VARCHAR(64)  NOT NULL,
    quantity    INTEGER      NOT NULL CHECK (quantity > 0),
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
