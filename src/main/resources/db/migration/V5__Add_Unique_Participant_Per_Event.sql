DELETE FROM participant participant_to_delete
USING participant participant_to_keep
WHERE participant_to_delete.event_id = participant_to_keep.event_id
  AND participant_to_delete.user_id = participant_to_keep.user_id
  AND participant_to_delete.id > participant_to_keep.id;

ALTER TABLE participant
    ADD CONSTRAINT participant_event_user_unique UNIQUE (event_id, user_id);
