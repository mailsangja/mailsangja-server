CREATE EXTENSION IF NOT EXISTS pg_trgm;;

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

ALTER TABLE labels ADD COLUMN IF NOT EXISTS is_sensitive BOOLEAN NOT NULL DEFAULT FALSE;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_webhook_id
    ON orders (webhook_id);;

CREATE UNIQUE INDEX IF NOT EXISTS uq_label_groups_user_name_active
    ON label_groups (user_id, lower(name))
    WHERE deleted_at IS NULL;;

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
-- pg_trgm 확장은 이 스크립트 상단에서 보장됨

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
