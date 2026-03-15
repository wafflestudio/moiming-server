DELETE FROM events WHERE registration_ends_at IS NULL;
ALTER TABLE events MODIFY registration_ends_at DATETIME(6) NOT NULL;