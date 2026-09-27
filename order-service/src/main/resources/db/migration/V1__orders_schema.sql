CREATE TABLE orders (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    customer_id VARCHAR(128) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(16) CHECK (status IN ('CONFIRMED', 'REJECTED')),
    reservation_id UUID,
    rejection_reason VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT order_outcome CHECK (
        (status IS NULL AND reservation_id IS NULL AND rejection_reason IS NULL)
        OR (status = 'CONFIRMED' AND reservation_id IS NOT NULL AND rejection_reason IS NULL)
        OR (status = 'REJECTED' AND reservation_id IS NULL AND rejection_reason IS NOT NULL)
    )
);

CREATE INDEX orders_created_at_idx ON orders (created_at, id) WHERE status IS NOT NULL;
