CREATE TABLE rfq_invitation_tokens (
    id UUID PRIMARY KEY,
    invited_supplier_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    create_by VARCHAR(100) NOT NULL
);

CREATE INDEX idx_rfq_invitation_tokens_invited_supplier
    ON rfq_invitation_tokens(invited_supplier_id);