# zTech CRM — Backend

API del CRM de salones de eventos corporativos (UNLaM GADS1). Este README refleja el
estado implementado al **10/10/2026**: acceso, clientes, oferta, catálogos y embudos
configurables, oportunidades, cambio de etapa, cierres, reapertura, reservas y
registro append-only de actividades con históricos paginados. Se actualiza en cada corte — ver la sección [Estado](#estado-y-alcance)
al final para el detalle de qué está implementado y qué no.

Contexto funcional y arquitectónico completo: [`docs/`](../docs/README.md) en la raíz
del repositorio. Decisiones técnicas del backend: [`docs/context/`](../docs/context/)
y [`docs/specs-backend/`](../docs/specs-backend/), compartidas por todos los asistentes.

Regresión local y límites de publicación:
[validación de §11](../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-punto-11-2026-10-10.md).

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Java 25 (LTS) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate 7) |
| Persistencia | PostgreSQL (Supabase) + Spring Data JPA + Flyway |
| Seguridad | Spring Security + JWT propio (HS512) |
| Build | Maven (con wrapper — no hace falta tener Maven instalado) |
| Documentación de API | springdoc-openapi 3.1.1 (Swagger UI + OpenAPI 3.1) |
| Tests | JUnit 5, Mockito, Testcontainers |

## Requisitos previos

- **JDK 25**, y tiene que ser el que se usa en la terminal — ver la nota de abajo si
  hay más de un JDK instalado. El proyecto usa el wrapper de Maven (`./mvnw`), así que
  no hace falta instalar Maven aparte.
  - Windows: `winget install --id EclipseAdoptium.Temurin.25.JDK -e`
  - Mac/Linux: [adoptium.net](https://adoptium.net/) o el gestor de paquetes del sistema.
  - Verificar: `java -version` tiene que mostrar `"25...`.
- **Docker Desktop instalado y corriendo** (no alcanza con tenerlo instalado nada más).
  Hace falta para los tests de integración (Testcontainers levanta su propio PostgreSQL
  efímero) y, opcionalmente, para correr la app con `docker compose`.
- Acceso a un proyecto de **Supabase** (PostgreSQL administrado). No se usa Supabase
  Auth ni RLS — sólo la base de datos. Sin credenciales propias, pedirle al equipo el
  connection string del proyecto compartido.

> ⚠️ **Si tenés más de un JDK instalado** (muy común), `java -version` y `./mvnw` pueden
> estar usando uno viejo sin que sea obvio. Sintoma típico:
> `UnsupportedClassVersionError: ... class file version 69.0 ... only recognizes ... up
> to 61.0`. Solución: setear `JAVA_HOME` apuntando al JDK 25 antes de correr nada —
> `run.ps1`/`run.sh` (ver abajo) lo detectan y avisan en vez de fallar con ese error
> críptico.

## Variables de entorno

Copiar [`.env.example`](.env.example) a `.env` y completar. **Nunca commitear `.env`**
(ya está en `.gitignore`).

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | JDBC URL de PostgreSQL. Ver la nota de conexión más abajo. |
| `DB_USERNAME` | Sí | Usuario de conexión (formato distinto según directo vs. pooler). |
| `DB_PASSWORD` | Sí | Password de la base. |
| `DB_POOL_SIZE` | No (default 10) | Tamaño del pool de Hikari. |
| `JWT_SECRET` | Sí | Secreto para firmar los JWT. Generar uno largo y aleatorio, p. ej. `openssl rand -base64 48`. |
| `JWT_EXPIRATION_MINUTES` | No (default 480) | Vigencia del access token, en minutos. |
| `CORS_ALLOWED_ORIGINS` | No (default `http://localhost:5173`) | Orígenes permitidos, separados por coma. |
| `PORT` | No (default 8080) | Puerto HTTP. Render lo inyecta automáticamente en producción. |

### Conexión a Supabase: directa vs. pooler

- **Local, sin Docker** (`./mvnw spring-boot:run`): la conexión **directa** funciona si
  tu red tiene IPv6 real (verificado el 12/09/2026 desde una conexión hogareña
  argentina). Formato: `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`,
  usuario `postgres`.
- **Local, con Docker Compose** o **en Render**: usar el **pooler Supavisor en modo
  session** — Docker Desktop y Render no tienen salida IPv6. Se obtiene en el dashboard
  de Supabase: *Settings → Database → Connection pooling → Session mode*. Formato:
  `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres`, usuario
  `postgres.<project-ref>`.

Ver también [`docs/docker.md`](docs/docker.md) y
[ADR-002](../docs/decisiones/adr/ADR-002-plataforma-supabase-render.md).

## Cómo levantar en local

### Sin Docker

```powershell
# Windows
.\run.ps1
```
```bash
# Mac/Linux/Git Bash
./run.sh
```

`run.ps1`/`run.sh` hacen tres cosas antes de arrancar: **(1)** verifican que el JDK
activo sea la versión 25 y cortan con un mensaje claro si no — no el
`UnsupportedClassVersionError` críptico; **(2)** cargan `backend/.env` al proceso —
**esto es necesario**: a diferencia de `docker compose`, un `./mvnw spring-boot:run`
directo **no lee `.env` por su cuenta**, Spring Boot no tiene ese comportamiento
integrado; **(3)** corren `./mvnw spring-boot:run` con el perfil `local`.

Si preferís no usar el script (por ejemplo, ya tenés las variables exportadas en tu
shell de otra forma), `./mvnw spring-boot:run` a secas también funciona — pero entonces
sos vos quien tiene que exportar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`
antes, no alcanza con que `.env` exista en el directorio.

`run.ps1`/`run.sh` fuerzan el perfil `local` (logging de desarrollo sin SQL) si no está seteado
`SPRING_PROFILES_ACTIVE`. Un `./mvnw spring-boot:run` a secas, sin el script, **no** usa
ese perfil por su cuenta — corre con el perfil `default` de Spring (sin las variantes de
`application-local.yml`).

La primera vez que la app apunta a una base vacía, Flyway crea el esquema completo y
carga los datos semilla automáticamente (tenant, usuario admin, 7 etapas, 3 salones,
servicios, orígenes, motivos de pérdida y tipos de actividad — ver
`src/main/resources/db/migration/`).

### Con Docker

```powershell
docker compose up --build
```

Ver el detalle en [`docs/docker.md`](docs/docker.md).

### Usuario para probar

| Email | Password | Rol |
|---|---|---|
| `admin@ztech.local` | `Admin123!` | `ADMIN` |
| `vendedor@ztech.local` | `Admin123!` | `SELLER` |
| `gerente@ztech.local` | `Admin123!` | `SALES_MANAGER` |

Son usuarios **semilla para desarrollo y demo**, creados por
`V1__initial_schema.sql` — no son credenciales de
producción y exigen cambio de contraseña al ingresar.

## Cómo correr los tests

```powershell
./mvnw test      # unitarios (JUnit + Mockito, rápidos, sin Docker)
./mvnw verify     # unitarios + integración (Testcontainers — requiere Docker Desktop activo)
```

Los tests de integración (sufijo `*IT`, corren con Failsafe en la fase `verify`, no con
Surefire en `test` — es la convención estándar de Maven) levantan su propio PostgreSQL
efímero con Testcontainers y corren las migraciones de Flyway desde una base vacía en
cada ejecución. No dependen de Supabase ni de `.env`.

La suite usa PostgreSQL 16 de Testcontainers y las migraciones V1/V2 desde base
vacía, además de actualización V1→V2 con datos existentes. Incluye verificaciones
del esquema, datos semilla, exclusión GiST, filtros, actividades e historial.

El 17/09/2026 se consolidaron V1–V4 en `V1__initial_schema.sql` tras el reinicio de la
base autorizado por el usuario. Requiere una base vacía, incluido el historial
`flyway_schema_history`; no es una actualización de bases con las versiones anteriores.
Los cambios posteriores se incorporan en nuevas migraciones desde V2.

`V2__normalize_company_cuit.sql` normaliza CUIT sin guiones y blancos a `null`.
Detecta equivalentes repetidos por tenant antes de escribir y revierte la migración
con IDs de empresas para corregir los datos; no fusiona ni elimina registros.
Los dígitos incorrectos existentes se conservan: la API exige corregirlos u omitir
el CUIT opcional al guardar. No se recalcula una identidad fiscal automáticamente.
Altas y ediciones validan el dígito y la unicidad canónica, email y sitio HTTP/HTTPS;
normalizan blancos opcionales de empresas/contactos antes de validar.

## Documentación de la API

### Diagnóstico de solicitudes y carga

Las solicitudes autenticadas registran método, ruta sin query, estado y duración,
con `requestId`/`userId`/`tenantId` en el MDC. Errores controlados registran sólo
estado/código; los inesperados, la clase de excepción. No se registran cuerpos,
cabeceras, contraseñas, tokens ni parámetros de búsqueda. Los tres identificadores
se limpian al finalizar; una solicitud anónima no hereda actor/tenant.
`X-Request-Id` permite correlacionar un `ProblemDetail` sin exponer actor/tenant
en el error. SQL, binds y diagnósticos JDBC de Hibernate están deshabilitados
también en el perfil local; no habilitarlos para capturas compartidas.

El listado/tablero de oportunidades resuelve nombres y servicios en lotes por
IDs autorizados, con mapas por request. `OpportunityLoadIT` comprueba el conteo
con 20/100 filas y aislamiento; `ObservabilityIT` captura logs reales de errores
400/401/403/404/409/422 y comprueba correlación y ausencia de datos sensibles.
La instrumentación de carga existe sólo en test: ver los comandos de la
[línea base](../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/evidencias/linea-base-carga.md).

### Contrato

- **Swagger UI** (con la app corriendo en local): http://localhost:8080/swagger-ui.html
- **OpenAPI estático**: [`docs/openapi.yaml`](docs/openapi.yaml) — exportado desde
  `/v3/api-docs.yaml`; se regenera cada vez que cambia el contrato.
- **Colección de Postman**: [`docs/postman/`](docs/postman/) — importar
  `zTech-CRM.postman_collection.json` y el environment `zTech-CRM-Local.postman_environment.json`.
  Completar `baseUrl`, `email` y `password` de un ADMIN de prueba que ya haya
  cambiado su contraseña temporal. Correr primero **Login** de *Auth*: autosetea
  `{{accessToken}}`; las carpetas siguientes capturan IDs/versiones. Usar datos
  de prueba: la colección crea registros, los edita y ejecuta bajas lógicas.
  El E2E con Newman valida las 69 operaciones runtime sin ajustar IDs manualmente;
  preparación en [README frontend](../frontend/README.md#pruebas-integradas-locales).
- **Diagramas**: [`docs/diagrams/`](docs/diagrams/) — componentes y secuencias
  implementadas de login, cambio de etapa, cierre/reapertura y reserva concurrente.

## Estructura de paquetes

Módulo de negocio al primer nivel, capas adentro — detalle completo en
[`docs/context/02-backend-convenciones.md`](../docs/context/02-backend-convenciones.md)
y [ADR-001](../docs/decisiones/adr/ADR-001-estructura-de-paquetes.md).

```text
com.ztech.crm/
├── shared/{config,security,exception,audit,validation}/
├── tenancy/          # Tenant
├── access/           # User, Role, login, contraseña y administración de usuarios
├── customers/        # Company, Contact
├── offerings/        # Venue, EventService, ABM ADMIN y consulta comercial paginada
├── catalogs/         # Etapas, tipos de evento/actividad, orígenes y motivos
├── opportunities/    # Opportunity, StageHistory, cambio de etapa, tablero
└── activities/       # Lectura del historial comercial
```

## Estado y alcance

**Implementado:**

- Login con JWT (`POST /api/v1/auth/login`).
- ABM de empresas y contactos, con relación opcional entre ambos.
- Origen comercial validado por tenant y actividad al escribir clientes; las
  referencias históricas inactivas pueden conservarse. Listado de empresas y
  detalles de empresa/contacto incluyen `salesRepName`, también para responsables
  inactivos, sin ampliar `/users/assignable` del vendedor.
- ABM de salones y servicios adicionales por ADMIN, con baja lógica y consulta
  comercial para los tres roles. Listados paginados con `q`, `status` en salones o
  `active` en servicios, tamaño 20/máximo 100 y orden validado.
- Oferta histórica conservada al editar oportunidades; nuevas asociaciones exigen
  disponibilidad. Omitir `serviceIds` al editar conserva los actuales; `[]` los quita.
- Reducción de capacidad rechazada con 409 si invalida una reserva ganada; las
  oportunidades abiertas se revalidan al editar. Puerto `VenueReservationPort` sin
  acceso a repositorios ajenos. Al ganar se revalidan salón y capacidad bajo bloqueo compartido.
- ABM ADMIN de orígenes, motivos de pérdida y tipos de actividad/evento: `GET/POST`,
  detalle `GET` y edición/baja lógica `PUT /{id}`. Lecturas para los tres roles,
  nombre no vacío y único por catálogo/tenant. Listados con `q`, `active`, página
  de 20/máximo 100 y orden permitido `id`, `name`, `active` (estable por ID).
- Referencias de catálogos nuevas activas; clientes/oportunidades pueden conservar
  las inactivas anteriores. IDs ajenos devuelven 404, nuevas inactivas 422.
  `CatalogEntryInfo` es el contrato de consulta entre módulos. Los cierres consumen
  el motivo y el registro de actividades valida el tipo activo.
- ABM ADMIN de embudos y etapas propias, orden completo atómico y baja lógica.
  Predeterminado activo único, activación con clases OPEN/WON/LOST y copias
  independientes de plantillas corporativa/breve. V3 conserva IDs e historial.
- Oportunidad con embudo fijo; `pipelineId` filtra listado/tablero. Omitido en alta
  o tablero usa predeterminado. Columnas por tipo y actividad, incluidas históricas.
  Embudo inactivo no permite altas; abiertas existentes pueden seguir operándose.
- ABM de oportunidades (título, empresa/contacto, responsable, salón, etapa inicial,
  fecha del evento, cantidad de asistentes).
- Cambio de etapa con historial atómico (`POST /{id}/stage`).
- Tablero de oportunidades agrupadas por etapa (`GET /opportunities/board`).
- Multi-tenancy (ADR-003): toda entidad de negocio filtra por el tenant del usuario
  autenticado.
- Administración de usuarios por `ADMIN`, contraseña temporal, cambio obligatorio y
  revocación de JWT anteriores con `auth_version`.
- Empresas con razón social/nombre comercial/localidad y responsables obligatorios.
- Salones con estado, localidad, descripción y equipamiento; tipos de evento.
- Oportunidades con rango horario, tipo de evento, servicios asociados, control de
  capacidad y restricción GiST de reservas ganadas.
- Filtros DP-10, paginación acotada y orden validado en clientes y oportunidades.
  Búsqueda de contactos con unión izquierda para incluir individuales; filtro
  companyId aplicado en servidor. OpportunityCreationIT verifica contexto, herencia,
  contradicciones, tenant, fechas reales y capacidad (60 permitido dentro de 80).
- Registro y lectura paginada de actividades por empresa, contacto y oportunidad,
  con autor y tipo resueltos, orden estable por fecha/ID y alcance comercial.
- Lectura del historial de etapas; el alta registra también la etapa inicial.

**Cierre de OpenSpec (13/10/2026)**: el usuario aprobó el testing integral local y
autorizó archivar con §11.8 pendiente. Ver el
[cierre](../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/cierre-2026-10-13.md).
Al cierre del 13/10 no se había realizado el despliegue. El 19/10/2026 se publicó
`81454a8` en Render y Flyway aplicó V2–V5 en Supabase. El
[registro de producción](../docs/despliegue-produccion-2026-10-19.md) distingue
las comprobaciones remotas del testing local y de la demostración autenticada pendiente.

### Ciclo comercial y conflictos

POST `/opportunities/{id}/assign|win|lose|reopen`: versión leída obligatoria también
en PUT/stage. Assign/reopen sólo ADMIN/SALES_MANAGER; cierre por alcance comercial.
Ganada exige valor final ≥0 y etapa WON; perdida, motivo activo y etapa LOST.
Reapertura exige nota, embudo activo y etapa OPEN. Cierres y auditoría se guardan
atómicamente, con fecha del servidor. GET `/{id}/availability` orienta; ganar revalida.
V4 conserva historial anterior como LEGACY sin snapshots inventados y agrega datos
de futuros cierres. IDs/nombres de motivo y valor sobreviven a reaperturas/renombres.
GiST de V1 no cambia. Exclusión → 409 VENUE_ALREADY_BOOKED; versión → 409
OPPORTUNITY_VERSION_CONFLICT; aborto concurrente de bloqueo → 409
CONCURRENT_WRITE_CONFLICT. No se registran SQL/claves del error desde el advice.
Recargar antes de reintentar; no se hacen reintentos automáticos de escrituras.
Postman incluye carpeta Ciclo comercial y reservas, captura IDs/versiones y crea
un embudo independiente. Diagramas en docs/diagrams describen transacciones reales.

### Actividades e historial comercial

`POST /api/v1/activities` recibe `typeId`, `occurredAt` (instante no futuro),
`description`, `result` y al menos `companyId`, `contactId` u `opportunityId`.
Tipo activo obligatorio; descripción y resultado no vacíos. Actor, tenant y fecha
de registro proceden del servidor. Una oportunidad deriva sus clientes y un contacto
deriva su empresa; asociaciones explícitas contradictorias reciben `422`.

SELLER registra en clientes asignados directamente o en oportunidades propias,
incluso cerradas; la consulta indirecta de un cliente no autoriza escritura general.
No se filtran las actividades de oportunidades ajenas por compartir cliente.
Las bajas lógicas conservan hechos y autores; no hay PUT/DELETE de actividades.

GET `/activities` y `/{companies|contacts|opportunities}/{id}/activities` aceptan
`page`, `size` (20, máximo 100) y `sort` validado. Orden habitual:
`occurredAt desc, id desc`. `occurredAt` identifica el hecho, `createdAt` el registro.
V5 agrega índices por relación y cronología. La colección Postman incorpora
**Registro de actividades e historial**, con referencias capturadas de la API.
`ActivityRegistrationIT` cubre asociaciones, validación, alcance, páginas con empates,
bajas y autor inactivo. La UI ofrece alta contextual y navegación completa.

## Resumen comercial con IA

La función está apagada por defecto y no participa del health check. Sin clave el
CRM inicia normalmente. Ver [configuración, límites y evaluación](docs/resumen-comercial-ia.md).
Sólo usar Gemini Free Tier comprobado en AI Studio, con una clave nueva fuera del
repositorio. Reemplazar cualquier credencial expuesta previamente; no reutilizarla.
No se activa facturación desde esta integración. Las variables son sólo backend;
ninguna clave debe tener prefijo VITE_ ni incorporarse al contrato o a fixtures.
