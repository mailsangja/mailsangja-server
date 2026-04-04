CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)     NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    username        VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    plan            VARCHAR(20)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    credit_usage    INT         NOT NULL,
    default_account CHAR(36)    NULL,
    created_at      TIMESTAMP   NOT NULL,
    modified_at     TIMESTAMP   NOT NULL,
    deleted_at      TIMESTAMP   NULL,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS mail_accounts (
    id                      CHAR(36)                NOT NULL,
    user_id                 CHAR(36)                NOT NULL,
    provider                VARCHAR(50)             NOT NULL,
    email_address           VARCHAR(255)            NOT NULL,
    alias                   VARCHAR(255)            NOT NULL,
    icon                    VARCHAR(255)            NOT NULL,
    color                   VARCHAR(7)              NOT NULL,
    access_token            TEXT                    NOT NULL,
    access_token_expires_at TIMESTAMP               NOT NULL,
    refresh_token           TEXT                    NULL,
    is_active               BOOLEAN                 NOT NULL,
    sync_history_id         VARCHAR(255)            NULL,
    created_at              TIMESTAMP               NOT NULL,
    modified_at             TIMESTAMP               NOT NULL,
    deleted_at              TIMESTAMP               NULL,

    PRIMARY KEY (id)
);
