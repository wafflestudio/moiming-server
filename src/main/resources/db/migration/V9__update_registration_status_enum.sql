ALTER TABLE registrations
    MODIFY status ENUM(
    'HOST',
    'CONFIRMED',
    'WAITLISTED',
    'CANCELED',
    'BANNED'
    ) NOT NULL;
