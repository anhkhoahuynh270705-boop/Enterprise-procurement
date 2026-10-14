-- Procurement SKU sequence and Purchase Orders system

CREATE SEQUENCE IF NOT EXISTS procurement_item_sku_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS purchase_orders (
    id UUID PRIMARY KEY,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    ticket_id UUID NOT NULL REFERENCES procurement_tickets(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    expected_delivery_date DATE,
    notes VARCHAR(1000),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    camunda_process_instance_id VARCHAR(64),
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP(6) WITHOUT TIME ZONE,
    review_comment VARCHAR(2000),
    CONSTRAINT chk_purchase_orders_status
        CHECK (status IN ('PENDING_APPROVAL', 'DRAFT', 'SENT', 'RECEIVED', 'CANCELLED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_ticket_created
    ON purchase_orders (ticket_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status_created
    ON purchase_orders (status, created_at DESC);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id UUID PRIMARY KEY,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    procurement_item_id UUID NOT NULL REFERENCES procurement_items(id),
    item_code VARCHAR(50),
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit VARCHAR(50),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    total_price NUMERIC(15, 2) NOT NULL CHECK (total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order
    ON purchase_order_items (purchase_order_id);
