CREATE TABLE rfqs (
    id UUID PRIMARY KEY,
    rfq_code VARCHAR(50) NOT NULL UNIQUE,
    procurement_ticket_id UUID NOT NULL REFERENCES procurement_tickets(id),
    title VARCHAR(255) NOT NULL,
    department VARCHAR(100),

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN (
            'DRAFT',
            'PENDING_RFQ_APPROVAL',
            'RETURNED',
            'SENT',
            'RECEIVING_QUOTES',
            'CLOSED_QUOTES',
            'COMPARING',
            'PENDING_RESULT_APPROVAL',
            'SELECTED_SUPPLIER',
            'PO_CREATED',
            'REJECTED',
            'CANCELED',
            'EXPIRED'
        )),

    deadline TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    quote_validity_days INTEGER NOT NULL CHECK (quote_validity_days > 0),
    delivery_location VARCHAR(500),
    delivery_window VARCHAR(255),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    estimated_value NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (estimated_value >= 0),

    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    submitted_at TIMESTAMP(6) WITHOUT TIME ZONE,

    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP(6) WITHOUT TIME ZONE,
    review_reason VARCHAR(2000),

    selected_supplier_id UUID REFERENCES suppliers(id),
    selection_reason VARCHAR(2000),
    exception_reason VARCHAR(2000),

    CONSTRAINT chk_rfq_not_self_review
        CHECK (reviewed_by IS NULL OR reviewed_by <> created_by)
);

CREATE INDEX idx_rfqs_status_created
    ON rfqs (status, created_at DESC);

CREATE INDEX idx_rfqs_creator_created
    ON rfqs (created_by, created_at DESC);

CREATE INDEX idx_rfqs_deadline
    ON rfqs (deadline);


-- Ảnh chụp hạng mục tại thời điểm tạo RFQ.
-- Giữ snapshot để thay đổi sau này ở phiếu mua sắm không làm đổi RFQ cũ.
CREATE TABLE rfq_items (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),
    procurement_item_id UUID REFERENCES procurement_items(id),

    sku VARCHAR(50),
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit VARCHAR(50),
    budget_unit_price NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (budget_unit_price >= 0),

    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_rfq_items_rfq
    ON rfq_items (rfq_id);


-- Danh sách nhà cung cấp được mời tham gia từng RFQ.
CREATE TABLE rfq_invited_suppliers (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),

    status VARCHAR(20) NOT NULL DEFAULT 'INVITED'
        CHECK (status IN (
            'INVITED',
            'RESPONDED',
            'DECLINED',
            'REVOKED'
        )),

    invited_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    responded_at TIMESTAMP(6) WITHOUT TIME ZONE,

    CONSTRAINT uq_rfq_invited_supplier
        UNIQUE (rfq_id, supplier_id)
);

CREATE INDEX idx_rfq_invited_supplier
    ON rfq_invited_suppliers (supplier_id, status);


-- Mỗi lần NCC gửi hoặc gửi lại báo giá tạo một vòng mới.
CREATE TABLE rfq_quote_rounds (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    round_number INTEGER NOT NULL CHECK (round_number > 0),

    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'WITHDRAWN')),

    submitted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    valid_until DATE,
    delivery_days INTEGER CHECK (delivery_days IS NULL OR delivery_days >= 0),
    payment_terms VARCHAR(1000),
    warranty VARCHAR(1000),
    technical_status VARCHAR(20)
        CHECK (technical_status IN ('PASS', 'PARTIAL', 'FAIL')),

    subtotal_before_tax NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (subtotal_before_tax >= 0),
    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (tax_amount >= 0),
    shipping_cost NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (shipping_cost >= 0),
    other_cost NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (other_cost >= 0),
    total_after_tax NUMERIC(18, 2) NOT NULL DEFAULT 0
        CHECK (total_after_tax >= 0),

    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_rfq_supplier_round
        UNIQUE (rfq_id, supplier_id, round_number)
);

CREATE INDEX idx_rfq_quote_rounds_rfq_supplier
    ON rfq_quote_rounds (rfq_id, supplier_id, round_number DESC);


-- Giá NCC chào cho từng hạng mục trong một vòng báo giá.
-- Không chào giá: is_quoted = FALSE, unit_price = NULL.
CREATE TABLE rfq_quote_items (
    id UUID PRIMARY KEY,
    quote_round_id UUID NOT NULL REFERENCES rfq_quote_rounds(id),
    rfq_item_id UUID NOT NULL REFERENCES rfq_items(id),

    is_quoted BOOLEAN NOT NULL DEFAULT TRUE,
    unit_price NUMERIC(18, 2)
        CHECK (unit_price IS NULL OR unit_price >= 0),
    note VARCHAR(1000),

    CONSTRAINT uq_quote_round_item
        UNIQUE (quote_round_id, rfq_item_id),

    CONSTRAINT chk_quote_item_price
        CHECK (
            (is_quoted = TRUE AND unit_price IS NOT NULL)
            OR
            (is_quoted = FALSE AND unit_price IS NULL)
        )
);

CREATE INDEX idx_rfq_quote_items_round
    ON rfq_quote_items (quote_round_id);


-- Trọng số tiêu chí cho RFQ.
CREATE TABLE rfq_score_criteria (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),

    criterion VARCHAR(30) NOT NULL
        CHECK (criterion IN (
            'PRICE',
            'TECHNICAL',
            'DELIVERY',
            'PAYMENT',
            'REPUTATION'
        )),

    weight_percent NUMERIC(5, 2) NOT NULL
        CHECK (weight_percent >= 0 AND weight_percent <= 100),

    CONSTRAINT uq_rfq_score_criterion
        UNIQUE (rfq_id, criterion)
);


-- Điểm NCC theo từng tiêu chí và từng vòng báo giá.
CREATE TABLE rfq_quote_scores (
    id UUID PRIMARY KEY,
    quote_round_id UUID NOT NULL REFERENCES rfq_quote_rounds(id),
    criterion VARCHAR(30) NOT NULL
        CHECK (criterion IN (
            'PRICE',
            'TECHNICAL',
            'DELIVERY',
            'PAYMENT',
            'REPUTATION'
        )),

    score NUMERIC(5, 2) NOT NULL
        CHECK (score >= 0 AND score <= 100),
    note VARCHAR(1000),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_quote_score_criterion
        UNIQUE (quote_round_id, criterion)
);


-- NCC thắng theo từng hạng mục, nếu RFQ chia lựa chọn theo dòng hàng.
CREATE TABLE rfq_item_awards (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),
    rfq_item_id UUID NOT NULL REFERENCES rfq_items(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    reason VARCHAR(2000),
    selected_by VARCHAR(100) NOT NULL,
    selected_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_rfq_item_award
        UNIQUE (rfq_id, rfq_item_id)
);


-- Lịch sử thao tác RFQ; dùng lưu log, không cập nhật/xóa bản ghi cũ.
CREATE TABLE rfq_audit_logs (
    id UUID PRIMARY KEY,
    rfq_id UUID NOT NULL REFERENCES rfqs(id),
    actor_username VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_rfq_audit_logs_rfq_created
    ON rfq_audit_logs (rfq_id, created_at DESC);