CREATE TABLE payments
(
    id              UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    sender_id       VARCHAR(50)         NOT NULL,
    receiver_id     VARCHAR(50)         NOT NULL,
    amount          DECIMAL(15, 2)      NOT NULL,
    currency        VARCHAR(3)          NOT NULL DEFAULT 'USD',
    status          VARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    created_at      TIMESTAMP                    DEFAULT NOW(),
    updated_at      TIMESTAMP                    DEFAULT NOW()
);
