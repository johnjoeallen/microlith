CREATE TABLE message (
    notification_id VARCHAR(64)  PRIMARY KEY,
    request_id      VARCHAR(128) NOT NULL UNIQUE,
    customer_id     VARCHAR(64)  NOT NULL,
    email           VARCHAR(200) NOT NULL,
    template        VARCHAR(100) NOT NULL,
    params          TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
