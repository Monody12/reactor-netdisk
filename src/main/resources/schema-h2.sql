-- H2 compatible schema (runs on every startup with mode=always)
DROP TABLE IF EXISTS file_token;
DROP TABLE IF EXISTS user_token;
DROP TABLE IF EXISTS file;
DROP TABLE IF EXISTS folder;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(20)                        NOT NULL,
    password    VARCHAR(20)                              NULL,
    email       VARCHAR(50)                              NULL,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    delete_flag BOOLEAN   DEFAULT FALSE                  NULL,
    CONSTRAINT users_username UNIQUE (username)
);

CREATE TABLE user_token
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT                                NOT NULL,
    token       VARCHAR(50)                        NOT NULL,
    expire_at   TIMESTAMP                          NOT NULL,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP  NULL,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP  NULL,
    delete_flag BOOLEAN   DEFAULT FALSE                  NULL
);

CREATE TABLE folder
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT                                 NOT NULL,
    parent_id   BIGINT                                  NULL,
    description VARCHAR(500)                            NULL,
    public_flag BOOLEAN   DEFAULT FALSE                  NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    delete_flag BOOLEAN   DEFAULT FALSE
);

CREATE TABLE file
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT                                 NOT NULL,
    folder_id   BIGINT                                  NULL,
    name        VARCHAR(255) NOT NULL,
    public_flag BOOLEAN   DEFAULT FALSE,
    description VARCHAR(500)                            NULL,
    path_name   VARCHAR(500) NOT NULL,
    size        BIGINT   DEFAULT 0,
    mime_type   VARCHAR(100)                            NULL,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    delete_flag BOOLEAN   DEFAULT FALSE
);

CREATE TABLE file_token
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id     BIGINT                                  NULL,
    token       VARCHAR(50)                          NOT NULL,
    expire_at   TIMESTAMP                            NOT NULL,
    created_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL,
    updated_at  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP     NULL
);
