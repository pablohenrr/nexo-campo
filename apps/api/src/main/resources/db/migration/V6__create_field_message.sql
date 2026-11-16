CREATE TABLE field_message (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES app_user(id),
    channel VARCHAR(20) NOT NULL,
    external_id VARCHAR(120),
    text VARCHAR(4000),
    transcript VARCHAR(4000),
    media_type VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    kind VARCHAR(30),
    plot_id UUID REFERENCES plot(id) ON DELETE SET NULL,
    evidence_id UUID REFERENCES evidence(id) ON DELETE SET NULL,
    activity_id UUID REFERENCES activity(id) ON DELETE SET NULL,
    occurrence_id UUID REFERENCES occurrence(id) ON DELETE SET NULL,
    interpretation JSONB,
    reply VARCHAR(1000),
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- O mesmo id de mensagem não pode ser processado duas vezes (o WhatsApp reenvia webhooks).
CREATE UNIQUE INDEX uq_field_message_channel_external_id
    ON field_message (channel, external_id)
    WHERE external_id IS NOT NULL;

CREATE INDEX idx_field_message_sender ON field_message (sender_id, created_at DESC);
