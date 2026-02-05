ALTER TABLE events
    CHANGE COLUMN start_at starts_at DATETIME(6) NULL,
    CHANGE COLUMN end_at ends_at DATETIME(6) NULL,
    CHANGE COLUMN registration_deadline registration_ends_at DATETIME(6) NULL;

ALTER TABLE events
    ADD COLUMN registration_starts_at DATETIME(6) NULL
    AFTER waitlist_enabled;
