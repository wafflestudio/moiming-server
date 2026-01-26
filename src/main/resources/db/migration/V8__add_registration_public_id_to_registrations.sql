ALTER TABLE registrations
    ADD COLUMN registration_public_id VARCHAR(36) NULL;

UPDATE registrations
SET registration_public_id = UUID()
WHERE registration_public_id IS NULL;

ALTER TABLE registrations
    MODIFY COLUMN registration_public_id VARCHAR(36) NOT NULL;

ALTER TABLE registrations
    ADD CONSTRAINT uk_registrations_public_id UNIQUE (registration_public_id);
