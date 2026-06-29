ALTER TABLE participant
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SIGNED_UP';

ALTER TABLE participant
    ADD CONSTRAINT participant_status_check
        CHECK (status IN ('SIGNED_UP', 'WAITLISTED'));
