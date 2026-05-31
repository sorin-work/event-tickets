-- Free text for organisers, e.g. "paid cash at the door" or "refund sent".
ALTER TABLE orders ADD COLUMN admin_note VARCHAR(1000);
