-- Orden estable y navegación por relación sin cargar historias completas.
CREATE INDEX idx_activities_company_chronology ON activities(tenant_id, company_id, occurred_at DESC, id DESC);
CREATE INDEX idx_activities_contact_chronology ON activities(tenant_id, contact_id, occurred_at DESC, id DESC);
CREATE INDEX idx_activities_opportunity_chronology ON activities(tenant_id, opportunity_id, occurred_at DESC, id DESC);
