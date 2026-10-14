-- Los eventos previos conservan todos sus campos. No se infieren ciclos de cierre.
ALTER TABLE stage_history
    ADD COLUMN event_type varchar(24) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN snapshot_complete boolean NOT NULL DEFAULT false,
    ADD COLUMN destination_status varchar(20),
    ADD COLUMN final_value numeric(15,2),
    ADD COLUMN closed_at timestamptz,
    ADD COLUMN loss_reason_id bigint,
    ADD COLUMN loss_reason_name varchar(255),
    ADD CONSTRAINT ck_history_event_type CHECK (event_type IN
        ('LEGACY', 'CREATED', 'STAGE_CHANGED', 'WON', 'LOST', 'REOPENED')),
    ADD CONSTRAINT ck_history_snapshot CHECK (
        (event_type = 'LEGACY' AND NOT snapshot_complete AND destination_status IS NULL
            AND final_value IS NULL AND closed_at IS NULL AND loss_reason_id IS NULL AND loss_reason_name IS NULL)
        OR (event_type <> 'LEGACY' AND snapshot_complete AND destination_status IS NOT NULL AND (
            (event_type IN ('CREATED','STAGE_CHANGED','REOPENED') AND destination_status = 'ABIERTA'
                AND final_value IS NULL AND closed_at IS NULL AND loss_reason_id IS NULL AND loss_reason_name IS NULL)
            OR (event_type = 'WON' AND destination_status = 'GANADA' AND final_value IS NOT NULL
                AND final_value >= 0 AND closed_at IS NOT NULL AND loss_reason_id IS NULL AND loss_reason_name IS NULL)
            OR (event_type = 'LOST' AND destination_status = 'PERDIDA' AND final_value IS NULL
                AND closed_at IS NOT NULL AND loss_reason_id IS NOT NULL AND loss_reason_name IS NOT NULL)
        )));
CREATE INDEX idx_stage_history_chronology ON stage_history(tenant_id, opportunity_id, changed_at, id);
