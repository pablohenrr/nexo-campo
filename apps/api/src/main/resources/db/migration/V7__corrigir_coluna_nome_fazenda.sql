-- A V6 traduziu as outras colunas de fazenda, mas esqueceu esta; a entidade Farm já mapeia "nome".
ALTER TABLE fazenda RENAME COLUMN name TO nome;
