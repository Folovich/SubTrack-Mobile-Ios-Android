ALTER TABLE integrations
    ADD COLUMN IF NOT EXISTS external_account_email VARCHAR(320),
    ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_error_message TEXT;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS import_fingerprint VARCHAR(64);

ALTER TABLE import_items
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PARSE_ERROR',
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS message_received_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS source_provider VARCHAR(255),
    ADD COLUMN IF NOT EXISTS service_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS amount NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS billing_period VARCHAR(20),
    ADD COLUMN IF NOT EXISTS next_billing_date DATE,
    ADD COLUMN IF NOT EXISTS category_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_integrations_user_provider_updated_at
    ON integrations(user_id, provider, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_import_items_job_id
    ON import_items(job_id);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_import_fingerprint
    ON subscriptions(user_id, import_fingerprint);
