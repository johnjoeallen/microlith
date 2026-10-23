-- Demo-only seed data (demo profile).
INSERT INTO product (sku, name, price_cents) VALUES
    ('sku-widget', 'Widget', 1000),
    ('sku-gadget', 'Gadget', 2500),
    ('sku-oos', 'Out-of-stock thing', 700),
    ('sku-undeliverable', 'Undeliverable thing', 1200);
