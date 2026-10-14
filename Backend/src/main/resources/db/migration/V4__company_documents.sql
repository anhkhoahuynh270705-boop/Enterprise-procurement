-- Company policies and documents

CREATE TABLE IF NOT EXISTS company_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    filename VARCHAR(200) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS company_document_contents (
    id UUID PRIMARY KEY REFERENCES company_documents(id) ON DELETE CASCADE,
    bytes BYTEA NOT NULL
);
