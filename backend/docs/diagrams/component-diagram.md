# Diagrama de componentes

Modelo efectivo al 10/10/2026, revisado en §11. Módulos del monolito; dependencias
consumen contratos de service y los puertos invierten las consultas de uso comercial.

```mermaid
flowchart TB
    shared["shared: seguridad JWT, tenant, errores, validación, auditoría y PageResponse"]
    tenancy["tenancy: Tenant y serialización de configuración"]
    access["access: login, clave temporal, usuarios, roles e identidades históricas"]
    customers["customers: empresas, contactos, responsables y alcance"]
    offerings["offerings: ABM ADMIN de salones y servicios; capacidad"]
    catalogs["catalogs: embudos, etapas, plantillas y cuatro catálogos"]
    opportunities["opportunities: alta/edición, tablero, asignación, cierres, reapertura, disponibilidad e historial"]
    activities["activities: POST append-only e históricos paginados por asociación"]
    visibility["CustomerVisibilityPort"]
    reservations["VenueReservationPort"]
    usage["StageUsagePort"]

    access --> tenancy
    customers --> access
    customers --> catalogs
    offerings --> tenancy
    catalogs --> tenancy
    opportunities --> customers
    opportunities --> offerings
    opportunities --> catalogs
    opportunities --> access
    activities --> opportunities
    activities --> customers
    activities --> catalogs
    activities --> access
    customers --- visibility
    offerings --- reservations
    catalogs --- usage
    opportunities -. implementa .-> visibility
    opportunities -. implementa .-> reservations
    opportunities -. implementa .-> usage
    activities -.-> shared
    opportunities -.-> shared
    customers -.-> shared
    catalogs -.-> shared
    offerings -.-> shared
    access -.-> shared
```

- Las flechas llenas apuntan a service público, sin acceso a repositorios ajenos.
  Los puertos pertenecen al módulo que necesita la consulta; opportunities los implementa
  con su propio repositorio. No introducen ciclos de dependencias.
- activities consume OpportunityContext y CustomerActivityContext, records de service.
  No recibe la entidad Opportunity ni DTOs internos. El rol compartido de access
  conserva la excepción transversal documentada en las convenciones.
- Referencias entre módulos por ID; relaciones JPA sólo dentro del módulo propietario.
  Tenant y actor proceden de la sesión; cada lectura/escritura valida el alcance.
- Flyway V1–V5 gobierna el esquema. GiST protege reservas concurrentes; V4 conserva
  snapshots de cierres y V5 indexa los históricos. No hay edición/borrado de hechos.
