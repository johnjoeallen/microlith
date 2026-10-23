CREATE TABLE stock (
    sku       VARCHAR(64) PRIMARY KEY,
    available INTEGER     NOT NULL CHECK (available >= 0)
);

CREATE TABLE reservation (
    reservation_id VARCHAR(64)  PRIMARY KEY,
    request_id     VARCHAR(128) NOT NULL UNIQUE,
    order_id       VARCHAR(64)  NOT NULL,
    sku            VARCHAR(64)  NOT NULL,
    quantity       INTEGER      NOT NULL CHECK (quantity > 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
