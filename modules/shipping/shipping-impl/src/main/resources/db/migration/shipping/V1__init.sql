CREATE TABLE shipment (
    shipment_id    VARCHAR(64)  PRIMARY KEY,
    request_id     VARCHAR(128) NOT NULL UNIQUE,
    order_id       VARCHAR(64)  NOT NULL,
    customer_id    VARCHAR(64)  NOT NULL,
    reservation_id VARCHAR(64)  NOT NULL,
    destination    VARCHAR(400) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE undeliverable_sku (
    sku VARCHAR(64) PRIMARY KEY
);
