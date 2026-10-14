-- DP-03: representación canónica, sin modificar identidades ni inventar verificadores.
-- La transacción de Flyway revierte todo si hay colisiones por tenant.
LOCK TABLE companies IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    collision RECORD;
BEGIN
    WITH normalized AS (
        SELECT id, tenant_id,
               NULLIF(replace(regexp_replace(cuit, '^[[:space:]]+|[[:space:]]+$', '', 'g'), '-', ''), '') AS cuit
          FROM companies
    )
    SELECT tenant_id, array_agg(id ORDER BY id) AS company_ids
      INTO collision
      FROM normalized
     WHERE cuit IS NOT NULL
     GROUP BY tenant_id, cuit
    HAVING count(*) > 1
     ORDER BY tenant_id, min(id)
     LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'CUIT equivalentes: tenant %, empresas %. Corregir los datos antes de reintentar; no se fusionaron registros.',
            collision.tenant_id, collision.company_ids USING ERRCODE = '23505';
    END IF;
END $$;

WITH normalized AS (
    SELECT id, NULLIF(replace(regexp_replace(cuit, '^[[:space:]]+|[[:space:]]+$', '', 'g'), '-', ''), '') AS cuit
      FROM companies
)
UPDATE companies AS company
   SET cuit = normalized.cuit
  FROM normalized
 WHERE company.id = normalized.id AND company.cuit IS DISTINCT FROM normalized.cuit;
