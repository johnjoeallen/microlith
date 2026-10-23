CREATE TABLE product (
    sku         VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    price_cents BIGINT       NOT NULL CHECK (price_cents >= 0)
);
