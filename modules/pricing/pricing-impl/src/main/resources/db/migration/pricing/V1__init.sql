CREATE TABLE tier_discount (
    tier    VARCHAR(16) PRIMARY KEY,
    percent INTEGER     NOT NULL CHECK (percent BETWEEN 0 AND 100)
);

-- reference data owned by pricing
INSERT INTO tier_discount (tier, percent) VALUES ('STANDARD', 0), ('GOLD', 10);
