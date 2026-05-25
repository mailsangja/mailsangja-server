-- ── Unique Indexes ────────────────────────────────────────────────────────────

CREATE UNIQUE INDEX IF NOT EXISTS uq_mail_accounts_user_provider_email_active
    ON mail_accounts (user_id, provider, email_address)
    WHERE deleted_at IS NULL;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_devices_fcm_token_active
    ON user_devices (fcm_token)
    WHERE deleted_at IS NULL;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_threads_account_gmail_direction
    ON threads (mail_account_id, gmail_thread_id, direction);;

CREATE UNIQUE INDEX IF NOT EXISTS uq_gmail_thread_locks_account_thread
    ON gmail_thread_locks (mail_account_id, gmail_thread_id);;

CREATE UNIQUE INDEX IF NOT EXISTS uq_messages_thread_gmail
    ON messages (thread_id, gmail_message_id);;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contacts_user_email_active
    ON contacts (user_id, lower(email))
    WHERE deleted_at IS NULL;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_labels_user_name_active
    ON labels (user_id, lower(name))
    WHERE deleted_at IS NULL;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_webhook_id
    ON orders (webhook_id);;

CREATE UNIQUE INDEX IF NOT EXISTS uq_label_groups_user_name_active
    ON label_groups (user_id, lower(name))
    WHERE deleted_at IS NULL;;

-- ── Users ─────────────────────────────────────────────────────────────────────

-- 로그인 시 username 단건 조회
CREATE INDEX IF NOT EXISTS idx_users_username
    ON users (username)
    WHERE deleted_at IS NULL;;

-- ── Mail Accounts ─────────────────────────────────────────────────────────────

-- user_id 기반 계정 목록 (가장 빈번한 패턴)
CREATE INDEX IF NOT EXISTS idx_mail_accounts_user_id_active
    ON mail_accounts (user_id)
    WHERE deleted_at IS NULL;;

-- Pub/Sub webhook 수신 시 email_address로 계정 탐색
CREATE INDEX IF NOT EXISTS idx_mail_accounts_email_address_active
    ON mail_accounts (email_address)
    WHERE deleted_at IS NULL;;

-- user_id + active 조합 조회 (활성 계정 필터링)
CREATE INDEX IF NOT EXISTS idx_mail_accounts_user_id_active_flag
    ON mail_accounts (user_id, active)
    WHERE deleted_at IS NULL;;

-- Gmail watch 갱신 배치: provider + watch_expires_at 범위 스캔
CREATE INDEX IF NOT EXISTS idx_mail_accounts_renewal_target
    ON mail_accounts (provider, watch_expires_at)
    WHERE deleted_at IS NULL;;

-- ── Threads ───────────────────────────────────────────────────────────────────

-- 받은편지함/보낸편지함 목록 + 정렬 핵심 쿼리
-- (mail_account_id, direction) 필터 후 last_message_at DESC 정렬
CREATE INDEX IF NOT EXISTS idx_threads_mail_account_direction_last_msg
    ON threads (mail_account_id, direction, last_message_at DESC)
    WHERE deleted_at IS NULL;;

-- 읽지 않은 스레드 카운트 (mail_account_id + read 필터)
CREATE INDEX IF NOT EXISTS idx_threads_mail_account_read
    ON threads (mail_account_id, read)
    WHERE deleted_at IS NULL;;

-- ── Messages ──────────────────────────────────────────────────────────────────

-- thread_id 기반 메시지 목록 (핵심 FK 조회)
CREATE INDEX IF NOT EXISTS idx_messages_thread_id_active
    ON messages (thread_id)
    WHERE deleted_at IS NULL;;

-- ── Attachments ───────────────────────────────────────────────────────────────

-- message_id 기반 첨부파일 목록
CREATE INDEX IF NOT EXISTS idx_attachments_message_id_active
    ON attachments (message_id)
    WHERE deleted_at IS NULL;;

-- ── Message Labels ────────────────────────────────────────────────────────────

-- label_id 역방향 조회 (레이블별 메시지·스레드 필터링)
-- 복합 PK가 (message_id, label_id) 순서이므로 label_id 단독 인덱스 필요
CREATE INDEX IF NOT EXISTS idx_message_labels_label_id
    ON message_labels (label_id);;

-- ── Labels ────────────────────────────────────────────────────────────────────

-- user_id 기반 레이블 목록
CREATE INDEX IF NOT EXISTS idx_labels_user_id_active
    ON labels (user_id)
    WHERE deleted_at IS NULL;;

-- ── Label Groups ──────────────────────────────────────────────────────────────

-- user_id 기반 그룹 목록
CREATE INDEX IF NOT EXISTS idx_label_groups_user_id_active
    ON label_groups (user_id)
    WHERE deleted_at IS NULL;;

-- label_id 역방향 조회 (레이블이 속한 그룹 탐색)
-- 복합 PK가 (label_group_id, label_id) 순서이므로 label_id 단독 인덱스 필요
CREATE INDEX IF NOT EXISTS idx_label_group_labels_label_id
    ON label_group_labels (label_id);;

-- ── Contacts ──────────────────────────────────────────────────────────────────

-- user_id 기반 연락처 목록
CREATE INDEX IF NOT EXISTS idx_contacts_user_id_active
    ON contacts (user_id)
    WHERE deleted_at IS NULL;;

-- ── Reply Draft Suggestions ───────────────────────────────────────────────────

-- message_id 기반 AI 답장 초안 조회 및 삽입 잠금
CREATE INDEX IF NOT EXISTS idx_reply_draft_suggestions_message_id_active
    ON reply_draft_suggestions (message_id)
    WHERE deleted_at IS NULL;;

-- ── User Devices ──────────────────────────────────────────────────────────────

-- user_id 기반 FCM 디바이스 목록 (푸시 알림 발송)
CREATE INDEX IF NOT EXISTS idx_user_devices_user_id_active
    ON user_devices (user_id)
    WHERE deleted_at IS NULL;;

-- ── Orders ────────────────────────────────────────────────────────────────────

-- user_id 기반 주문 이력 조회
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders (user_id);;

-- ── Full-Text Search (Korean) ────────────────────────────────────────────────

-- messages.search_vector: subject + from_name + body_text + 수신/참조 이름
ALTER TABLE messages ADD COLUMN IF NOT EXISTS search_vector tsvector;;

CREATE OR REPLACE FUNCTION messages_search_vector_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.search_vector = to_tsvector('korean',
        coalesce(NEW.subject, '') || ' ' ||
        coalesce(NEW.from_name, '') || ' ' ||
        coalesce(NEW.body_text, '') || ' ' ||
        coalesce(array_to_string(
            ARRAY(SELECT jsonb_array_elements_text(coalesce(NEW.to_names, '[]'::jsonb))), ' '
        ), '') || ' ' ||
        coalesce(array_to_string(
            ARRAY(SELECT jsonb_array_elements_text(coalesce(NEW.cc_names, '[]'::jsonb))), ' '
        ), '')
    );
    RETURN NEW;
END;
$$;;

DROP TRIGGER IF EXISTS trg_messages_search_vector ON messages;;

CREATE TRIGGER trg_messages_search_vector
    BEFORE INSERT OR UPDATE ON messages
    FOR EACH ROW EXECUTE FUNCTION messages_search_vector_update();;

CREATE INDEX IF NOT EXISTS idx_messages_search_vector ON messages USING GIN(search_vector);;

-- attachments.filename_vector: 첨부파일명
ALTER TABLE attachments ADD COLUMN IF NOT EXISTS filename_vector tsvector;;

CREATE OR REPLACE FUNCTION attachments_filename_vector_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.filename_vector = to_tsvector('korean', coalesce(NEW.filename, ''));
    RETURN NEW;
END;
$$;;

DROP TRIGGER IF EXISTS trg_attachments_filename_vector ON attachments;;

CREATE TRIGGER trg_attachments_filename_vector
    BEFORE INSERT OR UPDATE ON attachments
    FOR EACH ROW EXECUTE FUNCTION attachments_filename_vector_update();;

CREATE INDEX IF NOT EXISTS idx_attachments_filename_vector ON attachments USING GIN(filename_vector);;

-- 검색 정렬 성능을 위한 threads.last_message_at 인덱스
CREATE INDEX IF NOT EXISTS idx_threads_last_message_at ON threads(last_message_at DESC);;

-- 기존 데이터 backfill (필요 시 DB에서 1회 수동 실행)
-- UPDATE messages
-- SET search_vector = to_tsvector('korean',
--     coalesce(subject, '') || ' ' || coalesce(from_name, '') || ' ' ||
--     coalesce(body_text, '') || ' ' ||
--     coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(coalesce(to_names, '[]'::jsonb))), ' '), '') || ' ' ||
--     coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(coalesce(cc_names, '[]'::jsonb))), ' '), ''))
-- WHERE search_vector IS NULL;
--
-- UPDATE attachments
-- SET filename_vector = to_tsvector('korean', coalesce(filename, ''))
-- WHERE filename_vector IS NULL;

-- ── Full-Text Search (English, pg_trgm) ──────────────────────────────────────
-- pg_trgm 확장은 extensions.sql(docker-entrypoint-initdb.d)에서 superuser로 설치됨

-- messages.search_text: 영어 부분/대소문자 무시 검색을 위한 소문자 정규화 텍스트
-- body_text는 인덱스 크기 제한을 위해 앞 5000자만 포함
ALTER TABLE messages ADD COLUMN IF NOT EXISTS search_text text;;

CREATE OR REPLACE FUNCTION messages_search_text_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.search_text = lower(
        coalesce(NEW.subject, '') || ' ' ||
        coalesce(NEW.from_name, '') || ' ' ||
        coalesce(array_to_string(
            ARRAY(SELECT jsonb_array_elements_text(coalesce(NEW.to_names, '[]'::jsonb))), ' '
        ), '') || ' ' ||
        coalesce(array_to_string(
            ARRAY(SELECT jsonb_array_elements_text(coalesce(NEW.cc_names, '[]'::jsonb))), ' '
        ), '') || ' ' ||
        left(coalesce(NEW.body_text, ''), 5000)
    );
    RETURN NEW;
END;
$$;;

DROP TRIGGER IF EXISTS trg_messages_search_text ON messages;;

CREATE TRIGGER trg_messages_search_text
    BEFORE INSERT OR UPDATE ON messages
    FOR EACH ROW EXECUTE FUNCTION messages_search_text_update();;

CREATE INDEX IF NOT EXISTS idx_messages_search_text_trgm ON messages USING GIN(search_text gin_trgm_ops);;

-- attachments.filename 트라이그램 인덱스 (대소문자 무시 부분 매칭)
CREATE INDEX IF NOT EXISTS idx_attachments_filename_trgm ON attachments USING GIN(lower(filename) gin_trgm_ops);;

-- 기존 데이터 backfill (필요 시 DB에서 1회 수동 실행)
-- UPDATE messages
-- SET search_text = lower(
--     coalesce(subject, '') || ' ' || coalesce(from_name, '') || ' ' ||
--     coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(coalesce(to_names, '[]'::jsonb))), ' '), '') || ' ' ||
--     coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(coalesce(cc_names, '[]'::jsonb))), ' '), '') || ' ' ||
--     left(coalesce(body_text, ''), 5000))
-- WHERE search_text IS NULL;
