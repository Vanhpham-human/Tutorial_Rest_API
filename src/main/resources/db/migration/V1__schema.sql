CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(30),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_sequences (
    seq_year INT PRIMARY KEY,
    last_value BIGINT NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    card_provider VARCHAR(60),
    card_last4 VARCHAR(4),
    bank_code VARCHAR(32),
    transfer_ref VARCHAR(64),
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_orders_code UNIQUE (code),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uk_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE order_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    action VARCHAR(32),
    actor VARCHAR(180) NOT NULL,
    reason VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_events_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content BLOB NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_invoice_order UNIQUE (order_id),
    CONSTRAINT fk_invoice_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE idempotency_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body VARCHAR(4000) NOT NULL,
    location VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idem_customer_key UNIQUE (customer_id, idempotency_key),
    CONSTRAINT fk_idem_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);
