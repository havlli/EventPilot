CREATE TABLE guild
(
    id      VARCHAR(30) NOT NULL,
    name     VARCHAR(150) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE event
(
    id           VARCHAR(30) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    description  TEXT        NOT NULL,
    author       VARCHAR(30) NOT NULL,
    date_time    TIMESTAMP   NOT NULL,
    dest_channel VARCHAR(30) NOT NULL,
    member_size  VARCHAR(5)  NOT NULL,
    guild_id     VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (guild_id) REFERENCES guild(id) ON DELETE CASCADE
);

CREATE TABLE participant
(
    id           BIGSERIAL NOT NULL,
    user_id      VARCHAR(30) NOT NULL,
    username     VARCHAR(45) NOT NULL,
    position     INT         NOT NULL,
    role_index   INT         NOT NULL,
    event_id     VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);