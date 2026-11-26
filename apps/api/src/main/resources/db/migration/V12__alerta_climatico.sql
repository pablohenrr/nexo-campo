CREATE TABLE preferencia_alerta_climatico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fazenda_id UUID NOT NULL UNIQUE REFERENCES fazenda(id) ON DELETE CASCADE,
    raio_km INTEGER NOT NULL DEFAULT 30,
    categorias VARCHAR(200) NOT NULL DEFAULT 'CHUVA,VENTO,TEMPESTADE',
    frequencia VARCHAR(20) NOT NULL DEFAULT 'POR_EVENTO',
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_preferencia_alerta_raio CHECK (raio_km BETWEEN 1 AND 80),
    CONSTRAINT ck_preferencia_alerta_frequencia CHECK (frequencia IN ('POR_EVENTO', 'DIARIO', 'SEMANAL'))
);

CREATE TABLE evento_climatico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fonte VARCHAR(20) NOT NULL,
    id_externo VARCHAR(200) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    inicio TIMESTAMPTZ NOT NULL,
    fim TIMESTAMPTZ NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    geometria TEXT,
    texto VARCHAR(2000),
    frase VARCHAR(400) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_evento_climatico_fonte_externo UNIQUE (fonte, id_externo, categoria),
    CONSTRAINT ck_evento_climatico_fonte CHECK (fonte IN ('INMET', 'OPEN_METEO')),
    CONSTRAINT ck_evento_climatico_categoria CHECK (
        categoria IN ('CHUVA', 'VENTO', 'TEMPESTADE', 'GRANIZO', 'GEADA', 'UMIDADE_BAIXA', 'TORNADO')
    )
);

CREATE INDEX idx_evento_climatico_criado_em ON evento_climatico (criado_em DESC);

CREATE TABLE evento_climatico_fazenda (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evento_id UUID NOT NULL REFERENCES evento_climatico(id) ON DELETE CASCADE,
    fazenda_id UUID NOT NULL REFERENCES fazenda(id) ON DELETE CASCADE,
    CONSTRAINT uk_evento_climatico_fazenda UNIQUE (evento_id, fazenda_id)
);

CREATE INDEX idx_evento_climatico_fazenda_fazenda ON evento_climatico_fazenda (fazenda_id);

CREATE TABLE leitura_alerta_climatico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    evento_id UUID NOT NULL REFERENCES evento_climatico(id) ON DELETE CASCADE,
    lida_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_leitura_alerta_climatico UNIQUE (usuario_id, evento_id)
);

CREATE INDEX idx_leitura_alerta_climatico_usuario ON leitura_alerta_climatico (usuario_id);
