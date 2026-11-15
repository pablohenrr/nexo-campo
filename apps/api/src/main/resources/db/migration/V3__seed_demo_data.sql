INSERT INTO app_user (id, name, email, password_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Usuário Demo',
    'demo@nexocampo.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'PRODUTOR'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO farm (
    id, name, owner_id, description, municipality, state, total_area_hectares, latitude, longitude
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

INSERT INTO plot (
    id, farm_id, name, crop, area_hectares, status, boundary
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

INSERT INTO activity (
    id, plot_id, responsible_id, type, description, activity_date, cost, inputs
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
