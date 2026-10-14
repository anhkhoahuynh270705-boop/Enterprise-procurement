-- Supplier contracts, contract counters, contents, and supplier document management

CREATE TABLE IF NOT EXISTS supplier_contracts (
    id UUID PRIMARY KEY,
    contract_number VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    signed_date DATE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED')),
    notes VARCHAR(5000),
    attachment_name VARCHAR(200),
    attachment_content_type VARCHAR(255),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT chk_supplier_contract_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_supplier_contracts_supplier_created
    ON supplier_contracts (supplier_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_supplier_contracts_status_end_date
    ON supplier_contracts (status, end_date);

CREATE TABLE IF NOT EXISTS supplier_contract_number_counters (
    contract_year INTEGER PRIMARY KEY,
    last_number BIGINT NOT NULL CHECK (last_number > 0)
);

CREATE TABLE IF NOT EXISTS supplier_contract_contents (
    id UUID PRIMARY KEY REFERENCES supplier_contracts(id) ON DELETE CASCADE,
    bytes BYTEA NOT NULL
);

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
    id UUID PRIMARY KEY REFERENCES supplier_documents(id) ON DELETE CASCADE,
    bytes BYTEA NOT NULL
);
