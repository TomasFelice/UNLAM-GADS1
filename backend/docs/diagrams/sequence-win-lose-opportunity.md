# Secuencia: cierre y reapertura (BE-OPP-07/08/09)

```mermaid
sequenceDiagram
    actor Persona
    participant API as OpportunityController
    participant Ciclo as OpportunityLifecycleService
    participant Catalogos as Services de catalogs
    participant Disponibilidad as OpportunityAvailabilityService
    participant DB as PostgreSQL
    Persona->>API: POST /win o /lose (version, etapa, valor o motivo, nota)
    API->>Ciclo: Request validado
    Ciclo->>DB: Cargar oportunidad por tenant y alcance
    Ciclo->>Ciclo: Abierta y versión esperada
    Ciclo->>Catalogos: Etapa activa del embudo fijo y clase WON/LOST
    alt Ganar
        Ciclo->>Disponibilidad: Revalidar salón, capacidad y rango
        Disponibilidad->>DB: Bloqueo compartido del salón y precheck
    else Perder
        Ciclo->>Catalogos: Motivo activo del tenant y nombre actual
    end
    Note over Ciclo,DB: Una transacción
    Ciclo->>DB: UPDATE estado, etapa, cierre servidor y versión
    Ciclo->>DB: INSERT snapshot append-only del cierre
    alt Todo correcto
        DB-->>API: Commit
        API-->>Persona: 200 con nueva versión
    else Conflicto o fallo histórico
        DB-->>Ciclo: Rollback de ambas escrituras
        API-->>Persona: 409 de conflicto o error sin datos parciales
    end
    Persona->>API: POST /reopen (version, etapa OPEN, nota)
    API->>Ciclo: Verificar ADMIN/SALES_MANAGER, cerrada y versión
    Ciclo->>Catalogos: Embudo activo y etapa OPEN activa propia
    Ciclo->>DB: UPDATE a ABIERTA y limpiar datos de cierre
    Ciclo->>DB: INSERT REOPENED con nota y actor
    Note over Ciclo,DB: Commit libera reserva; snapshots anteriores intactos
    API-->>Persona: 200 con nueva versión
```

V4 distingue LEGACY sin snapshot de los eventos nuevos completos. PUT y /stage
no cierran ni reabren. /assign sólo modifica responsable/auditoría de una abierta.
