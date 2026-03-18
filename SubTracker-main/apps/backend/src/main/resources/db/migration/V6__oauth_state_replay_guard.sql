CREATE TABLE IF NOT EXISTS oauth_state_usage (
    state_jti VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_oauth_state_usage_user_provider
    ON oauth_state_usage(user_id, provider);

CREATE INDEX IF NOT EXISTS idx_oauth_state_usage_expires_at
    ON oauth_state_usage(expires_at);
