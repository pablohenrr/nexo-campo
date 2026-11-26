ALTER TABLE usuario
    ADD COLUMN telefone VARCHAR(20),
    ADD COLUMN notificacoes_sms_ativas BOOLEAN NOT NULL DEFAULT FALSE;
