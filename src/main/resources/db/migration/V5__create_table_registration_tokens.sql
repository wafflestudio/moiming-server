CREATE TABLE registration_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    purpose ENUM('CANCEL', 'CHANGE_VOTE') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_registration_tokens_registration
        FOREIGN KEY (registration_id) REFERENCES registrations(id)
);
