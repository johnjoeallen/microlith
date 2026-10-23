CREATE TABLE stock (
    sku       VARCHAR(64) PRIMARY KEY,
    available INTEGER     NOT NULL CHECK (available >= 0),
    warehouse VARCHAR(32) NOT NULL DEFAULT 'wh-1'
);

CREATE TABLE reservation (
    reservation_id VARCHAR(64)  PRIMARY KEY,
    request_id     VARCHAR(128) NOT NULL UNIQUE,
    order_id       VARCHAR(64)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE reservation_line (
    reservation_id VARCHAR(64) NOT NULL REFERENCES reservation (reservation_id),
    sku            VARCHAR(64) NOT NULL,
    quantity       INTEGER     NOT NULL CHECK (quantity > 0)
);
