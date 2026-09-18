# Backend — convenciones técnicas

Aplica a `backend/`. Complementa `AGENTS.md` para cualquier asistente. Las decisiones
vigentes se registran en `03-decisiones-tecnicas.md` y los ADR; consultar la precedencia
de `AGENTS.md` si un documento conserva una propuesta anterior.

## Stack

| Área | Elección |
|---|---|
| Lenguaje | Java 25 (LTS) |
| Framework | Spring Boot 4.x (Spring Framework 7, Security 7, Hibernate 7) |
| Persistencia | Spring Data JPA + PostgreSQL |
| Migraciones | Flyway |
| Seguridad | Spring Security + JWT propio |
| Base de datos | Supabase (PostgreSQL administrado) |
| Deploy | Render, imagen Docker |
| Tests | JUnit 5, Mockito, Spring Boot Test, **Testcontainers** |
| Observabilidad | Spring Boot Actuator (`/actuator/health` para el health check de Render) |

## Estructura de paquetes

Base: `com.ztech.crm`. **Módulo de negocio al primer nivel, capas adentro.**

```text
backend/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/ztech/crm/
    │   │   ├── CrmApplication.java
    │   │   ├── shared/
    │   │   │   ├── config/       # CorsConfig, OpenApiConfig, JacksonConfig, JpaAuditingConfig
    │   │   │   ├── security/     # SecurityConfig, JwtService, JwtAuthenticationFilter,
    │   │   │   │                 # CustomUserDetailsService, CurrentUser, TenantContext
    │   │   │   ├── exception/    # BusinessException, NotFoundException, ConflictException,
    │   │   │   │                 # GlobalExceptionHandler (@RestControllerAdvice)
    │   │   │   ├── validation/   # anotaciones y validadores propios (CUIT, rango de fechas…)
    │   │   │   ├── audit/        # AuditableEntity (createdAt/By, updatedAt/By), AuditorAware
    │   │   │   └── dto/          # PageResponse<T>, ApiErrorResponse — sólo lo transversal
    │   │   ├── tenancy/          # Tenant
    │   │   ├── access/           # User, Role, login/refresh, gestión de usuarios
    │   │   ├── customers/        # Company, Contact
    │   │   ├── offerings/        # Venue (salón) + EventService (catering, audio, decoración)
    │   │   ├── catalogs/         # Stage, ActivityType, Origin, LossReason
    │   │   ├── opportunities/    # Opportunity, StageHistory, reserva del salón
    │   │   └── activities/       # Activity
    │   └── resources/
    │       ├── application.yml           # config base
    │       ├── application-local.yml     # perfil local
    │       ├── application-prod.yml      # perfil Render/Supabase
    │       └── db/migration/             # V1__initial_schema.sql (esquema + semilla)
    └── test/
        └── java/com/ztech/crm/   # espeja la estructura de main
```

Cada módulo de negocio, adentro:

```text
<modulo>/
├── controller/
├── service/
├── domain/
│   └── enums/
├── dto/
│   ├── request/
│   └── response/
├── repository/
│   └── specification/
└── mapper/
```

Un módulo crea sólo las carpetas que necesita: `tenancy/` no va a tener `controller/`.

### Dónde va cada cosa (referencia rápida)

| Cosa | Dónde |
|---|---|
| Endpoint HTTP | `<modulo>/controller/` |
| Caso de uso, transacción, regla que cruza entidades | `<modulo>/service/` |
| Entidad JPA, invariantes de la entidad | `<modulo>/domain/` |
| Enum de dominio (`OpportunityStatus`, `PartyStatus`) | `<modulo>/domain/enums/` |
| Objeto de entrada del endpoint | `<modulo>/dto/request/` |
| Objeto de salida del endpoint | `<modulo>/dto/response/` |
| Interfaz Spring Data, `@Query` | `<modulo>/repository/` |
| Filtro dinámico / búsqueda combinada | `<modulo>/repository/specification/` |
| Conversión entidad ↔ DTO | `<modulo>/mapper/` |
| `Role` (lo usa todo el mundo) | `access/domain/enums/` |
| `@Configuration`, CORS, OpenAPI, auditoría JPA | `shared/config/` |
| Todo lo de JWT, tenant y usuario logueado | `shared/security/` |
| Excepción propia y `@RestControllerAdvice` | `shared/exception/` |
| Validador Bean Validation propio | `shared/validation/` |
| `PageResponse<T>`, formato de error | `shared/dto/` |
| Script de esquema o de datos semilla | `resources/db/migration/` |

**No se crea un paquete `util/`** salvo necesidad concreta y justificada: tiende a ser un
cajón de sastre. Un helper vive junto a quien lo usa.

`shared/` es sólo para lo **realmente transversal**: configuración, seguridad, errores y
auditoría. Si algo pertenece a un módulo, va en el módulo aunque lo use otro.

## Reglas de capas

1. `controller` sólo traduce HTTP ↔ DTO y delega en `service`. Sin lógica de negocio,
   sin acceso a `repository`.
2. `service` es el único que abre transacciones (`@Transactional`), aplica autorización de
   rol y de alcance, y orquesta repositorios.
3. `domain` no conoce DTOs ni Spring MVC.
4. `repository` no contiene reglas de negocio.
5. Una entidad JPA **nunca** sale de un controller ni entra por él.

## Reglas entre módulos

1. Un módulo consume **sólo el `service`** de otro módulo. Nunca su `repository`, su
   `domain` ni sus DTOs internos.
2. Sin dependencias circulares. Dirección permitida:
   `activities → opportunities → {customers, offerings, catalogs} → access → tenancy`.
3. `opportunities` coordina clientes, salones, catálogos y usuarios, pero no toca sus
   repositorios.
4. Si dos módulos necesitan la misma entidad, no se duplica: uno la posee y el otro la
   referencia por id a través del `service` del dueño.

## Multi-tenancy

- Toda tabla de negocio lleva `tenant_id NOT NULL` con FK a `tenants`.
- El `tenantId` sale del **JWT del usuario autenticado**, nunca de un parámetro del request.
- Se expone por `TenantContext` (`shared/security/`), poblado por el filtro JWT.
- Todo método de repositorio filtra por tenant: `findByIdAndTenantId`, `findAllByTenantId`.
- Los índices y claves únicas locales a una organización incluyen `tenant_id`
  (p. ej. `UNIQUE (tenant_id, cuit)`, no `UNIQUE (cuit)`).
- Tests negativos obligatorios: acceso cruzado por listado **y** por id directo.

## Convenciones de nombres

- Código, entidades, tablas y columnas: **inglés**. Mensajes al usuario: **español**.
- Tablas en plural `snake_case`: `companies`, `contacts`, `opportunities`, `stage_history`.
- Columnas `snake_case`: `created_at`, `sales_rep_id`, `estimated_value`, `tenant_id`.
- DTOs: `CreateCompanyRequest`, `CompanyResponse`, `CompanyDetailResponse`.
- Servicios: `CompanyService`. Controllers: `CompanyController`.

## Convenciones de API

- Prefijo `/api/v1`.
- Recursos en plural: `/api/v1/companies`, `/api/v1/opportunities`.
- Operaciones que no son CRUD se modelan como sub-recurso explícito, **no** como un PUT
  genérico: `POST /api/v1/opportunities/{id}/stage`, `/win`, `/lose`, `/assign`.
- Códigos: `200` OK · `201` creado (con `Location`) · `204` sin contenido ·
  `400` validación · `401` sin autenticar · `403` sin permiso · `404` no existe ·
  `409` conflicto (salón ocupado, transición inválida, unicidad) · `422` regla de negocio.
- Errores con formato uniforme (ver DT-08). Nunca stacktrace ni datos internos.
- Paginación: `?page=0&size=20&sort=campo,asc`.

## Testing

Lo aplicado de verdad hasta Entrega 1 (12/09) — dos estrategias, no cuatro suites
separadas; detalle y motivo en `docs/specs-backend/design.md` §13:

- **Servicios con lógica genuina** (coordinan más de una escritura, o tienen una regla
  que vale la pena aislar): JUnit + Mockito, sin Spring ni Testcontainers. Corre con
  `mvn test`. Ejemplo real: `ChangeStageServiceTest`.
- **De punta a punta (`*IT`)**: HTTP real → service real → **Testcontainers** con
  PostgreSQL real, migraciones desde base vacía en cada corrida. Cubre contrato,
  validación, códigos, tenant-aware queries y transacciones de una sola vez. Corre con
  `mvn verify` (Failsafe) — hace falta `maven-failsafe-plugin` en el `pom.xml`, Surefire
  no recoge `*IT.java` por defecto.
- No se usó `@WebMvcTest`/`MockMvc` con el service mockeado, ni tests de dominio
  aislados: se prefirió entrar siempre por HTTP real contra infraestructura real.
- Casos negativos obligatorios: rol insuficiente (desde que exista `@PreAuthorize`,
  Fase 6), **acceso a otro tenant** (pendiente — hace falta un segundo tenant de
  prueba), cambio de etapa inválido (cubierto), reserva superpuesta y capacidad
  excedida (Fase 8, todavía no implementadas).
- Concurrencia: dos confirmaciones simultáneas del mismo salón y rango → exactamente una
  tiene éxito, la otra recibe `409` — Fase 8, todavía no implementado.

## Gotchas conocidos de la infra

- **Supabase + Render**: la conexión directa a Postgres de Supabase es IPv6; Render no
  siempre resuelve IPv6 saliente. Usar el **pooler (Supavisor)**. En modo *transaction*
  (6543) hay que **desactivar prepared statements** en el driver/Hibernate; en modo
  *session* no hace falta. Ver DT-07.
- **Render free tier**: el servicio se duerme por inactividad → primer request lento.
  Tenerlo en cuenta en la demo.
- **Flyway sobre Supabase**: trabajar sobre el schema `public` y no tocar los schemas
  internos de Supabase (`auth`, `storage`, etc.).
- La restricción de exclusión GiST necesita la extensión `btree_gist` habilitada
  (`CREATE EXTENSION IF NOT EXISTS btree_gist;` en la migración).
- **Testcontainers requiere Docker Desktop corriendo**. Si no está, los tests de
  integración fallan al arrancar; documentarlo en el README del backend.
