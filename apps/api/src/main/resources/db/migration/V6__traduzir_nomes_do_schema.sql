ALTER TABLE app_user RENAME TO usuario;
ALTER TABLE farm RENAME TO fazenda;
ALTER TABLE plot RENAME TO talhao;
ALTER TABLE activity RENAME TO atividade;
ALTER TABLE evidence RENAME TO evidencia;

ALTER TABLE usuario RENAME COLUMN name TO nome;
ALTER TABLE usuario RENAME COLUMN password_hash TO senha_hash;
ALTER TABLE usuario RENAME COLUMN role TO perfil;

ALTER TABLE fazenda RENAME COLUMN owner_id TO proprietario_id;
ALTER TABLE fazenda RENAME COLUMN description TO descricao;
ALTER TABLE fazenda RENAME COLUMN municipality TO municipio;
ALTER TABLE fazenda RENAME COLUMN state TO estado;
ALTER TABLE fazenda RENAME COLUMN total_area_hectares TO area_total_hectares;

ALTER TABLE talhao RENAME COLUMN farm_id TO fazenda_id;
ALTER TABLE talhao RENAME COLUMN name TO nome;
ALTER TABLE talhao RENAME COLUMN crop TO cultura;
ALTER TABLE talhao RENAME COLUMN status TO situacao;
ALTER TABLE talhao RENAME COLUMN boundary TO limite;

ALTER TABLE atividade RENAME COLUMN plot_id TO talhao_id;
ALTER TABLE atividade RENAME COLUMN responsible_id TO responsavel_id;
ALTER TABLE atividade RENAME COLUMN type TO tipo;
ALTER TABLE atividade RENAME COLUMN description TO descricao;
ALTER TABLE atividade RENAME COLUMN activity_date TO data_atividade;
ALTER TABLE atividade RENAME COLUMN cost TO custo;
ALTER TABLE atividade RENAME COLUMN inputs TO insumos;

ALTER TABLE evidencia RENAME COLUMN plot_id TO talhao_id;
ALTER TABLE evidencia RENAME COLUMN uploaded_by TO enviado_por;
ALTER TABLE evidencia RENAME COLUMN type TO tipo;
ALTER TABLE evidencia RENAME COLUMN original_name TO nome_original;
ALTER TABLE evidencia RENAME COLUMN content_type TO tipo_conteudo;
ALTER TABLE evidencia RENAME COLUMN size_bytes TO tamanho_bytes;
ALTER TABLE evidencia RENAME COLUMN storage_key TO chave_armazenamento;
ALTER TABLE evidencia RENAME COLUMN created_at TO criada_em;
