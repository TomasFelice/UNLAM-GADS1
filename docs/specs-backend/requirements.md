# Backend — Requisitos

Deriva de `docs/consignas/*.pdf`, `docs/context/01-producto-y-consignas.md` y las
decisiones registradas en `docs/decisiones/`. Cada requisito tiene un ID propio
(`BE-<módulo>-##`) porque no contamos con el ERS `.docx` con la numeración `RF-XX`
original; cuando ese documento exista, se puede mapear 1 a 1.

Prioridad: **E1** (24/09) · **EF** (entrega final, 12/11) · **POST-E1** (después de E1,
antes de la final).

---

## BE-SEC — Seguridad y tenancy (transversal)

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-SEC-01 | Login con email + password devuelve un JWT | E1 | `POST /api/v1/auth/login` con credenciales válidas devuelve `200` con `accessToken`; credenciales inválidas devuelven `401` sin distinguir "usuario no existe" de "password incorrecta" |
| BE-SEC-02 | Todo endpoint protegido exige JWT válido | E1 | Request sin token o con token vencido/alterado devuelve `401` |
| BE-SEC-03 | El tenant se resuelve del JWT, nunca del request | E1 | Un `tenant_id` enviado por query/body/header es ignorado; se usa el del token |
| BE-SEC-04 | Toda lectura y escritura se filtra por tenant | E1 | Un usuario del tenant A no puede leer ni modificar (por listado ni por id directo) un recurso del tenant B — `404`, no `403`, para no confirmar existencia |
| BE-SEC-05 | Autorización por rol (`ADMIN`, `SELLER`, `SALES_MANAGER`) | EF | Un endpoint restringido a `ADMIN` devuelve `403` para los otros roles |
| BE-SEC-06 | Autorización por alcance del `SELLER` | POST-E1 | Edita sólo clientes asignados directamente; puede leer además clientes relacionados con oportunidades propias; sus oportunidades siempre quedan asignadas a sí mismo (DP-02) |
| BE-SEC-07 | Passwords con hash seguro | E1 | Se almacenan con BCrypt; nunca en texto plano ni cifrado reversible; no aparecen en logs ni en respuestas |
| BE-SEC-08 | Sin endpoint público de alta de organización ni de usuario | E1 | El tenant y el `ADMIN` semilla se crean por migración Flyway; el alta de usuarios adicionales requiere sesión `ADMIN` (BE-ACC-02, EF) |
| BE-SEC-09 | Auditoría mínima en cambios importantes | E1 | Toda entidad auditable registra `created_at`, `created_by`, `updated_at`, `updated_by`, tomados del contexto autenticado, nunca del cliente |
| BE-SEC-10 | Errores no filtran información | E1 | Ninguna respuesta de error incluye stacktrace, datos de otro tenant, ni distingue credenciales inválidas de usuario inexistente |

Validación de BE-SEC-10 en §10 (10/10/2026): errores HTTP y logs correlacionados
por `requestId`, sin cuerpos, credenciales, SQL ni datos ajenos. Actor/tenant sólo
en MDC de solicitudes autenticadas; no se incluyen en ProblemDetail y se limpian
al finalizar. `ObservabilityIT` verifica 400/401/403/404/409/422 por HTTP real.

## BE-ACC — Usuarios y acceso

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-ACC-01 | Al menos un usuario habilitado para iniciar sesión | E1 | Usuario semilla creado por migración, con rol `ADMIN`, permite login inmediato tras el deploy |
| BE-ACC-02 | `ADMIN` crea y modifica usuarios, asigna rol | EF | `POST /api/v1/users`, `PUT /api/v1/users/{id}` sólo accesibles por `ADMIN`; el rol es uno de los tres válidos |
| BE-ACC-03 | Listado y detalle de usuarios | EF | `GET /api/v1/users`, `GET /api/v1/users/{id}`, paginados, sólo `ADMIN` |
| BE-ACC-04 | Baja lógica de usuario | EF | Un usuario dado de baja no puede loguearse pero conserva su autoría en historiales |
| BE-ACC-05 | Contraseña temporal y revocación | EF | Alta/reset exige contraseña temporal; mientras `mustChangePassword` sea verdadero sólo se admite cambiarla; los cambios sensibles incrementan `auth_version` e invalidan JWT anteriores |

## BE-CUS — Empresas y contactos

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-CUS-01 | Alta de empresa | E1 | `POST /api/v1/companies`; razón social, nombre comercial, estado y responsable obligatorios; CUIT opcional con dígito válido, canónico sin guiones y único por tenant; email y sitio HTTP/HTTPS por formato, blancos opcionales a `null` (DP-03) |
| BE-CUS-02 | Edición de empresa | E1 | `PUT /api/v1/companies/{id}` actualiza los campos editables |
| BE-CUS-03 | Listado y detalle de empresa | E1 | `GET /api/v1/companies` paginado; `GET /api/v1/companies/{id}` incluye sus contactos |
| BE-CUS-04 | Alta de contacto | E1 | `POST /api/v1/contacts`; `firstName`, `lastName`, `status` obligatorios; `companyId` **opcional** (cliente individual) |
| BE-CUS-05 | Edición de contacto | E1 | `PUT /api/v1/contacts/{id}` |
| BE-CUS-06 | Listado y detalle de contacto | E1 | `GET /api/v1/contacts` paginado; `GET /api/v1/contacts/{id}` |
| BE-CUS-07 | Relacionar contacto con empresa | E1 | Un contacto se crea o edita con `companyId`; se puede desvincular (pasa a cliente individual) |
| BE-CUS-08 | Baja lógica por cambio de estado | POST-E1 | Cambiar `status` a `INACTIVO`/`NO_CONTACTAR` no borra el registro ni rompe sus oportunidades/actividades históricas; no existe `DELETE` físico |
| BE-CUS-09 | Búsqueda y filtros de empresas/contactos | POST-E1 | Filtro por nombre, estado, origen, responsable; ver DP-10 para el detalle exacto |

Clientes implementados al 09/10/2026: origen seleccionable en alta/edición de empresa,
nombres de responsables en listado de empresas y detalle de ambos clientes, sin
ampliar asignables del vendedor. Las nuevas referencias a origen exigen tenant y
actividad; puede conservarse la referencia histórica inactiva. La UI permite
edición sólo a asignados directos para SELLER, conforme DP-02; el backend conserva
la autorización efectiva. La baja por estado mantiene relaciones e historial.

## BE-OFF — Salones y servicios (producto/servicio de la consigna)

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-OFF-01 | Salones precargados | E1 | Al menos 2 `Venue` cargados por migración semilla, consultables por `GET /api/v1/venues` |
| BE-OFF-02 | ABM completo de salones | EF | `POST`/`PUT`/`GET` de `Venue`; capacidad, localidad, descripción, equipamiento y estado `DISPONIBLE`/`MANTENIMIENTO`/`INACTIVO`, sólo por `ADMIN` (DP-04) |
| BE-OFF-03 | Servicios adicionales precargados | E1 | Al menos algunos `EventService` cargados por migración (catering, audio, decoración) |
| BE-OFF-04 | ABM completo de servicios | EF | `POST`/`PUT`/`GET` de `EventService` |
| BE-OFF-05 | Desactivar oferta no rompe oportunidades existentes | EF | Salón `MANTENIMIENTO`/`INACTIVO` o servicio `active=false` conserva asociaciones anteriores; impide nuevas. Editar otros campos permite conservarlas, pero ganar exige salón disponible y capacidad suficiente |

Implementado al 09/10/2026: POST/PUT/GET de oferta, baja lógica ADMIN y lecturas
comerciales paginadas por tenant. Tamaño 20/máximo 100, búsqueda `q`, filtro
`status`/`active` y orden permitido. Conservar asociaciones anteriores está cubierto
por HTTP; omitir `serviceIds` en edición conserva y `[]` quita. Reducir capacidad
por debajo de una reserva ganada devuelve 409. Confirmación ganada y su revalidación
se implementaron en el corte de cierres (§8): confirmación revalida salón y capacidad.

## BE-CAT — Catálogos configurables

Ampliación aceptada el 09/10/2026: [ADR-006](../decisiones/adr/ADR-006-multiples-embudos.md).
Los criterios de múltiples embudos están implementados en §6; su evidencia se sigue
en `openspec/changes/archive/2026-10-13-completar-crm-entrega-final/tasks.md` §6.

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-CAT-01 | Etapas precargadas | E1 | Las 7 etapas de DP-06 cargadas por migración, consultables por `GET /api/v1/stages` |
| BE-CAT-02 | ABM de etapas | EF | `ADMIN` crea/edita/reordena etapas propias de un embudo; orden único por embudo y reordenamiento completo atómico; tipo inmutable si está en uso; baja lógica sin perder historia ni dejar un embudo activo incompleto |
| BE-CAT-03 | ABM de tipos de actividad | EF | Catálogo `ActivityType` con al menos los tipos mínimos de la consigna |
| BE-CAT-04 | ABM de orígenes comerciales | EF | Catálogo `Origin`; nuevas asociaciones activas y conservación de referencias previas inactivas |
| BE-CAT-05 | ABM de motivos de pérdida | EF | Catálogo `LossReason`, disponible para el caso de uso de cierre perdido |
| BE-CAT-06 | ABM de tipos de evento | EF | Catálogo `EventType`; toda oportunidad referencia uno activo al crearla |
| BE-CAT-07 | Múltiples embudos por tenant | EF | `ADMIN` administra embudos con un predeterminado activo único; activar exige etapas activas `OPEN`/`WON`/`LOST`; baja lógica, consulta histórica y plantillas copiadas independientes; migración desde V1 conserva IDs e historial (ADR-006) |

## BE-OPP — Oportunidades y embudo

Para EF (ADR-006), alta, detalle y filtros incorporan `pipelineId`; todas las etapas
deben pertenecer al embudo de la oportunidad, fijo tras el alta. `PUT` rechaza un
cambio de embudo. El tablero usa el predeterminado si se omite el filtro y devuelve
identidad del embudo, `kind` y `active` de sus columnas, incluidas las históricas
necesarias. Crear/reabrir exige embudo activo; las abiertas de un embudo inactivo
pueden operarse con sus etapas activas. La exclusión de reservas sigue siendo por
tenant/salón, independientemente del embudo. §6 implementa migración, configuración,
altas, filtros y tablero; el ciclo de cierre/reapertura se implementó en §8.

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-OPP-01 | Alta de oportunidad | E1 | `POST /api/v1/opportunities`: título, cliente, responsable heredado/reemplazable según rol, embudo fijo y etapa inicial abierta, salón, tipo de evento, `eventStart`, `eventEnd`, asistentes y `serviceIds`; contacto deriva empresa y rechaza contradicciones; fechas reales obligatorias |
| BE-OPP-02 | Edición de oportunidad abierta | E1 | `PUT /api/v1/opportunities/{id}` mientras `status = ABIERTA` |
| BE-OPP-03 | Listado y detalle de oportunidad | E1 | `GET /api/v1/opportunities` paginado; `GET /api/v1/opportunities/{id}` con etapa actual, salón, cliente, responsable |
| BE-OPP-04 | Asignar/reasignar responsable | POST-E1 | `POST /api/v1/opportunities/{id}/assign`; reasignación libre para `ADMIN`/`SALES_MANAGER` |
| BE-OPP-05 | Cambio de etapa persistido | E1 | `POST /api/v1/opportunities/{id}/stage`: valida que la nueva etapa sea `OPEN` y compatible con `status = ABIERTA`; actualiza `Opportunity` **y** agrega `StageHistory` en una sola transacción |
| BE-OPP-06 | Vista de embudo agrupada por etapa | E1 | `GET /api/v1/opportunities/board` devuelve oportunidades agrupadas por `stageId`, filtradas por tenant (y por alcance del `SELLER` cuando aplique) |
| BE-OPP-07 | Marcar como ganada | POST-E1 | `POST /api/v1/opportunities/{id}/win`: exige etapa `WON`, registra `closedAt` y `finalValue`; transacción única con `StageHistory` |
| BE-OPP-08 | Marcar como perdida | POST-E1 | `POST /api/v1/opportunities/{id}/lose`: exige `lossReasonId`, registra `closedAt`; transacción única con `StageHistory` |
| BE-OPP-09 | Oportunidad cerrada no se modifica sin autorización | POST-E1 | `PUT`/`stage` sobre una oportunidad `GANADA`/`PERDIDA` devuelve `409`; sólo un caso de uso explícito de reapertura (DP-01) lo permite, y ese cambio queda en el historial |
| BE-OPP-10 | Validación de capacidad del salón | EF | `attendeeCount` no puede superar `venue.capacity`; violación → `422`; superar 50 sólo agrega una advertencia de UI (DP-08) |
| BE-OPP-11 | Reserva sin solapamiento | EF | Una oportunidad `GANADA` no puede compartir salón y rango `[eventStart,eventEnd)` con otra `GANADA` del tenant; horarios contiguos se admiten; protección de aplicación y GiST → `409` (DP-07) |
| BE-OPP-12 | Búsqueda y filtros de oportunidades | POST-E1 | Filtro por responsable, etapa, estado, origen (mínimo exigido por la consigna); ver DP-10 |

## BE-ACT — Actividades e historial comercial

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-ACT-01 | Registrar actividad | POST-E1 | `POST /api/v1/activities`: `typeId` activo, `occurredAt` no futuro, relacionada a empresa **o** contacto **u** oportunidad, `description` y `result` no vacíos; asociaciones derivadas/coherentes, usuario/tenant/fecha de registro del servidor; append-only |
| BE-ACT-02 | Historial cronológico por empresa/contacto/oportunidad | POST-E1 | `GET /api/v1/companies/{id}/activities`, análogos para contact y opportunity, paginados y ordenados por `occurredAt desc, id desc`, navegables íntegramente con bajas y autores inactivos; alcance por tenant/vendedor |
| BE-ACT-03 | Historial de cambios de etapa consultable | POST-E1 | `GET /api/v1/opportunities/{id}/stage-history` devuelve todos los cambios, append-only, sin edición ni borrado posible vía API |
| BE-ACT-04 | El historial reconstruye el recorrido completo | POST-E1 | Para una oportunidad se puede responder: etapa inicial, etapas atravesadas, cuándo cambió cada una, quién lo hizo, cuándo y cómo cerró |

## BE-DOC — Documentación y artefactos de entrega

| ID | Requisito | Prioridad | Criterio de aceptación |
|---|---|---|---|
| BE-DOC-01 | Anotaciones Swagger en cada controller | Continuo (no se pospone) | Todo endpoint tiene `@Tag`/`@Operation`/`@ApiResponse`; se agrega en el mismo PR que crea el endpoint, no al final |
| BE-DOC-02 | Archivo OpenAPI estático | EF | `backend/docs/openapi.yaml` exportado y actualizado, coherente con `/v3/api-docs` en runtime |
| BE-DOC-03 | Diagrama de componentes | EF | `backend/docs/diagrams/component-diagram.md` en Mermaid, refleja los módulos y capas reales del código |
| BE-DOC-04 | Diagramas de secuencia de flujos críticos | EF | Al menos login, cambio de etapa, ganar/perder oportunidad y reserva concurrente, en Mermaid, en `backend/docs/diagrams/` |
| BE-DOC-05 | Colección de Postman | EF | `backend/docs/postman/zTech-CRM.postman_collection.json` importable, cubre todos los endpoints, con environment y auto-seteo de token tras login |
| BE-DOC-06 | Docker Compose para levantar en local | EF | `backend/docker-compose.yml` levanta la API contra Supabase vía `.env`; documentado en `backend/docs/docker.md` |
| BE-DOC-07 | README del backend | EF | `backend/README.md` permite a alguien sin contexto previo clonar, configurar y levantar el proyecto en local (con y sin Docker), correr los tests y encontrar toda la documentación anterior |

## Fuera de alcance (no implementar)

Gestión de tareas y agenda · recordatorios/notificaciones · indicadores y estadísticas ·
exportación · integraciones con terceros · API pública · importación automática ·
facturación, pagos y contabilidad · gestión de stock · campañas de marketing · envío de
mails desde el sistema · integración con WhatsApp. (Multi-tenancy es la única excepción
deliberada — ver ADR-003.)

## Trazabilidad con decisiones abiertas

Las decisiones de esta tabla están resueltas en
`docs/decisiones/decisiones-pendientes.md`; se conserva la trazabilidad porque sus
resoluciones forman parte del criterio de aceptación.

| DP | Bloquea |
|---|---|
| DP-01 | BE-OPP-09 (reapertura) |
| DP-02 | BE-SEC-06 |
| DP-03 | BE-CUS-01, BE-CUS-04 (formatos y obligatoriedad exactos) |
| DP-04 | BE-OFF-02 |
| DP-05 | BE-ACT-01 (fecha de ocurrencia vs. registro) |
| DP-07 | BE-OPP-11 |
| DP-08 | BE-OPP-10 |
| DP-10 | BE-CUS-09, BE-OPP-12 |

### Estado implementado del ciclo comercial (§8)

Assign/win/lose/reopen y PUT/stage exigen versión leída obligatoria; creación no.
Conflictos tardíos/JPA → 409 sin sobrescritura. Asignación sólo elevada y abierta;
reapertura sólo elevada, nota y embudo activo, limpia cierre actual y libera reserva.
Ganar/perder usan clases WON/LOST activas del embudo fijo, fecha del servidor y
snapshot append-only. Motivo conserva nombre original; V4 marca legacy incompleto
sin inventar datos. Disponibilidad consultable y revalidada, GiST entre embudos y
rollback ante fallo histórico; prueba HTTP concurrente después del precheck.
La interfaz conserva valores ante 409 y ofrece recarga; los cierres muestran centavos.
§9 implementa alta append-only con asociaciones derivadas, fecha no futura y actor
del servidor, alcance por tenant/vendedor y navegación íntegra del historial comercial.
V5 agrega índices de cronología; las bajas lógicas conservan hechos y autores.
