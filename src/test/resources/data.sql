INSERT INTO guild (id, name)
    VALUES ('1', 'testguild');
INSERT INTO event (id, name, description, author, date_time, dest_channel, member_size, guild_id)
    VALUES ('10','testEvent','description','123456789','2023-07-25 12:34:56','123456789','15','1');
INSERT INTO participant (user_id, username, position, role_index, event_id)
    VALUES ('100','testUser',1,1,'10');