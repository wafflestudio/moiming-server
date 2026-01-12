CREATE TABLE registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    event_id BIGINT NOT NULL,
    guest_name VARCHAR(100) NULL,
    guest_email VARCHAR(255) NULL,
    status ENUM('CONFIRMED', 'WAITING', 'CANCELED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_registrations_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_registrations_event
        FOREIGN KEY (event_id) REFERENCES events(id)
);
