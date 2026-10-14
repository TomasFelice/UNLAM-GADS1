# Secuencia: reserva concurrente (BE-OPP-11)

```mermaid
sequenceDiagram
    participant A as Confirmación A
    participant B as Confirmación B
    participant DB as PostgreSQL
    A->>DB: Cargar oportunidad A y validar versión
    B->>DB: Cargar oportunidad B y validar versión
    A->>DB: Bloqueo compartido salón; capacidad y precheck libres
    B->>DB: Bloqueo compartido mismo salón; precheck libre
    Note over A,B: El IT coloca aquí una barrera de dos requests HTTP
    par Confirmaciones
        A->>DB: UPDATE a GANADA, flush (GiST)
    and
        B->>DB: UPDATE a GANADA, flush (GiST)
    end
    DB-->>A: Una transacción puede continuar
    A->>DB: INSERT snapshot WON; commit
    DB-->>B: Exclusión 23P01 o aborto por competencia de bloqueos
    Note over B,DB: Rollback: oportunidad abierta y sin cierre histórico
    B-->>B: Advice traduce a 409 recuperable
    Note over A,DB: Exactamente una ganada y un historial de cierre
```

La exclusión se aplica por tenant, salón y `[inicio, fin)`, con independencia del
embudo. Permite rangos contiguos y otro salón/tenant. El precheck mejora el mensaje;
GiST sigue protegiendo la carrera. La edición de capacidad toma bloqueo exclusivo
del salón, incompatible con una confirmación en curso.

Exclusión → VENUE_ALREADY_BOOKED; aborto concurrente de bloqueo →
CONCURRENT_WRITE_CONFLICT. Se conserva el formulario y se exige recargar. No se
reintenta una escritura automáticamente. Una carrera sobre la misma oportunidad
se resuelve además con @Version y devuelve OPPORTUNITY_VERSION_CONFLICT.
