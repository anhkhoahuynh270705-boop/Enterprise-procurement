-- Optional explicit PostgreSQL deployment migration. Local development uses ddl-auto=update.
CREATE TABLE IF NOT EXISTS supplier_documents (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    filename VARCHAR(200) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    review_comment VARCHAR(2000)
);
CREATE INDEX IF NOT EXISTS idx_supplier_document_supplier ON supplier_documents(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_document_owner ON supplier_documents(submitted_by);
CREATE TABLE IF NOT EXISTS supplier_document_contents (
    id UUID PRIMARY KEY REFERENCES supplier_documents(id),
    bytes BYTEA NOT NULL
);
