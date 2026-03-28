CREATE TYPE plan_type AS ENUM ('FREE', 'PRO', 'ENTERPRISE');

CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)    NOT NULL,
    name            VARCHAR(50) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    plan            plan_type   NOT NULL,
    credit_usage    INT         NOT NULL,
    default_account CHAR(36)    NULL,
    created_at      TIMESTAMP   NOT NULL,
    modified_at     TIMESTAMP   NOT NULL,
    deleted_at      TIMESTAMP   NULL,

    PRIMARY KEY (id)
);
