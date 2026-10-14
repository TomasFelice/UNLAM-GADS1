-- ADR-006: cada proceso comercial tiene etapas propias. No cambia IDs ni reservas.
CREATE TABLE pipelines (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    name TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT REFERENCES users(id),
    CONSTRAINT uq_pipelines_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_pipelines_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_pipelines_default_active CHECK (NOT is_default OR active)
);
CREATE UNIQUE INDEX uq_pipelines_default ON pipelines(tenant_id) WHERE is_default;

INSERT INTO pipelines(tenant_id, name, active, is_default)
SELECT id, 'Eventos corporativos', TRUE, TRUE FROM tenants;

ALTER TABLE stages ADD COLUMN pipeline_id BIGINT;
UPDATE stages s SET pipeline_id = p.id FROM pipelines p WHERE p.tenant_id = s.tenant_id;
ALTER TABLE stages ALTER COLUMN pipeline_id SET NOT NULL;
ALTER TABLE stages DROP CONSTRAINT uq_stages_tenant_position;
ALTER TABLE stages ADD CONSTRAINT uq_stages_pipeline_position
    UNIQUE (tenant_id, pipeline_id, position) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE stages ADD CONSTRAINT uq_stages_tenant_pipeline_id UNIQUE (tenant_id, pipeline_id, id);
ALTER TABLE stages ADD CONSTRAINT fk_stages_pipeline
    FOREIGN KEY (tenant_id, pipeline_id) REFERENCES pipelines(tenant_id, id);

ALTER TABLE opportunities ADD COLUMN pipeline_id BIGINT;
UPDATE opportunities o SET pipeline_id = s.pipeline_id
FROM stages s WHERE s.id = o.stage_id AND s.tenant_id = o.tenant_id;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM opportunities WHERE pipeline_id IS NULL) THEN
        RAISE EXCEPTION 'No se puede migrar: hay oportunidades cuya etapa no pertenece a su tenant.';
    END IF;
END $$;
ALTER TABLE opportunities ALTER COLUMN pipeline_id SET NOT NULL;
ALTER TABLE opportunities ADD CONSTRAINT fk_opportunities_pipeline
    FOREIGN KEY (tenant_id, pipeline_id) REFERENCES pipelines(tenant_id, id);
ALTER TABLE opportunities ADD CONSTRAINT fk_opportunities_pipeline_stage
    FOREIGN KEY (tenant_id, pipeline_id, stage_id) REFERENCES stages(tenant_id, pipeline_id, id);
CREATE INDEX ix_opportunities_pipeline_status ON opportunities(tenant_id, pipeline_id, status);

-- Los tenants sin etapas reciben sólo la configuración inicial de DP-06.
INSERT INTO stages(tenant_id, pipeline_id, name, position, kind)
SELECT p.tenant_id, p.id, s.name, s.position, s.kind
FROM pipelines p CROSS JOIN (VALUES
    ('Consulta recibida', 1, 'OPEN'), ('Necesidad relevada', 2, 'OPEN'),
    ('Visita al salón', 3, 'OPEN'), ('Propuesta enviada', 4, 'OPEN'),
    ('Negociación', 5, 'OPEN'), ('Ganada / Reservado', 6, 'WON'), ('Perdida', 7, 'LOST')
) AS s(name, position, kind)
WHERE NOT EXISTS (SELECT 1 FROM stages existing WHERE existing.pipeline_id = p.id);

-- Una configuración parcial existente necesita corrección explícita, no etapas inventadas.
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM pipelines p
        WHERE EXISTS (
            SELECT 1 FROM (VALUES ('OPEN'), ('WON'), ('LOST')) AS required(kind)
            WHERE NOT EXISTS (SELECT 1 FROM stages s WHERE s.pipeline_id = p.id AND s.active AND s.kind = required.kind)
        )
    ) THEN
        RAISE EXCEPTION 'No se puede migrar un embudo inicial incompleto: se requieren etapas activas OPEN, WON y LOST.';
    END IF;
END $$;
