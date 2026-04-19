CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)     NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    username        VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    plan            VARCHAR(20)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    credit_usage    INT          NOT NULL,
    default_account CHAR(36)     NULL,
    created_at      TIMESTAMP    NOT NULL,
    modified_at     TIMESTAMP    NOT NULL,
    deleted_at      TIMESTAMP    NULL,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_devices (
    id          CHAR(36) NOT NULL,
    user_id     CHAR(36) NOT NULL,
    fcm_token   TEXT     NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    deleted_at  TIMESTAMP NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_devices_fcm_token_active
    ON user_devices (fcm_token)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS mail_accounts (
    id                      CHAR(36)     NOT NULL,
    user_id                 CHAR(36)     NOT NULL,
    provider                VARCHAR(50)  NOT NULL,
    email_address           VARCHAR(255) NOT NULL,
    alias                   VARCHAR(255) NOT NULL,
    icon                    VARCHAR(255) NOT NULL,
    color                   VARCHAR(7)   NOT NULL,
    access_token            TEXT         NOT NULL,
    access_token_expires_at TIMESTAMP    NOT NULL,
    refresh_token           TEXT         NULL,
    is_active               BOOLEAN      NOT NULL,
    sync_history_id         VARCHAR(255) NULL,
    watch_expires_at        TIMESTAMP    NULL,
    created_at              TIMESTAMP    NOT NULL,
    modified_at             TIMESTAMP    NOT NULL,
    deleted_at              TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_mail_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- direction: INBOUND(받은 편지함) / OUTBOUND(보낸 편지함)
-- 같은 gmail_thread_id라도 대화가 있으면 direction별로 최대 2개 row 존재
CREATE TABLE IF NOT EXISTS threads (
    id                         CHAR(36)     NOT NULL,
    mail_account_id            CHAR(36)     NOT NULL,
    gmail_thread_id            VARCHAR(255) NOT NULL,
    direction                  VARCHAR(20)  NOT NULL,
    history_id                 VARCHAR(255) NULL,
    latest_subject             VARCHAR(500) NULL,
    latest_snippet             VARCHAR(500) NULL,
    latest_participant_address VARCHAR(255) NULL,
    latest_participant_name    VARCHAR(255) NULL,
    last_message_at            TIMESTAMP    NULL,
    is_read                    BOOLEAN      NOT NULL DEFAULT FALSE,
    message_count              INT          NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP    NOT NULL,
    modified_at                TIMESTAMP    NOT NULL,
    deleted_at                 TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_threads_account_gmail_direction UNIQUE (mail_account_id, gmail_thread_id, direction),
    CONSTRAINT fk_threads_account FOREIGN KEY (mail_account_id) REFERENCES mail_accounts (id)
);

CREATE TABLE IF NOT EXISTS gmail_thread_locks (
    id              CHAR(36)     NOT NULL,
    mail_account_id CHAR(36)     NOT NULL,
    gmail_thread_id VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    modified_at     TIMESTAMP    NOT NULL,
    deleted_at      TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_gmail_thread_locks_account_thread UNIQUE (mail_account_id, gmail_thread_id),
    CONSTRAINT fk_gmail_thread_locks_account FOREIGN KEY (mail_account_id) REFERENCES mail_accounts (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id               CHAR(36)     NOT NULL,
    thread_id        CHAR(36)     NOT NULL,
    gmail_message_id VARCHAR(255) NOT NULL,
    direction        VARCHAR(20)  NOT NULL,
    subject          VARCHAR(500) NULL,
    from_address     VARCHAR(255) NOT NULL,
    from_name        VARCHAR(255) NULL,
    to_addresses     JSONB        NULL,
    to_names         JSONB        NULL,
    cc_addresses     JSONB        NULL,
    cc_names         JSONB        NULL,
    snippet          VARCHAR(500) NULL,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at          TIMESTAMP    NULL,
    body_text        TEXT         NULL,
    body_html        TEXT         NULL,
    created_at       TIMESTAMP    NOT NULL,
    modified_at      TIMESTAMP    NOT NULL,
    deleted_at       TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_messages_thread_gmail UNIQUE (thread_id, gmail_message_id),
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES threads (id)
);

CREATE TABLE IF NOT EXISTS attachments (
    id                  CHAR(36)     NOT NULL,
    message_id          CHAR(36)     NOT NULL,
    gmail_attachment_id VARCHAR(1024) NULL,
    filename            VARCHAR(255) NOT NULL,
    mime_type           VARCHAR(255) NOT NULL,
    size                INT          NULL,
    created_at          TIMESTAMP    NOT NULL,
    modified_at         TIMESTAMP    NOT NULL,
    deleted_at          TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_attachments_message FOREIGN KEY (message_id) REFERENCES messages (id)
);

CREATE TABLE IF NOT EXISTS labels (
    id                   CHAR(36)     NOT NULL,
    user_id              CHAR(36)     NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    color_code           VARCHAR(7)   NOT NULL,
    notification_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    rule                 JSONB        NULL,
    created_at           TIMESTAMP    NOT NULL,
    modified_at          TIMESTAMP    NOT NULL,
    deleted_at           TIMESTAMP    NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_labels_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS thread_labels (
    thread_id CHAR(36) NOT NULL,
    label_id  CHAR(36) NOT NULL,

    PRIMARY KEY (thread_id, label_id),
    CONSTRAINT fk_thread_labels_thread FOREIGN KEY (thread_id) REFERENCES threads (id),
    CONSTRAINT fk_thread_labels_label  FOREIGN KEY (label_id)  REFERENCES labels (id)
);

CREATE TABLE IF NOT EXISTS message_labels (
    message_id CHAR(36) NOT NULL,
    label_id   CHAR(36) NOT NULL,

    PRIMARY KEY (message_id, label_id),
    CONSTRAINT fk_message_labels_message FOREIGN KEY (message_id) REFERENCES messages (id),
    CONSTRAINT fk_message_labels_label   FOREIGN KEY (label_id)   REFERENCES labels (id)
);
