ALTER TABLE usuario ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_google_subject UNIQUE (google_subject);
