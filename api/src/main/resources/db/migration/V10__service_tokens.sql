CREATE TABLE IF NOT EXISTS service_tokens (
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    service       VARCHAR(50) NOT NULL,
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    label         VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_used_at  TIMESTAMP,
    revoked_at    TIMESTAMP
);

CREATE INDEX idx_service_tokens_token_hash ON service_tokens(token_hash);
CREATE INDEX idx_service_tokens_user_id ON service_tokens(user_id);
