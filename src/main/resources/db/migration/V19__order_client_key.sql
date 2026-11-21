-- Keyed hash of the address that started the checkout, to cap the seats one visitor holds.
ALTER TABLE orders ADD COLUMN client_key VARCHAR(64);
CREATE INDEX idx_orders_event_client ON orders (event_id, client_key, status);
