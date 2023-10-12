CREATE TABLE users
(
    id          BIGSERIAL NOT NULL,
    username    VARCHAR(35) NOT NULL UNIQUE,
    email       VARCHAR(60) NOT NULL,
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