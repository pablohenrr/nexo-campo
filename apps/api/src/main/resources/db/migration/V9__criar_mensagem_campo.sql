CREATE TABLE mensagem_campo (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    remetente_id UUID NOT NULL REFERENCES usuario(id),
    canal VARCHAR(20) NOT NULL,
    id_externo VARCHAR(120),
    texto VARCHAR(4000),
    transcricao VARCHAR(4000),
    tipo_midia VARCHAR(30),
    situacao VARCHAR(30) NOT NULL,
    tipo VARCHAR(30),
    talhao_id UUID REFERENCES talhao(id) ON DELETE SET NULL,
    evidencia_id UUID REFERENCES evidencia(id) ON DELETE SET NULL,
    atividade_id UUID REFERENCES atividade(id) ON DELETE SET NULL,
    ocorrencia_id UUID REFERENCES ocorrencia(id) ON DELETE SET NULL,
    interpretacao JSONB,
    resposta VARCHAR(1000),
    mensagem_erro VARCHAR(500),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- O mesmo id de mensagem não pode ser processado duas vezes (o WhatsApp reenvia webhooks).
CREATE UNIQUE INDEX uq_mensagem_campo_canal_id_externo
    ON mensagem_campo (canal, id_externo)
    WHERE id_externo IS NOT NULL;

CREATE INDEX idx_mensagem_campo_remetente ON mensagem_campo (remetente_id, criada_em DESC);
