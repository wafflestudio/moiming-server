CREATE TABLE events (
        id BIGINT NOT NULL AUTO_INCREMENT,

        public_id VARCHAR(36) NOT NULL,

        title VARCHAR(255) NOT NULL,
        description TEXT NULL,
        location VARCHAR(255) NULL,

        starts_at DATETIME(6) NULL,
        ends_at DATETIME(6) NULL,

        capacity INT NULL,
        waitlist_enabled BOOLEAN NOT NULL,

        registration_starts_at DATETIME(6) NULL,
        registration_ends_at DATETIME(6) NULL,

        created_by BIGINT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL,

        PRIMARY KEY (id),
        UNIQUE KEY uk_events_public_id (public_id),

        CONSTRAINT fk_events_user
            FOREIGN KEY (created_by) REFERENCES users(id)
);
