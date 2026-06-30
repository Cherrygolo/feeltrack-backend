CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(50) UNIQUE,
    phone VARCHAR(15)
);

CREATE TABLE review (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    text VARCHAR(1000),
    type VARCHAR(10),
    customer_id BIGINT,
    CONSTRAINT fk_review_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(id)
);

CREATE INDEX idx_review_created_at_type
ON review (created_at, type);