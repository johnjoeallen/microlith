-- Demo-only seed data (demo profile).
INSERT INTO stock (sku, available, warehouse) VALUES
    ('sku-widget', 100, 'wh-1'),
    ('sku-gadget', 50, 'wh-1'),
    ('sku-oos', 0, 'wh-1'),
    ('sku-undeliverable', 100, 'wh-1');
