INSERT INTO guild (id, name)
    VALUES ('1', 'testGuild');
INSERT INTO embed_type (id, name, structure)
    VALUES (1, 'default','{"-1":"Absence","-2":"Late","1":"Tank","-3":"Tentative","2":"Melee","3":"Ranged","4":"Healer","5":"Support"}');
INSERT INTO event (id, name, description, author, date_time, dest_channel, member_size, guild_id, embed_type)
    VALUES ('10','testEvent','description','123456789','2023-07-25 12:34:56','123456789','15','1', 1);
INSERT INTO participant (user_id, username, position, role_index, event_id)
    VALUES ('100','testUser',1,1,'10');