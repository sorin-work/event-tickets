ALTER TABLE waitlist_entries ADD COLUMN leave_token VARCHAR(64);
CREATE UNIQUE INDEX uq_waitlist_leave_token ON waitlist_entries (leave_token);
