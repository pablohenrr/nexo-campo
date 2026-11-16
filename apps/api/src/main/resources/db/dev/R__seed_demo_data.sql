INSERT INTO usuario (id, nome, email, senha_hash, perfil)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Usuário Demo',
    'demo@nexocampo.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'PRODUTOR'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO fazenda (
    id, nome, proprietario_id, descricao, municipio, estado, area_total_hectares, latitude, longitude
)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    'Fazenda NEXO',
    '00000000-0000-0000-0000-000000000001',
    'Fazenda criada para demonstração do MVP',
    'Londrina',
    'PR',
    120.500,
    -23.3100000,
    -51.1600000
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO talhao (
    id, fazenda_id, nome, cultura, area_hectares, situacao, limite
)
VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000010',
    'Talhão 01',
    'Soja',
    12.300,
    'ATIVO',
    ST_GeomFromText(
        'POLYGON((-51.161 -23.309, -51.159 -23.309, -51.159 -23.311, -51.161 -23.311, -51.161 -23.309))',
        4326
    )
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO atividade (
    id, talhao_id, responsavel_id, tipo, descricao, data_atividade, custo, insumos
)
VALUES (
    '00000000-0000-0000-0000-000000000030',
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000001',
    'PLANTIO',
    'Plantio inicial do talhão',
    CURRENT_DATE,
    1500.00,
    'Sementes de soja'
)
ON CONFLICT (id) DO NOTHING;
