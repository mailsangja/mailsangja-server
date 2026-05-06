CREATE UNIQUE INDEX IF NOT EXISTS uq_user_devices_fcm_token_active
    ON user_devices (fcm_token)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_threads_account_gmail_direction
    ON threads (mail_account_id, gmail_thread_id, direction);

CREATE UNIQUE INDEX IF NOT EXISTS uq_gmail_thread_locks_account_thread
    ON gmail_thread_locks (mail_account_id, gmail_thread_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_messages_thread_gmail
    ON messages (thread_id, gmail_message_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_contacts_user_email_active
    ON contacts (user_id, lower(email))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_labels_user_name_active
    ON labels (user_id, lower(name))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_webhook_id
    ON orders (webhook_id);
