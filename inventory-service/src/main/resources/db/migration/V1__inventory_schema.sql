CREATE TABLE product_stock (
    sku VARCHAR(128) PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0)
);

CREATE TABLE reservations (
    reservation_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sku VARCHAR(128) NOT NULL REFERENCES product_stock (sku),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);
