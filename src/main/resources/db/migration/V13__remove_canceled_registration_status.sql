-- Remove canceled registrations before dropping enum value
DELETE FROM registrations
WHERE status = 'CANCELED';

-- Drop CANCELED from enum
ALTER TABLE registrations
    MODIFY status ENUM(
    'HOST',
    'CONFIRMED',
    'WAITLISTED',
    'BANNED'
    ) NOT NULL;
