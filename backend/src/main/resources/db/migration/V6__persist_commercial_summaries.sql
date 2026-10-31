CREATE TABLE commercial_summaries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    opportunity_id BIGINT NOT NULL REFERENCES opportunities(id),
    context_fingerprint VARCHAR(64) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    response_json TEXT NOT NULL,
    UNIQUE (tenant_id, opportunity_id)
);
