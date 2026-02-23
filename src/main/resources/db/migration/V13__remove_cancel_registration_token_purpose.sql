-- Remove cancel tokens before dropping enum value
DELETE FROM registration_tokens
WHERE purpose = 'CANCEL';

-- Drop CANCEL from enum
ALTER TABLE registration_tokens
    MODIFY purpose ENUM(
    'CHANGE_VOTE'
    ) NOT NULL;
