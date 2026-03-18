CREATE TABLE IF NOT EXISTS support_email_events (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    event VARCHAR(30) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_support_email_events_subscription_id
    ON support_email_events (subscription_id);

CREATE INDEX IF NOT EXISTS idx_support_email_events_user_created_at
    ON support_email_events (user_id, created_at DESC);
