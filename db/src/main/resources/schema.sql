CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)                            NOT NULL COMMENT 'PK',
    name            VARCHAR(50)                         NOT NULL COMMENT '성명',
    username        VARCHAR(255)                        NOT NULL COMMENT '로그인 id',
    password        VARCHAR(255)                        NOT NULL COMMENT '비밀번호, 암호화된',
    plan            ENUM ('FREE', 'PRO', 'ENTERPRISE')  NOT NULL COMMENT '구독 플랜',
    credit_usage    INT                                 NOT NULL COMMENT '크레딧 사용량',
    default_account CHAR(36)                            NULL     COMMENT '기본 메일 계정',
    created_at      TIMESTAMP                           NOT NULL COMMENT '생성 일시',
    modified_at     TIMESTAMP                           NOT NULL COMMENT '수정 일시',
    deleted_at      TIMESTAMP                           NULL     COMMENT '삭제 일시, 소프트 삭제',

    PRIMARY KEY (id)
);
