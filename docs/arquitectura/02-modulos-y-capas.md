# Módulos y capas

## Límites del backend

El backend se divide por capacidades de negocio:

| Módulo | Responsabilidad | Entidades principales |
|---|---|---|
| `tenancy` | Organizaciones y aislamiento | `Tenant` |
| `access` | Login, usuarios, roles y autorización | `User`, roles |
| `customers` | Empresas, contactos y responsables | `Company`, `Contact` |
| `offerings` | Producto o servicio ofrecido: salones y servicios adicionales | `Venue`, `EventService` |
| `catalogs` | Catálogos configurables | `Stage`, `ActivityType`, `Origin`, `LossReason` |
| `opportunities` | Embudo, asignación, cierres, reserva e historial | `Opportunity`, `StageHistory` |
| `activities` | Interacciones e historial comercial | `Activity` |
| `assistance` | Resumen comercial autorizado, efímero y acotado; REST Gemini | Sin persistencia |

`opportunities` coordina referencias a clientes, salones, catálogos y usuarios, pero no debe manipular sus repositorios internos. Cada módulo expone operaciones a los demás **únicamente a través de su paquete `service`** ([ADR-001](../decisiones/adr/ADR-001-estructura-de-paquetes.md)).

```mermaid
flowchart LR
    access --> tenancy
    customers --> access
    offerings --> tenancy
    catalogs --> tenancy
    opportunities --> access
    opportunities --> customers
    opportunities --> offerings
    opportunities --> catalogs
    activities --> access
    activities --> customers
    activities --> opportunities
    activities --> catalogs
    assistance --> activities
    assistance --> opportunities
```

Las flechas van del consumidor a la API pública del módulo consumido; no representan acceso directo a implementación. Se deben evitar dependencias circulares.

`activities` recibe `OpportunityContext` y `CustomerActivityContext`, records en
`service`, para derivar asociaciones autorizadas sin consumir entidades ni DTOs
internos ajenos. La oportunidad propia permite registrar hechos aun cerrada; un
cliente visible sólo por relación no habilita el alta general. Las lecturas paginadas
no exponen actividades de oportunidades ajenas por compartir cliente (OpenSpec §9).

La visibilidad de clientes derivada de oportunidades aplica inversión de dependencias:
`customers` define `CustomerVisibilityPort` y `opportunities` lo implementa mediante
`OpportunityAccessService`. Así, empresas y contactos pueden consultar si un vendedor
tiene una oportunidad propia relacionada sin acceder a tablas del otro módulo ni crear
una dependencia `customers → opportunities`.

Oferta usa la misma inversión: `offerings/service/VenueReservationPort` consulta
si una reducción de capacidad invalidaría una reserva ganada. La implementación
`OpportunityVenueReservations` usa sólo su repositorio. `VenueInfo`, en `service`,
es el contrato de consulta entre oferta y oportunidades; el detalle HTTP se arma
con DTO propio de oportunidades. Las asociaciones anteriores pueden conservarse
inactivas; las nuevas requieren disponibilidad.

## Capas internas

```text
module/
├── controller/   # entrada HTTP: @RestController
├── service/      # casos de uso, límites transaccionales y contrato hacia otros módulos
├── domain/       # entidades y reglas de negocio (+ enums/)
├── dto/          # request/ y response/ del módulo
├── repository/   # Spring Data JPA (+ specification/)
└── mapper/       # conversión entidad ↔ DTO
```

Reglas de dependencia:

1. `controller` convierte HTTP en llamadas al `service`; no accede a `repository` ni contiene reglas.
2. `service` coordina permisos, reglas, servicios de otros módulos y transacciones.
3. `domain` no depende de Spring MVC, DTOs HTTP ni detalles de persistencia.
4. `repository` sólo persiste; no contiene reglas de negocio.
5. **Sólo `service` es estable para consumidores externos al módulo**; el resto es interno.
   Cuando el módulo de menor nivel necesita una respuesta del superior, define un puerto
   en su propio `service` y el módulo superior aporta la implementación.
6. `shared` se limita a configuración, errores, seguridad, validación y auditoría realmente transversales.

La nomenclatura de capas fue cambiada respecto de la versión original de este documento
(`api/application/domain/infrastructure/web`); el motivo y las alternativas descartadas
están en [ADR-001](../decisiones/adr/ADR-001-estructura-de-paquetes.md).

Un caso de uso se separa en su propia clase de `service` cuando coordina más de una
escritura con una regla de atomicidad real — no todo verbo lo amerita. Así quedó
aplicado: `ChangeStage` es `ChangeStageService` propio, porque escribe `Opportunity` y
`StageHistory` en la misma transacción y no puede modelarse como un `PUT` genérico. En
cambio, crear o editar una oportunidad son métodos de `OpportunityService`: validan
reglas reales (empresa o contacto, salón activo, etapa inicial abierta) pero escriben
una sola entidad, así que no ganan nada con una clase aparte. `ConfirmReservation` y
`LoseOpportunity` se resuelven en `OpportunityLifecycleService`, junto con
asignación y reapertura (OpenSpec §8): coordinan estado, auditoría e historial en
una transacción. `ActivityService` registra hechos autorizados sin mutar oportunidades.

## Organización del backend

```text
backend/src/main/java/com/ztech/crm/
├── shared/{config,security,exception,validation,audit,dto}/
├── tenancy/        # implementado (Fase 0-1)
├── access/         # implementado (login, cambio de clave y ABM de usuarios)
├── customers/      # implementado (Fase 2)
├── offerings/      # consulta paginada y ABM ADMIN implementados (09/10)
├── catalogs/       # ABM ADMIN de catálogos, embudos y etapas; lecturas comerciales
├── opportunities/  # implementado (Fase 3-5, incluido alcance comercial)
├── activities/     # registro append-only e históricos paginados — OpenSpec §9
└── assistance/     # resumen efímero desde servicios de lectura; ADR-007
```

Estado actualizado de oferta/catálogos/embudos al 09/10; detalle fase por fase en
`docs/specs-backend/tasks.md`.

Las migraciones viven en `backend/src/main/resources/db/migration/` con `V1__initial_schema.sql` como instalación consolidada de esquema y datos semilla. Los cambios posteriores se versionan desde V2.

## Organización del frontend

```text
frontend/src/
├── app/{router,providers,layouts}/
├── features/{acceso,usuarios,empresas,contactos,salones,servicios,catalogos,embudos,
│             embudo,oportunidades,actividades,clientes,panel,inicio,institucional}/
└── shared/{api,components,hooks,sesion,seo,styles,data,tema}/
```

Cada feature contiene páginas, componentes, validaciones, consultas y tipos propios. `shared` solo recibe piezas reutilizadas por varias features. TanStack Query administra estado remoto; el estado de formularios queda en React Hook Form y Zod valida entradas. Las restricciones mostradas en la interfaz mejoran la experiencia, pero nunca reemplazan la validación del backend.

## Pruebas por límite

La intención original era una pirámide de 4 capas separadas para el backend (dominio,
aplicación, infraestructura, web). En la práctica, hasta la Entrega 1, se aplicaron sólo
dos estrategias — ambas contra infraestructura real, ninguna con mocks de repositorio o
`@WebMvcTest` aislado:

- **Unitarios (Mockito, sin Spring ni Testcontainers)**: sólo para servicios con lógica
  de negocio genuina que vale la pena aislar — hoy, `ChangeStageServiceTest`, que prueba
  que una excepción en el insert del historial se propaga sin `catch` (lo que permite
  que `@Transactional` revierta también el cambio de etapa). Corren con `mvn test`
  (Surefire).
- **Integración de punta a punta (`*IT`, Testcontainers)**: un test por flujo relevante,
  entrando por HTTP real (login incluido) y llegando a un PostgreSQL real con las
  migraciones de Flyway aplicadas desde una base vacía. Cubren de una sola vez lo que la
  pirámide original separaba en "aplicación" + "infraestructura" + "web": contratos
  HTTP, validación, códigos de respuesta, tenant-aware queries y transacciones. Corren
  con `mvn verify` (Failsafe), no con `mvn test` — requieren Docker Desktop.
- No hay tests de "dominio" puro (una entidad sin repositorio ni service alrededor):
  las reglas de las entidades son chicas y quedan cubiertas indirectamente por los `*IT`.

El conteo vigente y el detalle de cada corte se mantienen en
`docs/specs-backend/tasks.md`.

- Frontend: comportamiento de features con Vitest y Testing Library.
- E2E: recorridos de entrega con Playwright.

## Resumen comercial IA — decisión 31/10/2026

[ADR-007](../decisiones/adr/ADR-007-resumen-comercial-ia.md) incorpora el coordinador
`assistance → {activities, opportunities}`, sin dependencias inversas. REST JDK,
resumen efímero y configuración apagada por defecto. Implementación y validaciones
se siguen en [las tareas del cambio](../../openspec/changes/incorporar-resumen-comercial-ia/tasks.md);
no implica despliegue ni validación real de Gemini.
