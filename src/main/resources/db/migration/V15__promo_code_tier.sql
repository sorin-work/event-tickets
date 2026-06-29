-- Optional: the code only discounts tickets of this tier.
ALTER TABLE promo_codes ADD COLUMN tier_id BIGINT REFERENCES ticket_tiers (id) ON DELETE CASCADE;
