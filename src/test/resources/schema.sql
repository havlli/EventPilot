CREATE TABLE guild
(
    id      VARCHAR(30) NOT NULL,
    name    VARCHAR(150) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE embed_type
(
    id      BIGSERIAL NOT NULL,
    name    VARCHAR(150) NOT NULL,
    structure TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE event
(
    id           VARCHAR(30) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    description  TEXT        NOT NULL,
    author       VARCHAR(30) NOT NULL,
    date_time    TIMESTAMP WITH TIME ZONE   NOT NULL,
    dest_channel VARCHAR(30) NOT NULL,
    member_size  VARCHAR(5)  NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reminder_sent BOOLEAN    NOT NULL DEFAULT FALSE,
    guild_id     VARCHAR(30) NOT NULL,
    embed_type   BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT event_status_check CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED', 'EXPIRED')),
    FOREIGN KEY (guild_id) REFERENCES guild(id) ON DELETE CASCADE,
    FOREIGN KEY (embed_type) REFERENCES embed_type(id) ON DELETE CASCADE
);

CREATE TABLE participant
(
    id           BIGSERIAL NOT NULL,
    user_id      VARCHAR(30) NOT NULL,
    username     VARCHAR(45) NOT NULL,
    position     INT         NOT NULL,
    role_index   INT         NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'SIGNED_UP',
    event_id     VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT participant_status_check CHECK (status IN ('SIGNED_UP', 'WAITLISTED')),
    CONSTRAINT participant_event_user_unique UNIQUE (event_id, user_id)
);

INSERT INTO embed_type (name, structure)
VALUES ('default', '{"-1":"Absence","-2":"Late","1":"Tank","-3":"Tentative","2":"Melee","3":"Ranged","4":"Healer","5":"Support"}');

CREATE TABLE users
(
    id          BIGSERIAL NOT NULL,
    username    VARCHAR(35) NOT NULL UNIQUE,
    email       VARCHAR(60) NOT NULL UNIQUE,
    password    VARCHAR(150) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE role
(
    id          BIGSERIAL NOT NULL,
    name        VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_role
(
    id          BIGSERIAL NOT NULL,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (role_id) REFERENCES role (id)
);

INSERT INTO role(name) VALUES('USER');
INSERT INTO role(name) VALUES('MODERATOR');
INSERT INTO role(name) VALUES('ADMIN');
