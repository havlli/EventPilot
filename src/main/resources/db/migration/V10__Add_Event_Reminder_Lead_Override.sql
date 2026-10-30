ALTER TABLE event
    ADD COLUMN reminder_lead_minutes INTEGER,
    ADD CONSTRAINT event_reminder_lead_minutes_check
        CHECK (reminder_lead_minutes IS NULL OR reminder_lead_minutes BETWEEN 1 AND 10080);
