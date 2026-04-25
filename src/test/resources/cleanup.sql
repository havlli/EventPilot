TRUNCATE TABLE participant, event, guild, embed_type, user_role, users, role RESTART IDENTITY CASCADE;

INSERT INTO embed_type (name, structure)
VALUES ('default', '{"-1":"Absence","-2":"Late","1":"Tank","-3":"Tentative","2":"Melee","3":"Ranged","4":"Healer","5":"Support"}');

INSERT INTO role(name) VALUES('USER');
INSERT INTO role(name) VALUES('MODERATOR');
INSERT INTO role(name) VALUES('ADMIN');
