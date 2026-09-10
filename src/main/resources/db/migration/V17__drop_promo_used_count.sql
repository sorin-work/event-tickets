-- Uses are counted from orders now (paid ones and open checkouts).
ALTER TABLE promo_codes DROP COLUMN used_count;
