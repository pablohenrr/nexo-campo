CREATE TABLE webhook_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payload TEXT NOT NULL,
    signature VARCHAR(255) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
