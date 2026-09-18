# Decisiones técnicas del backend (DT)

Complementa `docs/decisiones/decisiones-pendientes.md`, que registra las decisiones
**funcionales** (DP-01..DP-12). Acá van las **técnicas**. Estado: `Acordada` / `Abierta`.

---

## Acordadas

### DT-00 — Stack y plataforma · Acordada (12/09/2026)
Java 25 + Spring Boot 4.x · PostgreSQL en **Supabase** (reemplaza a Neon del doc de
arquitectura) · deploy de la API en **Render** vía Docker · frontend en carpeta aparte,
desarrollado en una sesión independiente del asistente que use el equipo.

### DT-01 — Multi-tenancy · Acordada (12/09/2026)
**Se mantiene completa**, aunque la consigna liste "soporte para múltiples organizaciones"
como fuera de alcance. Shared Database + Shared Schema: `tenant_id` en toda entidad de
negocio, resuelto desde el JWT, filtrado en cada consulta, con tests de acceso cruzado.
Se asume el costo a cambio de fidelidad al documento de arquitectura aprobado.

### DT-02 — Organización de paquetes · Acordada (12/09/2026)
**Módulo de negocio al primer nivel, capas adentro**:
`com.ztech.crm.<modulo>.{controller,service,domain,dto,repository,mapper}`.
Se conserva el monolito modular del doc de arquitectura, pero con los nombres de capa que
pidió el usuario en lugar de `api/application/domain/infrastructure/web`.
Lo transversal va en `com.ztech.crm.shared.{config,security,exception,validation,audit}`.
Detalle en `02-backend-convenciones.md`.

> El doc de arquitectura definía un paquete `api/` por módulo como contrato público. Se
> reemplaza por la regla: **un módulo consume sólo el `service` de otro módulo**.

### DT-03 — Producto o servicio · Acordada (12/09/2026)
Dos entidades separadas dentro del módulo `offerings/`:
- **`Venue`** (salón): capacidad, tarifa, estado. Es lo que se reserva y lo que tiene
  restricción de solapamiento.
- **`EventService`**: catering, audio, decoración, mobiliario. Ítem con precio, sin reserva.

Una oportunidad toma **un salón** (ver DP-09) y opcionalmente varios servicios.
Cumple el módulo "Gestión de productos o servicios" que exige la consigna.

### DT-10 — Testing de integración · Acordada (12/09/2026)
**Testcontainers** con PostgreSQL real. Cubre migraciones desde base vacía, consultas
tenant-aware, la restricción de exclusión GiST y la doble reserva concurrente.
Requiere Docker Desktop corriendo; se documenta en el README del backend.

### DP-06 (parcial) — Etapas del embudo · Etapas acordadas (12/09/2026)
Siete etapas, con **visita al salón** como etapa propia de la industria:

| # | Etapa | Tipo | `is_closed` |
|---|---|---|---|
| 1 | Consulta recibida | abierta | no |
| 2 | Necesidad relevada | abierta | no |
| 3 | Visita al salón | abierta | no |
| 4 | Propuesta enviada | abierta | no |
| 5 | Negociación | abierta | no |
| 6 | Ganada / Reservado | ganada | sí |
| 7 | Perdida | perdida | sí |

`Stage` es tabla configurable con `name`, `position`, `kind` (`OPEN`/`WON`/`LOST`) y
`active`. Se precargan en `V1__initial_schema.sql`. Regla: una oportunidad `ABIERTA` sólo
puede estar en una etapa `OPEN`; pasar a la etapa `WON`/`LOST` cierra la oportunidad.

### DP-09 — Salón y datos del evento · Acordada (12/09/2026)
**Un solo salón por oportunidad** (FK `venue_id`, no N:N). La V1 consolidada incluye
`event_start`/`event_end`, `attendee_count`, tipo de evento, servicios N:N y exclusión
GiST; el backend valida la capacidad. Ver DP-05/07/08 para su semántica.

### DT-04 a DT-22 — Bloque técnico · Acordadas (12/09/2026)
Aprobadas en bloque tal como estaban propuestas. Se listan abajo como referencia.

---

## Acordadas — bloque técnico

| ID | Tema | Decisión |
|---|---|---|
| DT-04 | Estrategia de IDs | `BIGINT GENERATED ALWAYS AS IDENTITY`. Simple y legible; la enumerabilidad no es un riesgo real en un TP. |
| DT-05 | Build tool | **Maven**. Más común en la cátedra y sale directo de start.spring.io. |
| DT-06 | `groupId` / paquete / `artifactId` | `com.ztech` / `com.ztech.crm` / `crm-backend` |
| DT-07 | Conexión a Supabase | **Pooler Supavisor en modo session** para **Render** y para **Docker** en cualquier entorno — confirmado empíricamente el 12/09: la conexión directa da `UnknownHostException` dentro de un contenedor Docker (red IPv4-only), y el pooler session (`aws-0-<región>.pooler.supabase.com:5432`, usuario `postgres.<project-ref>`) conecta y funciona completo (Flyway, login). Para desarrollo local **sin** Docker, la conexión **directa** funciona si la red del desarrollador tiene IPv6 real (verificado el 12/09 desde una conexión hogareña); si no lo tiene, usar el pooler también ahí. URL completa por variable de entorno en todos los casos. |
| DT-08 | Formato de error | **`ProblemDetail` (RFC 7807)**, nativo desde Spring 6. Campos extra: `code`, `errors[]` para validación, `requestId`. |
| DT-09 | Formato de paginación | **`PageResponse<T>` propio** (`content`, `page`, `size`, `totalElements`, `totalPages`). El JSON de `Page` de Spring es verboso e inestable entre versiones. |
| DT-11 | JWT | HS256, secreto por env var. **Sólo access token** (~8 h) para E1; refresh token después si sobra tiempo. Claims: `sub`, `userId`, `tenantId`, `role`, `exp`. |
| DT-12 | Hash de contraseñas | **BCrypt** (`BCryptPasswordEncoder`, cost 10). |
| DT-13 | Alta de usuarios y de tenants | **Sin endpoint público de registro.** Tenant + `ADMIN` semilla por migración Flyway; el resto de los usuarios los crea el `ADMIN` de su tenant. No hay alta de organizaciones por API. |
| DT-14 | Baja lógica | La consigna la define como **cambio de estado**: `status = INACTIVO` para Company/Contact. Booleano `active` sólo para catálogos, salones, servicios y usuarios. Sin `deleted_at`. |
| DT-15 | Fechas y horas | Persistir en UTC (`timestamptz` ↔ `Instant`/`OffsetDateTime`). Zona de negocio `America/Argentina/Buenos_Aires`. El tipo exacto del rango del evento depende de **DP-07**. |
| DT-16 | Moneda | Una sola moneda (ARS). `NUMERIC(15,2)` ↔ `BigDecimal`. Sin campo `currency`. |
| DT-17 | Datos semilla | **Migraciones Flyway** (`V1__initial_schema.sql`), no `CommandLineRunner`: reproducible y versionado. |
| DT-18 | Contrato con el frontend | **springdoc-openapi** activado, con anotaciones (`@Tag`, `@Operation`, `@ApiResponse`) en cada controller desde que se crea. El export estático vive en **`backend/docs/openapi.yaml`** (DT-23), no en `docs/api/` de la raíz — la instancia de frontend lo lee desde ahí o directo de `/v3/api-docs` en local. |
| DT-23 | Artefactos de documentación del backend | Todo vive en **`backend/docs/`** salvo el `README.md`, que queda en `backend/` (convención estándar). Detalle completo en `docs/specs-backend/design.md` §12: `openapi.yaml`, `diagrams/` (mermaid), `postman/` (colección + environment). |
| DT-24 | Docker en local | `backend/Dockerfile` construye la imagen de despliegue (ya definido, DT-21). Se agrega `backend/docker-compose.yml` para levantar esa misma imagen en local contra Supabase vía `.env`, espejando el ambiente de Render. Los tests de integración no usan compose: Testcontainers levanta su propio Postgres efímero por corrida. |
| DT-19 | CORS | `http://localhost:5173` (Vite) + dominio del frontend desplegado, por variable de entorno. |
| DT-20 | CI | GitHub Actions: build + tests + migraciones desde base vacía en cada PR. Deseable, no bloqueante para E1. |
| DT-21 | Ubicación del `Dockerfile` | `backend/Dockerfile` (Render apunta ahí). El doc de arquitectura proponía `infra/`; se simplifica. |
| DT-22 | Estrategia de ramas | `main` siempre desplegable; ramas `feature/*`; PRs chicos con el ID de requisito. |

---

## Decisiones funcionales relacionadas

DP-01 a DP-10 están resueltas y aceptadas; su formulación vigente y consecuencias se
mantienen en [`decisiones-pendientes.md`](../decisiones/decisiones-pendientes.md). En
particular, DP-02 ya está implementada mediante `CustomerVisibilityPort` y
`OpportunityAccessService`: `SELLER` ve clientes asignados o relacionados con sus
oportunidades, sólo edita los asignados directamente y nunca ve oportunidades ajenas.
DP-04 gobierna el siguiente corte de ABM administrativo. No quedan decisiones
funcionales abiertas que bloqueen ese trabajo.
