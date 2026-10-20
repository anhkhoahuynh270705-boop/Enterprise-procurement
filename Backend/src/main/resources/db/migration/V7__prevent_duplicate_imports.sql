ALTER TABLE procurement_tickets
    ADD COLUMN IF NOT EXISTS import_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS ux_procurement_tickets_import_fingerprint
    ON procurement_tickets (import_fingerprint)
    WHERE import_fingerprint IS NOT NULL;
