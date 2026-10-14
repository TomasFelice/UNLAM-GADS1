# Secuencia: cambio de etapa (BE-OPP-05)

Ilustra por qué el cambio de etapa no es un `PUT` genérico: coordina dos escrituras
(`Opportunity` y `StageHistory`) en una única transacción, con reglas de negocio antes
de tocar la base.

```mermaid
sequenceDiagram
    actor Cliente
    participant OC as OpportunityController
    participant CSS as ChangeStageService
    participant OR as OpportunityRepository
    participant SS as StageService
    participant SHR as StageHistoryRepository
    participant DB as PostgreSQL

    Cliente->>OC: POST /opportunities/{id}/stage {stageId, note, version}
    OC->>CSS: changeStage(id, request)

    CSS->>OR: findByIdAndTenantId(id, tenantId)
    OR->>DB: SELECT ...
    DB-->>OR: fila o vacío
    OR-->>CSS: Optional<Opportunity>

    alt no existe / otro tenant
        CSS-->>OC: NotFoundException
        OC-->>Cliente: 404
    else existe

        alt status != ABIERTA
            CSS-->>OC: ConflictException
            OC-->>Cliente: 409
        else abierta

            alt stageId destino == etapa actual
                CSS-->>OC: BusinessException (SAME_STAGE)
                OC-->>Cliente: 422
            else etapa distinta

                CSS->>SS: assertKindAndActive(stageId, pipelineId, OPEN)
                SS->>DB: SELECT ... FROM stages WHERE id = ? AND tenant_id = ?
                DB-->>SS: fila o vacío

                alt no existe
                    SS-->>CSS: NotFoundException
                    CSS-->>OC: (propaga)
                    OC-->>Cliente: 404
                else tipo incompatible o inactiva
                    Note over SS: Cubre también el intento de<br/>saltar directo a WON/LOST —<br/>eso son /win y /lose (Fase 7)
                    SS-->>CSS: ConflictException (tipo/embudo) o BusinessException (inactiva)
                    CSS-->>OC: (propaga)
                    OC-->>Cliente: 409 o 422
                else etapa OPEN y activa
                    SS-->>CSS: StageInfo

                    rect rgb(240, 248, 255)
                        Note over CSS,DB: Transacción única (@Transactional)
                        CSS->>OR: opportunity.changeStage(toStageId)
                        CSS->>OR: save(opportunity)
                        OR->>DB: UPDATE opportunities SET stage_id, version WHERE version leída

                        CSS->>SHR: save(new StageHistory(tenant, opportunityId,<br/>fromStageId, toStageId, changedBy, note))
                        SHR->>DB: INSERT INTO stage_history (...)

                        alt el INSERT del historial falla
                            DB-->>SHR: excepción
                            SHR-->>CSS: (propaga sin catch)
                            Note over CSS,DB: Spring revierte TODA la transacción:<br/>el UPDATE de stage_id también se deshace.
                            CSS-->>OC: excepción
                            OC-->>Cliente: 409/500 según el caso
                        else insert OK
                            DB-->>SHR: OK
                        end
                    end

                    CSS-->>OC: OpportunityResponse con nueva versión
                    OC-->>Cliente: 200 OK
                end
            end
        end
    end
```

La versión esperada se compara antes de modificar; flush comprueba @Version.
El destino debe pertenecer al embudo fijo. /stage sólo admite OPEN y no delega cierres.
