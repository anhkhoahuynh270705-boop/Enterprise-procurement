BEGIN;

CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_required BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT chk_app_users_role CHECK (role IN ('ADMIN', 'USER', 'CHECKER'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_username ON app_users (username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_email ON app_users (email);
CREATE INDEX IF NOT EXISTS idx_app_users_enabled_deleted
    ON app_users (enabled, deleted);

CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(255),
    country VARCHAR(255),
    tax_code VARCHAR(255),
    bank_account VARCHAR(255),
    bank_name VARCHAR(255),
    notes TEXT,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT chk_suppliers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLACKLISTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_suppliers_code ON suppliers (code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_suppliers_tax_code
    ON suppliers (tax_code) WHERE tax_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers (status);
CREATE INDEX IF NOT EXISTS idx_suppliers_name_lower ON suppliers (LOWER(name));

CREATE TABLE IF NOT EXISTS procurement_tickets (
    id UUID PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    reason VARCHAR(1000),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    maker_username VARCHAR(100) NOT NULL,
    checker_username VARCHAR(100),
    checker_comment VARCHAR(1000),
    camunda_process_instance_id VARCHAR(100),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    approved_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT chk_procurement_ticket_status
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_procurement_ticket_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_procurement_ticket_total CHECK (total_amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_procurement_tickets_code
    ON procurement_tickets (ticket_code);
CREATE INDEX IF NOT EXISTS idx_procurement_tickets_status_created
    ON procurement_tickets (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_procurement_tickets_maker_created
    ON procurement_tickets (maker_username, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_procurement_tickets_department_priority
    ON procurement_tickets (department, priority);
CREATE INDEX IF NOT EXISTS idx_procurement_tickets_process_instance
    ON procurement_tickets (camunda_process_instance_id)
    WHERE camunda_process_instance_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS procurement_items (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit VARCHAR(50),
    unit_price NUMERIC(15, 2) NOT NULL,
    total_price NUMERIC(15, 2) NOT NULL,
    supplier_name VARCHAR(255),
    notes VARCHAR(500),
    CONSTRAINT fk_procurement_items_ticket
        FOREIGN KEY (ticket_id) REFERENCES procurement_tickets (id) ON DELETE CASCADE,
    CONSTRAINT chk_procurement_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_procurement_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_procurement_item_total_price CHECK (total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_procurement_items_ticket
    ON procurement_items (ticket_id);

CREATE TABLE IF NOT EXISTS supplier_proposals (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    tax_code VARCHAR(13) NOT NULL,
    contact_person VARCHAR(200),
    email VARCHAR(254),
    phone VARCHAR(16),
    address VARCHAR(255),
    city VARCHAR(255),
    country VARCHAR(255),
    bank_name VARCHAR(255),
    bank_account VARCHAR(255),
    notes TEXT,
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP(6) WITHOUT TIME ZONE,
    review_comment VARCHAR(2000),
    supplier_id UUID,
    pending_tax_code VARCHAR(13),
    CONSTRAINT chk_supplier_proposal_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_supplier_proposals_pending_tax_code
    ON supplier_proposals (pending_tax_code)
    WHERE pending_tax_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_supplier_proposal_owner
    ON supplier_proposals (submitted_by, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_supplier_proposal_status
    ON supplier_proposals (status, submitted_at DESC);

COMMIT;
