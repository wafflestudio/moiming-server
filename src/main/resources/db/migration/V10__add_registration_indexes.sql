CREATE INDEX idx_registrations_user_created_at
    ON registrations (user_id, created_at);

CREATE INDEX idx_registrations_event_status_created_at
    ON registrations (event_id, status, created_at, id);
