CREATE TABLE customer (
    customer_id   VARCHAR(64)  PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(200) NOT NULL,
    tier          VARCHAR(16)  NOT NULL,
    line1         VARCHAR(200) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    country       VARCHAR(2)   NOT NULL,
    payment_token VARCHAR(100) NOT NULL,
    payment_brand VARCHAR(30)  NOT NULL
);
