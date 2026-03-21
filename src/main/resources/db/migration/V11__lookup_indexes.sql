-- "Find my tickets" looks orders up by buyer email.
CREATE INDEX idx_orders_buyer_email ON orders (buyer_email, status);

-- Hold expiry scans pending orders by expiry time every 30 seconds.
CREATE INDEX idx_orders_status_hold_expires ON orders (status, hold_expires_at);

-- Admin waitlist page lists entries per event.
CREATE INDEX idx_waitlist_event ON waitlist_entries (event_id, created_at);

-- Counting check-ins per event at the door.
CREATE INDEX idx_tickets_event_checked_in ON tickets (event_id, checked_in_at);
