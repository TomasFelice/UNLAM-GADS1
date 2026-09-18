-- Esquema y datos semilla de zTech CRM para una base vacía.
-- Consolidación de V1–V4 autorizada tras el reinicio de la base (17/09/2026).
-- Referencia: docs/specs-backend/design.md §3-4, ADR-003 (multi-tenancy).

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- =====================================================================
-- tenancy
-- =====================================================================

CREATE TABLE tenants (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- access
-- =====================================================================

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenants (id),
    email         TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    first_name    TEXT NOT NULL,
    last_name     TEXT NOT NULL,
    role          TEXT NOT NULL CHECK (role IN ('ADMIN', 'SELLER', 'SALES_MANAGER')),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    auth_version  BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    BIGINT REFERENCES users (id),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by    BIGINT REFERENCES users (id),
    -- Único a nivel sistema, no por tenant: el login (BE-SEC-01) recibe sólo
    -- email + password, sin selector de organización (no lo pide la consigna,
    -- y BE-SEC-08 no tiene alta pública de tenants). Con un solo tenant
    -- funcionando en la práctica, la unicidad por tenant sería equivalente
    -- pero obligaría a resolver el tenant antes de autenticar.
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_auth_version CHECK (auth_version >= 0)
);

CREATE INDEX ix_users_tenant ON users (tenant_id);

-- =====================================================================
-- catalogs (configurables por ADMIN, precargados al final de esta migración)
-- =====================================================================

CREATE TABLE origins (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_origins_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE loss_reasons (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_loss_reasons_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE activity_types (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_activity_types_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE event_types (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_types_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX ix_event_types_tenant ON event_types (tenant_id);

CREATE TABLE stages (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    position   INTEGER NOT NULL,
    kind       TEXT NOT NULL CHECK (kind IN ('OPEN', 'WON', 'LOST')),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stages_tenant_position UNIQUE (tenant_id, position)
);

-- =====================================================================
-- offerings ("producto o servicio" de la consigna — ADR-004)
-- =====================================================================

CREATE TABLE venues (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id  BIGINT NOT NULL REFERENCES tenants (id),
    name       TEXT NOT NULL,
    capacity   INTEGER NOT NULL CHECK (capacity > 0),
    rate       NUMERIC(15, 2),
    address    TEXT,
    status     TEXT NOT NULL,
    locality   TEXT,
    description TEXT,
    equipment  TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES users (id),
    CONSTRAINT ck_venues_status CHECK (status IN ('DISPONIBLE', 'MANTENIMIENTO', 'INACTIVO'))
);

CREATE INDEX ix_venues_tenant ON venues (tenant_id);

CREATE TABLE event_services (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants (id),
    name        TEXT NOT NULL,
    description TEXT,
    price       NUMERIC(15, 2),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT REFERENCES users (id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT REFERENCES users (id)
);

CREATE INDEX ix_event_services_tenant ON event_services (tenant_id);

-- =====================================================================
-- customers
-- =====================================================================

CREATE TABLE companies (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id    BIGINT NOT NULL REFERENCES tenants (id),
    business_name TEXT NOT NULL,
    legal_name   TEXT NOT NULL,
    cuit         TEXT,
    industry     TEXT,
    email        TEXT,
    phone        TEXT,
    address      TEXT,
    locality     TEXT,
    website      TEXT,
    status       TEXT NOT NULL CHECK (status IN ('POTENCIAL', 'CLIENTE', 'INACTIVO', 'NO_CONTACTAR')),
    sales_rep_id BIGINT NOT NULL REFERENCES users (id),
    origin_id    BIGINT REFERENCES origins (id),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   BIGINT REFERENCES users (id),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   BIGINT REFERENCES users (id)
);

CREATE INDEX ix_companies_tenant ON companies (tenant_id);
CREATE INDEX ix_companies_tenant_sales_rep ON companies (tenant_id, sales_rep_id);
CREATE UNIQUE INDEX ux_companies_tenant_cuit ON companies (tenant_id, cuit) WHERE cuit IS NOT NULL;

CREATE TABLE contacts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id    BIGINT NOT NULL REFERENCES tenants (id),
    company_id   BIGINT REFERENCES companies (id),
    first_name   TEXT NOT NULL,
    last_name    TEXT NOT NULL,
    document     TEXT,
    position     TEXT,
    email        TEXT,
    phone        TEXT,
    status       TEXT NOT NULL CHECK (status IN ('POTENCIAL', 'CLIENTE', 'INACTIVO', 'NO_CONTACTAR')),
    sales_rep_id BIGINT NOT NULL REFERENCES users (id),
    origin_id    BIGINT REFERENCES origins (id),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   BIGINT REFERENCES users (id),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   BIGINT REFERENCES users (id)
);

CREATE INDEX ix_contacts_tenant ON contacts (tenant_id);
CREATE INDEX ix_contacts_tenant_company ON contacts (tenant_id, company_id);
CREATE INDEX ix_contacts_tenant_sales_rep ON contacts (tenant_id, sales_rep_id);
CREATE UNIQUE INDEX ux_contacts_tenant_document ON contacts (tenant_id, document) WHERE document IS NOT NULL;

-- =====================================================================
-- opportunities
-- =====================================================================

CREATE TABLE opportunities (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id             BIGINT NOT NULL REFERENCES tenants (id),
    title                 TEXT NOT NULL,
    company_id            BIGINT REFERENCES companies (id),
    contact_id            BIGINT REFERENCES contacts (id),
    sales_rep_id          BIGINT NOT NULL REFERENCES users (id),
    venue_id              BIGINT NOT NULL REFERENCES venues (id),
    stage_id              BIGINT NOT NULL REFERENCES stages (id),
    status                TEXT NOT NULL CHECK (status IN ('ABIERTA', 'GANADA', 'PERDIDA')),
    estimated_value       NUMERIC(15, 2),
    final_value           NUMERIC(15, 2),
    probability           INTEGER CHECK (probability BETWEEN 0 AND 100),
    event_type_id         BIGINT NOT NULL REFERENCES event_types (id),
    event_start           TIMESTAMPTZ NOT NULL,
    event_end             TIMESTAMPTZ NOT NULL,
    attendee_count        INTEGER NOT NULL CHECK (attendee_count > 0),
    estimated_close_date  DATE,
    closed_at             TIMESTAMPTZ,
    origin_id             BIGINT REFERENCES origins (id),
    loss_reason_id        BIGINT REFERENCES loss_reasons (id),
    notes                 TEXT,
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            BIGINT REFERENCES users (id),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by            BIGINT REFERENCES users (id),
    CONSTRAINT ck_opportunities_party CHECK (company_id IS NOT NULL OR contact_id IS NOT NULL),
    CONSTRAINT ck_opportunities_event_range CHECK (event_end > event_start),
    CONSTRAINT no_overlapping_won_reservation EXCLUDE USING gist (
        tenant_id WITH =,
        venue_id WITH =,
        tstzrange(event_start, event_end, '[)') WITH &&
    ) WHERE (status = 'GANADA')
);

CREATE INDEX ix_opportunities_tenant ON opportunities (tenant_id);
CREATE INDEX ix_opportunities_tenant_stage ON opportunities (tenant_id, stage_id);
CREATE INDEX ix_opportunities_tenant_sales_rep ON opportunities (tenant_id, sales_rep_id);
CREATE INDEX ix_opportunities_tenant_status ON opportunities (tenant_id, status);
CREATE INDEX ix_opportunities_tenant_venue ON opportunities (tenant_id, venue_id);

CREATE INDEX ix_opportunities_tenant_event_range
    ON opportunities (tenant_id, event_start, event_end);

CREATE TABLE opportunity_event_services (
    opportunity_id BIGINT NOT NULL REFERENCES opportunities (id),
    event_service_id BIGINT NOT NULL REFERENCES event_services (id),
    PRIMARY KEY (opportunity_id, event_service_id)
);

CREATE INDEX ix_opportunity_event_services_service
    ON opportunity_event_services (event_service_id);

-- =====================================================================
-- historial de etapas — append-only (BE-ACT-03/04)
-- =====================================================================

CREATE TABLE stage_history (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    opportunity_id BIGINT NOT NULL REFERENCES opportunities (id),
    from_stage_id  BIGINT REFERENCES stages (id),
    to_stage_id    BIGINT NOT NULL REFERENCES stages (id),
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by     BIGINT NOT NULL REFERENCES users (id),
    note           TEXT
);

CREATE INDEX ix_stage_history_tenant_opportunity ON stage_history (tenant_id, opportunity_id);

-- =====================================================================
-- activities
-- =====================================================================

CREATE TABLE activities (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenants (id),
    type_id        BIGINT NOT NULL REFERENCES activity_types (id),
    occurred_at    TIMESTAMPTZ NOT NULL,
    company_id     BIGINT REFERENCES companies (id),
    contact_id     BIGINT REFERENCES contacts (id),
    opportunity_id BIGINT REFERENCES opportunities (id),
    description    TEXT,
    result         TEXT,
    created_by     BIGINT NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_activities_related CHECK (
        company_id IS NOT NULL OR contact_id IS NOT NULL OR opportunity_id IS NOT NULL
    )
);

CREATE INDEX ix_activities_tenant_company ON activities (tenant_id, company_id);
CREATE INDEX ix_activities_tenant_contact ON activities (tenant_id, contact_id);
CREATE INDEX ix_activities_tenant_opportunity ON activities (tenant_id, opportunity_id);

-- Datos semilla: un tenant, un ADMIN habilitado, el embudo de DP-06, y los
-- catálogos y salones precargados que exige E1 (BE-ACC-01, BE-CAT-01, BE-OFF-01/03).
-- Referencia: docs/specs-backend/design.md §4, docs/decisiones/decisiones-pendientes.md (DP-06).

-- =====================================================================
-- Tenant semilla
-- =====================================================================

INSERT INTO tenants (name) VALUES ('zTech Eventos Corporativos');

-- =====================================================================
-- Usuario ADMIN semilla
-- Password de demo: "Admin123!" (BCrypt, cost 10). Documentado en
-- backend/README.md para la demostración; no es una credencial productiva.
-- =====================================================================

INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active, must_change_password)
SELECT id, 'admin@ztech.local',
       '$2b$10$SUb/kgJkQMuMg4bfEDU9ruJTt7Jldlvr7sFAXShs14nxnoc4IS/ii',
       'Admin', 'zTech', 'ADMIN', TRUE, TRUE
FROM tenants WHERE name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Etapas del embudo (DP-06) — 7 etapas con visita al salón
-- =====================================================================

INSERT INTO stages (tenant_id, name, position, kind)
SELECT t.id, s.name, s.position, s.kind
FROM tenants t
CROSS JOIN (VALUES
    ('Consulta recibida',   1, 'OPEN'),
    ('Necesidad relevada',  2, 'OPEN'),
    ('Visita al salón',     3, 'OPEN'),
    ('Propuesta enviada',   4, 'OPEN'),
    ('Negociación',         5, 'OPEN'),
    ('Ganada / Reservado',  6, 'WON'),
    ('Perdida',             7, 'LOST')
) AS s(name, position, kind)
WHERE t.name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Salones (DP-09, ADR-004) — precargados para E1
-- =====================================================================

INSERT INTO venues (tenant_id, name, capacity, rate, address, status)
SELECT t.id, v.name, v.capacity, v.rate, v.address, 'DISPONIBLE'
FROM tenants t
CROSS JOIN (VALUES
    ('Salón Jacarandá', 50, 350000.00, 'Av. Rivadavia 4500, San Justo'),
    ('Salón Ceibo',      30, 220000.00, 'Av. Rivadavia 4500, San Justo'),
    ('Salón Palo Borracho', 15, 140000.00, 'Av. Rivadavia 4500, San Justo')
) AS v(name, capacity, rate, address)
WHERE t.name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Servicios adicionales (ADR-004)
-- =====================================================================

INSERT INTO event_services (tenant_id, name, description, price)
SELECT t.id, es.name, es.description, es.price
FROM tenants t
CROSS JOIN (VALUES
    ('Catering estándar',   'Menú de tres pasos por persona', 15000.00),
    ('Audio y sonido',      'Equipo de sonido con técnico', 60000.00),
    ('Decoración temática', 'Ambientación y centros de mesa', 45000.00),
    ('Mobiliario adicional','Mesas, sillas y vajilla extra', 25000.00)
) AS es(name, description, price)
WHERE t.name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Orígenes comerciales
-- =====================================================================

INSERT INTO origins (tenant_id, name)
SELECT t.id, o.name
FROM tenants t
CROSS JOIN (VALUES
    ('Sitio web'),
    ('Redes sociales'),
    ('Publicidad'),
    ('Recomendación'),
    ('Evento'),
    ('Prospección comercial'),
    ('Cliente existente')
) AS o(name)
WHERE t.name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Motivos de pérdida
-- =====================================================================

INSERT INTO loss_reasons (tenant_id, name)
SELECT t.id, lr.name
FROM tenants t
CROSS JOIN (VALUES
    ('Precio'),
    ('Falta de presupuesto'),
    ('Elección de un competidor'),
    ('Salón no disponible en la fecha'),
    ('Falta de respuesta'),
    ('Decisión postergada')
) AS lr(name)
WHERE t.name = 'zTech Eventos Corporativos';

-- =====================================================================
-- Tipos de actividad
-- =====================================================================

INSERT INTO activity_types (tenant_id, name)
SELECT t.id, at.name
FROM tenants t
CROSS JOIN (VALUES
    ('Llamada'),
    ('Correo electrónico'),
    ('Mensaje'),
    ('Reunión presencial'),
    ('Reunión virtual'),
    ('Demostración'),
    ('Envío de propuesta'),
    ('Nota interna'),
    ('Otro')
) AS at(name)
WHERE t.name = 'zTech Eventos Corporativos';

-- Tipo de evento general (DP-06).
INSERT INTO event_types (tenant_id, name)
SELECT id, 'Evento corporativo' FROM tenants;

-- Escenario comercial compacto para recorrer el frontend integrado.
-- Las credenciales de demostración usan Admin123! y exigen cambio al ingresar.

UPDATE venues SET
    locality = 'San Justo',
    description = CASE name
        WHEN 'Salón Jacarandá' THEN 'Salón principal para jornadas, lanzamientos y celebraciones corporativas.'
        WHEN 'Salón Ceibo' THEN 'Espacio versátil para capacitaciones, desayunos y encuentros de equipos.'
        ELSE 'Sala ejecutiva para reuniones, workshops y presentaciones privadas.' END,
    equipment = CASE name
        WHEN 'Salón Jacarandá' THEN ARRAY['Proyector 4K', 'Sonido profesional', 'Escenario modular', 'Wi-Fi']
        WHEN 'Salón Ceibo' THEN ARRAY['Pantalla', 'Audio', 'Pizarra', 'Wi-Fi']
        ELSE ARRAY['Pantalla 65 pulgadas', 'Videoconferencia', 'Pizarra', 'Wi-Fi'] END
WHERE tenant_id = (SELECT id FROM tenants WHERE name = 'zTech Eventos Corporativos');

INSERT INTO event_types (tenant_id, name)
SELECT tenant.id, event_type.name
FROM tenants tenant
CROSS JOIN (VALUES ('Capacitación'), ('Lanzamiento de producto'), ('Reunión ejecutiva'),
                   ('Celebración corporativa')) AS event_type(name)
WHERE tenant.name = 'zTech Eventos Corporativos';

INSERT INTO users (tenant_id, email, password_hash, first_name, last_name, role, active,
                   must_change_password)
SELECT tenant.id, account.email,
       '$2b$10$SUb/kgJkQMuMg4bfEDU9ruJTt7Jldlvr7sFAXShs14nxnoc4IS/ii',
       account.first_name, account.last_name, account.role, TRUE, TRUE
FROM tenants tenant
CROSS JOIN (VALUES
    ('vendedor@ztech.local', 'Martín', 'Sosa', 'SELLER'),
    ('gerente@ztech.local', 'Laura', 'Pereyra', 'SALES_MANAGER')
) AS account(email, first_name, last_name, role)
WHERE tenant.name = 'zTech Eventos Corporativos';

INSERT INTO companies (tenant_id, legal_name, business_name, cuit, industry, email, phone,
                       address, locality, website, status, sales_rep_id, origin_id, notes,
                       created_by, updated_by)
SELECT tenant.id, company.legal_name, company.business_name, company.cuit, company.industry,
       company.email, company.phone, company.address, company.locality, company.website,
       company.status, responsible.id, origin.id, company.notes, responsible.id, responsible.id
FROM tenants tenant
CROSS JOIN (VALUES
    ('Andina Tecnología S.A.', 'Andina Tech', '30712345678', 'Tecnología', 'eventos@andinatech.com.ar', '+54 11 4100-1200', 'Av. Belgrano 820', 'CABA', 'https://andinatech.example', 'CLIENTE', 'vendedor@ztech.local', 'Sitio web', 'Planifica su encuentro anual.'),
    ('Laboratorios Horizonte S.R.L.', 'Horizonte', '30723456789', 'Salud', 'compras@horizonte.example', '+54 11 4200-2200', 'Arieta 3100', 'San Justo', 'https://horizonte.example', 'POTENCIAL', 'vendedor@ztech.local', 'Recomendación', 'Evalúa jornadas de capacitación.'),
    ('Nexo Logística S.A.', 'Nexo Logística', '30734567890', 'Logística', 'personas@nexo.example', '+54 11 4300-3300', 'Ruta 3 4800', 'La Matanza', 'https://nexo.example', 'POTENCIAL', 'gerente@ztech.local', 'Evento', 'Necesita espacio para lanzamiento interno.'),
    ('Estudio Río y Asociados S.A.', 'Estudio Río', '30745678901', 'Servicios profesionales', 'administracion@estudiorio.example', '+54 11 4400-4400', 'Reconquista 640', 'CABA', 'https://estudiorio.example', 'CLIENTE', 'gerente@ztech.local', 'Cliente existente', 'Cliente recurrente para reuniones ejecutivas.'),
    ('Fundación Impulso', 'Impulso', '30756789012', 'Educación', 'alianzas@impulso.example', '+54 11 4500-5500', 'Mendoza 1150', 'Ramos Mejía', 'https://impulso.example', 'NO_CONTACTAR', 'admin@ztech.local', 'Redes sociales', 'Solicitud pausada por decisión de la organización.')
) AS company(legal_name, business_name, cuit, industry, email, phone, address, locality,
             website, status, responsible_email, origin_name, notes)
JOIN users responsible ON responsible.tenant_id = tenant.id AND responsible.email = company.responsible_email
JOIN origins origin ON origin.tenant_id = tenant.id AND origin.name = company.origin_name
WHERE tenant.name = 'zTech Eventos Corporativos';

INSERT INTO contacts (tenant_id, company_id, first_name, last_name, document, position, email,
                      phone, status, sales_rep_id, origin_id, notes, created_by, updated_by)
SELECT tenant.id, business.id, contact.first_name, contact.last_name, contact.document,
       contact.position, contact.email, contact.phone, contact.status, responsible.id, origin.id,
       contact.notes, responsible.id, responsible.id
FROM tenants tenant
CROSS JOIN (VALUES
    ('Andina Tech', 'Carolina', 'Suárez', '39111222', 'Jefa de Personas', 'carolina@andinatech.example', '+54 9 11 5100-1001', 'CLIENTE', 'vendedor@ztech.local', 'Sitio web', 'Prefiere contacto por correo.'),
    ('Andina Tech', 'Diego', 'Molina', '30222333', 'Compras', 'diego@andinatech.example', '+54 9 11 5100-1002', 'CLIENTE', 'vendedor@ztech.local', 'Sitio web', NULL),
    ('Horizonte', 'Paula', 'Benítez', '30333444', 'Capacitación', 'paula@horizonte.example', '+54 9 11 5200-2001', 'POTENCIAL', 'vendedor@ztech.local', 'Recomendación', 'Solicitó opciones de catering.'),
    ('Nexo Logística', 'Ramiro', 'Acosta', '30444555', 'Comunicación interna', 'ramiro@nexo.example', '+54 9 11 5300-3001', 'POTENCIAL', 'gerente@ztech.local', 'Evento', NULL),
    ('Estudio Río', 'Lucía', 'Ferrer', '30555666', 'Socia', 'lucia@estudiorio.example', '+54 9 11 5400-4001', 'CLIENTE', 'gerente@ztech.local', 'Cliente existente', 'Contacto principal.'),
    (NULL, 'Santiago', 'Vega', '30666777', 'Consultor', 'santiago.vega@example.com', '+54 9 11 5500-5001', 'POTENCIAL', 'admin@ztech.local', 'Redes sociales', 'Cliente individual.')
) AS contact(company_name, first_name, last_name, document, position, email, phone, status,
             responsible_email, origin_name, notes)
LEFT JOIN companies business ON business.tenant_id = tenant.id AND business.business_name = contact.company_name
JOIN users responsible ON responsible.tenant_id = tenant.id AND responsible.email = contact.responsible_email
JOIN origins origin ON origin.tenant_id = tenant.id AND origin.name = contact.origin_name
WHERE tenant.name = 'zTech Eventos Corporativos';

INSERT INTO opportunities (tenant_id, title, company_id, contact_id, sales_rep_id, venue_id,
                           stage_id, event_type_id, status, estimated_value, final_value,
                           probability, event_start, event_end, attendee_count,
                           estimated_close_date, closed_at, origin_id, loss_reason_id, notes,
                           created_by, updated_by)
SELECT tenant.id, opportunity.title, company.id, contact.id, responsible.id, venue.id, stage.id,
       event_type.id, opportunity.status, opportunity.estimated_value, opportunity.final_value,
       opportunity.probability, opportunity.event_start, opportunity.event_end,
       opportunity.attendees, opportunity.estimated_close_date, opportunity.closed_at,
       origin.id, loss_reason.id, opportunity.notes, responsible.id, responsible.id
FROM tenants tenant
CROSS JOIN (VALUES
    ('Jornada anual Andina', 'Andina Tech', 'Carolina', 'vendedor@ztech.local', 'Salón Jacarandá', 'Consulta recibida', 'Capacitación', 'ABIERTA', 680000.00, NULL, 20, '2026-10-08 12:00:00+00'::timestamptz, '2026-10-08 21:00:00+00'::timestamptz, 45, '2026-09-25'::date, NULL::timestamptz, 'Sitio web', NULL, 'Jornada para líderes de equipo.'),
    ('Workshop Horizonte', 'Horizonte', 'Paula', 'vendedor@ztech.local', 'Salón Ceibo', 'Necesidad relevada', 'Capacitación', 'ABIERTA', 390000.00, NULL, 35, '2026-10-15 13:00:00+00'::timestamptz, '2026-10-15 20:00:00+00'::timestamptz, 28, '2026-09-28'::date, NULL::timestamptz, 'Recomendación', NULL, 'Incluye catering estándar.'),
    ('Presentación Nexo', 'Nexo Logística', 'Ramiro', 'gerente@ztech.local', 'Salón Jacarandá', 'Visita al salón', 'Lanzamiento de producto', 'ABIERTA', 790000.00, NULL, 50, '2026-10-22 14:00:00+00'::timestamptz, '2026-10-22 22:00:00+00'::timestamptz, 50, '2026-10-02'::date, NULL::timestamptz, 'Evento', NULL, 'Requiere escenario y audio.'),
    ('Desayuno Estudio Río', 'Estudio Río', 'Lucía', 'gerente@ztech.local', 'Salón Palo Borracho', 'Propuesta enviada', 'Reunión ejecutiva', 'ABIERTA', 235000.00, NULL, 65, '2026-10-29 11:00:00+00'::timestamptz, '2026-10-29 15:00:00+00'::timestamptz, 12, '2026-09-30'::date, NULL::timestamptz, 'Cliente existente', NULL, 'Desayuno de socios.'),
    ('Encuentro de aliados Impulso', 'Impulso', NULL, 'admin@ztech.local', 'Salón Ceibo', 'Negociación', 'Evento corporativo', 'ABIERTA', 450000.00, NULL, 75, '2026-11-05 15:00:00+00'::timestamptz, '2026-11-05 21:00:00+00'::timestamptz, 30, '2026-10-05'::date, NULL::timestamptz, 'Redes sociales', NULL, 'Aguardando confirmación institucional.'),
    ('Directorio trimestral Río', 'Estudio Río', 'Lucía', 'gerente@ztech.local', 'Salón Palo Borracho', 'Ganada / Reservado', 'Reunión ejecutiva', 'GANADA', 190000.00, 185000.00, 100, '2026-09-18 12:00:00+00'::timestamptz, '2026-09-18 16:00:00+00'::timestamptz, 10, '2026-09-10'::date, '2026-09-09 18:00:00+00'::timestamptz, 'Cliente existente', NULL, 'Reserva confirmada.'),
    ('Celebración de cierre Andina', 'Andina Tech', 'Diego', 'vendedor@ztech.local', 'Salón Jacarandá', 'Perdida', 'Celebración corporativa', 'PERDIDA', 920000.00, NULL, 0, '2026-12-18 20:00:00+00'::timestamptz, '2026-12-19 03:00:00+00'::timestamptz, 50, '2026-09-08'::date, '2026-09-08 20:00:00+00'::timestamptz, 'Sitio web', 'Precio', 'Eligieron una alternativa de menor costo.')
) AS opportunity(title, company_name, contact_first_name, responsible_email, venue_name,
                 stage_name, event_type_name, status, estimated_value, final_value, probability,
                 event_start, event_end, attendees, estimated_close_date, closed_at,
                 origin_name, loss_reason_name, notes)
LEFT JOIN companies company ON company.tenant_id = tenant.id AND company.business_name = opportunity.company_name
LEFT JOIN contacts contact ON contact.tenant_id = tenant.id AND contact.first_name = opportunity.contact_first_name
JOIN users responsible ON responsible.tenant_id = tenant.id AND responsible.email = opportunity.responsible_email
JOIN venues venue ON venue.tenant_id = tenant.id AND venue.name = opportunity.venue_name
JOIN stages stage ON stage.tenant_id = tenant.id AND stage.name = opportunity.stage_name
JOIN event_types event_type ON event_type.tenant_id = tenant.id AND event_type.name = opportunity.event_type_name
JOIN origins origin ON origin.tenant_id = tenant.id AND origin.name = opportunity.origin_name
LEFT JOIN loss_reasons loss_reason ON loss_reason.tenant_id = tenant.id AND loss_reason.name = opportunity.loss_reason_name
WHERE tenant.name = 'zTech Eventos Corporativos';

INSERT INTO opportunity_event_services (opportunity_id, event_service_id)
SELECT opportunity.id, service.id
FROM opportunities opportunity
JOIN event_services service ON service.tenant_id = opportunity.tenant_id
WHERE opportunity.title IN ('Jornada anual Andina', 'Workshop Horizonte', 'Presentación Nexo')
  AND service.name IN ('Catering estándar', 'Audio y sonido');

WITH paths(title, stage_names) AS (VALUES
    ('Jornada anual Andina', ARRAY['Consulta recibida']),
    ('Workshop Horizonte', ARRAY['Consulta recibida', 'Necesidad relevada']),
    ('Presentación Nexo', ARRAY['Consulta recibida', 'Necesidad relevada', 'Visita al salón']),
    ('Desayuno Estudio Río', ARRAY['Consulta recibida', 'Necesidad relevada', 'Visita al salón', 'Propuesta enviada']),
    ('Encuentro de aliados Impulso', ARRAY['Consulta recibida', 'Necesidad relevada', 'Visita al salón', 'Propuesta enviada', 'Negociación']),
    ('Directorio trimestral Río', ARRAY['Consulta recibida', 'Necesidad relevada', 'Propuesta enviada', 'Negociación', 'Ganada / Reservado']),
    ('Celebración de cierre Andina', ARRAY['Consulta recibida', 'Necesidad relevada', 'Propuesta enviada', 'Negociación', 'Perdida'])
), expanded AS (
    SELECT opportunity.id opportunity_id, opportunity.tenant_id, opportunity.created_by,
           stage_name, ordinal, lag(stage_name) OVER (PARTITION BY opportunity.id ORDER BY ordinal) previous_name
    FROM paths
    JOIN opportunities opportunity ON opportunity.title = paths.title
    CROSS JOIN LATERAL unnest(paths.stage_names) WITH ORDINALITY AS value(stage_name, ordinal)
)
INSERT INTO stage_history (tenant_id, opportunity_id, from_stage_id, to_stage_id, changed_at, changed_by, note)
SELECT expanded.tenant_id, expanded.opportunity_id, previous_stage.id, current_stage.id,
       '2026-09-01 13:00:00+00'::timestamptz + (expanded.ordinal * INTERVAL '1 day'),
       expanded.created_by, CASE WHEN expanded.ordinal = 1 THEN 'Etapa inicial' ELSE 'Avance comercial' END
FROM expanded
JOIN stages current_stage ON current_stage.tenant_id = expanded.tenant_id AND current_stage.name = expanded.stage_name
LEFT JOIN stages previous_stage ON previous_stage.tenant_id = expanded.tenant_id AND previous_stage.name = expanded.previous_name;

INSERT INTO activities (tenant_id, type_id, occurred_at, company_id, contact_id, opportunity_id,
                        description, result, created_by, created_at)
SELECT tenant.id, activity_type.id, activity.occurred_at, company.id, contact.id, opportunity.id,
       activity.description, activity.result, author.id, activity.occurred_at + INTERVAL '10 minutes'
FROM tenants tenant
CROSS JOIN (VALUES
    ('Llamada', '2026-09-03 14:00:00+00'::timestamptz, 'Andina Tech', 'Carolina', 'Jornada anual Andina', 'Primera conversación sobre la jornada anual.', 'Se acordó relevar agenda y asistentes.', 'vendedor@ztech.local'),
    ('Reunión virtual', '2026-09-05 16:00:00+00'::timestamptz, 'Horizonte', 'Paula', 'Workshop Horizonte', 'Relevamiento de necesidades de capacitación.', 'Solicitaron propuesta con catering.', 'vendedor@ztech.local'),
    ('Reunión presencial', '2026-09-06 18:00:00+00'::timestamptz, 'Nexo Logística', 'Ramiro', 'Presentación Nexo', 'Visita técnica al salón Jacarandá.', 'Validaron escenario y conectividad.', 'gerente@ztech.local'),
    ('Envío de propuesta', '2026-09-07 13:00:00+00'::timestamptz, 'Estudio Río', 'Lucía', 'Desayuno Estudio Río', 'Propuesta por alquiler y desayuno.', 'Pendiente de aprobación de socios.', 'gerente@ztech.local'),
    ('Mensaje', '2026-09-08 15:00:00+00'::timestamptz, 'Impulso', NULL, 'Encuentro de aliados Impulso', 'Seguimiento de la propuesta institucional.', 'Piden una semana adicional.', 'admin@ztech.local'),
    ('Nota interna', '2026-09-09 18:10:00+00'::timestamptz, 'Estudio Río', 'Lucía', 'Directorio trimestral Río', 'Reserva confirmada por administración.', 'Horario y sala bloqueados.', 'gerente@ztech.local'),
    ('Correo electrónico', '2026-09-08 20:10:00+00'::timestamptz, 'Andina Tech', 'Diego', 'Celebración de cierre Andina', 'Confirmación de cierre de negociación.', 'La propuesta se perdió por precio.', 'vendedor@ztech.local'),
    ('Llamada', '2026-09-11 14:30:00+00'::timestamptz, 'Andina Tech', 'Carolina', NULL, 'Consulta por opciones de menú.', 'Se enviará detalle de catering.', 'vendedor@ztech.local'),
    ('Correo electrónico', '2026-09-12 12:00:00+00'::timestamptz, 'Horizonte', 'Paula', NULL, 'Envío de ficha técnica del salón Ceibo.', 'Documento recibido.', 'vendedor@ztech.local'),
    ('Nota interna', '2026-09-13 17:00:00+00'::timestamptz, 'Nexo Logística', NULL, NULL, 'Revisar disponibilidad de técnico de sonido.', 'Pendiente de coordinación.', 'gerente@ztech.local')
) AS activity(type_name, occurred_at, company_name, contact_first_name, opportunity_title,
              description, result, author_email)
JOIN activity_types activity_type ON activity_type.tenant_id = tenant.id AND activity_type.name = activity.type_name
LEFT JOIN companies company ON company.tenant_id = tenant.id AND company.business_name = activity.company_name
LEFT JOIN contacts contact ON contact.tenant_id = tenant.id AND contact.first_name = activity.contact_first_name
LEFT JOIN opportunities opportunity ON opportunity.tenant_id = tenant.id AND opportunity.title = activity.opportunity_title
JOIN users author ON author.tenant_id = tenant.id AND author.email = activity.author_email
WHERE tenant.name = 'zTech Eventos Corporativos';
