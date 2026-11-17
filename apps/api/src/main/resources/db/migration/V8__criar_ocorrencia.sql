CREATE TABLE ocorrencia (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    talhao_id UUID NOT NULL REFERENCES talhao(id) ON DELETE CASCADE,
    responsavel_id UUID NOT NULL REFERENCES usuario(id),
    categoria VARCHAR(40) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    descricao VARCHAR(1000) NOT NULL,
    data_observacao DATE NOT NULL,
    evidencia_id UUID REFERENCES evidencia(id) ON DELETE SET NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ocorrencia_talhao ON ocorrencia (talhao_id, data_observacao DESC);
