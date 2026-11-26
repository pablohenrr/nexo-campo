-- Contorno da propriedade inteira, desenhado com pinos no cadastro da fazenda.
-- Fica opcional para não invalidar fazendas antigas, que só têm latitude/longitude.
-- Quando presente, latitude/longitude e area_total_hectares são calculados a partir dele.
ALTER TABLE fazenda ADD COLUMN limite geometry(Polygon, 4326);

CREATE INDEX idx_fazenda_limite ON fazenda USING GIST (limite);
