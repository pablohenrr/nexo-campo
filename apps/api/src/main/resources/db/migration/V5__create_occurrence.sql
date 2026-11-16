CREATE TABLE occurrence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plot_id UUID NOT NULL REFERENCES plot(id) ON DELETE CASCADE,
    reported_by UUID NOT NULL REFERENCES app_user(id),
    category VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    description VARCHAR(1000) NOT NULL,
    observed_at DATE NOT NULL,
    evidence_id UUID REFERENCES evidence(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_occurrence_plot ON occurrence (plot_id, observed_at DESC);
