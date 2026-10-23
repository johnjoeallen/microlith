-- Demo-only seed data (demo profile).
INSERT INTO customer (customer_id, name, email, tier, line1, city, country, payment_token, payment_brand) VALUES
    ('cust-standard', 'Anna Standard', 'anna@example.com', 'STANDARD', '1 Main Street', 'Dublin', 'IE', 'tok-ok', 'VISA'),
    ('cust-gold', 'Gordon Gold', 'gordon@example.com', 'GOLD', '2 High Street', 'Cork', 'IE', 'tok-ok', 'MASTERCARD'),
    ('cust-declined', 'Dana Declined', 'dana@example.com', 'STANDARD', '3 Low Street', 'Galway', 'IE', 'tok-declined', 'VISA');
