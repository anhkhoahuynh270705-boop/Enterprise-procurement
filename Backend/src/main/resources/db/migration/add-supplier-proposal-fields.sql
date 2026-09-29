-- Adds fields introduced when supplier proposals were aligned with supplier creation.
BEGIN;

ALTER TABLE supplier_proposals
    ADD COLUMN IF NOT EXISTS city VARCHAR(255),
    ADD COLUMN IF NOT EXISTS country VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bank_account VARCHAR(255),
    ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_supplier_proposals_pending_tax_code
    ON supplier_proposals (pending_tax_code)
    WHERE pending_tax_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_supplier_proposal_owner
    ON supplier_proposals (submitted_by, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_supplier_proposal_status
    ON supplier_proposals (status, submitted_at DESC);

COMMIT;
