-- V10 está reservada para o login com Google (feature/oauth-google).
CREATE TABLE vinculo_whatsapp (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    telefone VARCHAR(20) NOT NULL UNIQUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
