-- Optional limit across all tiers, e.g. the fire-code capacity of the room.
ALTER TABLE events ADD COLUMN capacity INTEGER;
