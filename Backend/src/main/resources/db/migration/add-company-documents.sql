-- Optional PostgreSQL migration for deployments without Hibernate ddl-auto=update.
CREATE TABLE IF NOT EXISTS company_documents (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL,
    category varchar(30) NOT NULL,
    description varchar(2000) NOT NULL,
    filename varchar(200) NOT NULL,
    content_type varchar(255) NOT NULL,
    size bigint NOT NULL,
    uploaded_by varchar(255) NOT NULL,
    uploaded_at timestamp NOT NULL,
    deleted boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS company_document_contents (
    id uuid PRIMARY KEY REFERENCES company_documents(id),
    bytes bytea NOT NULL
);
