-- V15: POS Module - Core Transactions
-- Creates tables for POS terminals, shifts, transactions, lines, and payments

-- =============================================================================
-- POS TERMINALS
-- =============================================================================
CREATE TABLE pos_terminals (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    terminal_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    is_active BOOLEAN DEFAULT true,
    ip_address VARCHAR(50),
    mac_address VARCHAR(50),
    device_type VARCHAR(50),
    printer_name VARCHAR(100),
    cash_drawer_enabled BOOLEAN DEFAULT true,
    allow_offline BOOLEAN DEFAULT false,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    current_shift_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pos_terminals_tenant ON pos_terminals(tenant_id);
CREATE INDEX idx_pos_terminals_code ON pos_terminals(terminal_code);
CREATE INDEX idx_pos_terminals_location ON pos_terminals(location_id);
CREATE UNIQUE INDEX idx_pos_terminals_code_tenant ON pos_terminals(terminal_code, tenant_id);

-- =============================================================================
-- POS SHIFTS
-- =============================================================================
CREATE TABLE pos_shifts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    shift_number VARCHAR(50) NOT NULL,
    terminal_id BIGINT NOT NULL REFERENCES pos_terminals(id),
    cashier_id BIGINT NOT NULL,
    cashier_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    opening_cash NUMERIC(18, 4) NOT NULL DEFAULT 0,
    closing_cash NUMERIC(18, 4),
    expected_cash NUMERIC(18, 4),
    cash_difference NUMERIC(18, 4),
    total_sales NUMERIC(18, 4) DEFAULT 0,
    total_returns NUMERIC(18, 4) DEFAULT 0,
    total_discounts NUMERIC(18, 4) DEFAULT 0,
    total_taxes NUMERIC(18, 4) DEFAULT 0,
    cash_payments NUMERIC(18, 4) DEFAULT 0,
    card_payments NUMERIC(18, 4) DEFAULT 0,
    other_payments NUMERIC(18, 4) DEFAULT 0,
    transaction_count INTEGER DEFAULT 0,
    voided_count INTEGER DEFAULT 0,
    return_count INTEGER DEFAULT 0,
    cash_in NUMERIC(18, 4) DEFAULT 0,
    cash_out NUMERIC(18, 4) DEFAULT 0,
    notes VARCHAR(1000),
    closing_notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pos_shifts_tenant ON pos_shifts(tenant_id);
CREATE INDEX idx_pos_shifts_terminal ON pos_shifts(terminal_id);
CREATE INDEX idx_pos_shifts_cashier ON pos_shifts(cashier_id);
CREATE INDEX idx_pos_shifts_status ON pos_shifts(status);
CREATE INDEX idx_pos_shifts_opened_at ON pos_shifts(opened_at);

-- Add foreign key for current_shift_id in pos_terminals
ALTER TABLE pos_terminals ADD CONSTRAINT fk_pos_terminals_current_shift
    FOREIGN KEY (current_shift_id) REFERENCES pos_shifts(id);

-- =============================================================================
-- POS TRANSACTIONS
-- =============================================================================
CREATE TABLE pos_transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    transaction_number VARCHAR(50) NOT NULL,
    terminal_id BIGINT NOT NULL REFERENCES pos_terminals(id),
    shift_id BIGINT NOT NULL REFERENCES pos_shifts(id),
    customer_id BIGINT REFERENCES customers(id),
    customer_name VARCHAR(200),
    customer_phone VARCHAR(50),
    transaction_type VARCHAR(20) NOT NULL DEFAULT 'SALE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal NUMERIC(18, 4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(18, 4) DEFAULT 0,
    discount_percent NUMERIC(5, 2),
    discount_reason VARCHAR(200),
    tax_amount NUMERIC(18, 4) DEFAULT 0,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(18, 4) DEFAULT 0,
    change_amount NUMERIC(18, 4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    item_count INTEGER DEFAULT 0,
    line_count INTEGER DEFAULT 0,
    held_at TIMESTAMP WITH TIME ZONE,
    held_by BIGINT,
    held_reason VARCHAR(200),
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by BIGINT,
    voided_at TIMESTAMP WITH TIME ZONE,
    voided_by BIGINT,
    void_reason VARCHAR(500),
    original_transaction_id BIGINT,
    original_transaction_number VARCHAR(50),
    ar_invoice_id BIGINT,
    gl_journal_entry_id BIGINT,
    gl_posted BOOLEAN DEFAULT false,
    stock_deducted BOOLEAN DEFAULT false,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pos_transactions_tenant ON pos_transactions(tenant_id);
CREATE INDEX idx_pos_transactions_number ON pos_transactions(transaction_number);
CREATE INDEX idx_pos_transactions_terminal ON pos_transactions(terminal_id);
CREATE INDEX idx_pos_transactions_shift ON pos_transactions(shift_id);
CREATE INDEX idx_pos_transactions_customer ON pos_transactions(customer_id);
CREATE INDEX idx_pos_transactions_status ON pos_transactions(status);
CREATE INDEX idx_pos_transactions_type ON pos_transactions(transaction_type);
CREATE INDEX idx_pos_transactions_completed_at ON pos_transactions(completed_at);
CREATE UNIQUE INDEX idx_pos_transactions_number_tenant ON pos_transactions(transaction_number, tenant_id);

-- =============================================================================
-- POS TRANSACTION LINES
-- =============================================================================
CREATE TABLE pos_transaction_lines (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES pos_transactions(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id),
    variant_id BIGINT REFERENCES product_variants(id),
    product_code VARCHAR(50),
    product_name VARCHAR(200) NOT NULL,
    variant_name VARCHAR(200),
    barcode VARCHAR(100),
    quantity NUMERIC(18, 4) NOT NULL DEFAULT 1,
    unit_price NUMERIC(18, 4) NOT NULL,
    original_price NUMERIC(18, 4),
    cost_price NUMERIC(18, 4),
    discount_amount NUMERIC(18, 4) DEFAULT 0,
    discount_percent NUMERIC(5, 2),
    discount_reason VARCHAR(200),
    tax_code VARCHAR(20),
    tax_rate NUMERIC(5, 2),
    tax_amount NUMERIC(18, 4) DEFAULT 0,
    line_total NUMERIC(18, 4) NOT NULL DEFAULT 0,
    is_return BOOLEAN DEFAULT false,
    original_line_id BIGINT,
    serial_number VARCHAR(100),
    batch_number VARCHAR(100),
    location_id BIGINT,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pos_tx_lines_transaction ON pos_transaction_lines(transaction_id);
CREATE INDEX idx_pos_tx_lines_product ON pos_transaction_lines(product_id);

-- =============================================================================
-- POS PAYMENTS
-- =============================================================================
CREATE TABLE pos_payments (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES pos_transactions(id) ON DELETE CASCADE,
    payment_number INTEGER NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(18, 4) NOT NULL,
    tendered_amount NUMERIC(18, 4),
    change_amount NUMERIC(18, 4) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'UZS',
    card_type VARCHAR(50),
    card_last_four VARCHAR(4),
    auth_code VARCHAR(50),
    gateway_reference VARCHAR(100),
    gateway_response VARCHAR(1000),
    gift_card_number VARCHAR(50),
    check_number VARCHAR(50),
    mobile_reference VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by BIGINT,
    voided_at TIMESTAMP WITH TIME ZONE,
    voided_by BIGINT,
    void_reason VARCHAR(500),
    refunded_at TIMESTAMP WITH TIME ZONE,
    refunded_by BIGINT,
    refund_reason VARCHAR(500),
    refund_amount NUMERIC(18, 4),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_pos_payments_transaction ON pos_payments(transaction_id);
CREATE INDEX idx_pos_payments_type ON pos_payments(payment_type);
CREATE INDEX idx_pos_payments_status ON pos_payments(status);

-- =============================================================================
-- POS PERMISSIONS
-- =============================================================================
INSERT INTO permissions (name, code, description, module, action, created_at, updated_at) VALUES
-- Terminal permissions
('Create POS Terminal', 'POS_TERMINAL_CREATE', 'Create new POS terminals', 'POS', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Read POS Terminal', 'POS_TERMINAL_READ', 'View POS terminals', 'POS', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Update POS Terminal', 'POS_TERMINAL_UPDATE', 'Update POS terminals', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Delete POS Terminal', 'POS_TERMINAL_DELETE', 'Delete POS terminals', 'POS', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Shift permissions
('Open Shift', 'POS_SHIFT_OPEN', 'Open a new shift', 'POS', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Close Shift', 'POS_SHIFT_CLOSE', 'Close a shift', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('View Shifts', 'POS_SHIFT_READ', 'View shift information', 'POS', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cash In/Out', 'POS_SHIFT_CASH_OPERATION', 'Perform cash in/out operations', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Sale permissions
('Create Sale', 'POS_SALE_CREATE', 'Create new POS sales', 'POS', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Read Sales', 'POS_SALE_READ', 'View POS sales', 'POS', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Void Sale', 'POS_SALE_VOID', 'Void POS transactions', 'POS', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Refund Sale', 'POS_SALE_REFUND', 'Process refunds', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hold Sale', 'POS_SALE_HOLD', 'Hold and recall transactions', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Discount permissions
('Apply Discount', 'POS_DISCOUNT_APPLY', 'Apply discounts to transactions', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Override Price', 'POS_PRICE_OVERRIDE', 'Override item prices', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Discount Override', 'POS_DISCOUNT_OVERRIDE', 'Override discount limits', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Payment permissions
('Process Payment', 'POS_PAYMENT_PROCESS', 'Process payments', 'POS', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Void Payment', 'POS_PAYMENT_VOID', 'Void payments', 'POS', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Cash drawer permissions
('Open Drawer', 'POS_DRAWER_OPEN', 'Open cash drawer', 'POS', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Report permissions
('View POS Reports', 'POS_REPORTS_VIEW', 'View POS reports', 'POS', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant POS permissions to relevant roles
-- SUPER_ADMIN gets all permissions (they already have wildcard)
-- STORE_MANAGER gets most POS permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'STORE_MANAGER'
AND p.code IN (
    'POS_TERMINAL_READ', 'POS_SHIFT_OPEN', 'POS_SHIFT_CLOSE', 'POS_SHIFT_READ',
    'POS_SHIFT_CASH_OPERATION', 'POS_SALE_CREATE', 'POS_SALE_READ', 'POS_SALE_VOID',
    'POS_SALE_REFUND', 'POS_SALE_HOLD', 'POS_DISCOUNT_APPLY', 'POS_PRICE_OVERRIDE',
    'POS_DISCOUNT_OVERRIDE', 'POS_PAYMENT_PROCESS', 'POS_PAYMENT_VOID', 'POS_DRAWER_OPEN',
    'POS_REPORTS_VIEW'
)
ON CONFLICT DO NOTHING;

-- CASHIER gets basic sale permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'CASHIER'
AND p.code IN (
    'POS_SHIFT_READ', 'POS_SALE_CREATE', 'POS_SALE_READ', 'POS_SALE_HOLD',
    'POS_DISCOUNT_APPLY', 'POS_PAYMENT_PROCESS', 'POS_DRAWER_OPEN'
)
ON CONFLICT DO NOTHING;

COMMENT ON TABLE pos_terminals IS 'POS terminals/registers';
COMMENT ON TABLE pos_shifts IS 'Cashier working shifts';
COMMENT ON TABLE pos_transactions IS 'POS transactions (sales, returns, exchanges)';
COMMENT ON TABLE pos_transaction_lines IS 'Line items for POS transactions';
COMMENT ON TABLE pos_payments IS 'Payments for POS transactions';
