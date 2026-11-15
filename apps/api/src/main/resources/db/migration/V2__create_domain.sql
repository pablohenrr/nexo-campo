CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'PRODUTOR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE farm (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(160) NOT NULL,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    description VARCHAR(500),
    municipality VARCHAR(120),
    state VARCHAR(2),
    total_area_hectares NUMERIC(12, 3),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES farm(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    crop VARCHAR(120),
    area_hectares NUMERIC(12, 3),
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    boundary geometry(Polygon, 4326),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_plot_boundary ON plot USING GIST (boundary);

CREATE TABLE activity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plot_id UUID NOT NULL REFERENCES plot(id) ON DELETE CASCADE,
    responsible_id UUID NOT NULL REFERENCES app_user(id),
    type VARCHAR(80) NOT NULL,
    description VARCHAR(1000),
    activity_date DATE NOT NULL,
    cost NUMERIC(12, 2),
    inputs VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
