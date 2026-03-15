DELETE r FROM registrations r INNER JOIN events e ON r.event_id = e.id WHERE e.registration_ends_at IS NULL;
DELETE FROM events WHERE registration_ends_at IS NULL;
ALTER TABLE events MODIFY registration_ends_at DATETIME(6) NOT NULL;
