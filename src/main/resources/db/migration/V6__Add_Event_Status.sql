ALTER TABLE event
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';

ALTER TABLE event
    ADD CONSTRAINT event_status_check
        CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED'));
