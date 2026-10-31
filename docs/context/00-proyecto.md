# zTech CRM — Contexto del proyecto

TP de **Gestión Aplicada al Desarrollo de Software I** (UNLaM, Ing. en Informática).
CRM **especializado en salones de eventos corporativos**. Monorepo, monolito modular.

## Alcance de cada sesión

- `backend/`: API REST en Java + Spring Boot.
- `frontend/`: SPA React + TypeScript + Vite integrada con la API para las rutas privadas actuales.
- El usuario asigna el alcance de cada sesión, independientemente del asistente.
  Si hay trabajo paralelo, respetar las carpetas asignadas y coordinar cambios de contrato.
- El contrato compartido es **`backend/docs/openapi.yaml`**, exportado del backend.

## Documentación de referencia

| Qué | Dónde |
|---|---|
| Consignas originales (PDF) | `docs/consignas/` |
| Arquitectura general, módulos/capas, datos/seguridad | `docs/arquitectura/` |
| Plan de desarrollo e hitos | `docs/planificacion/plan-de-desarrollo.md` |
| Decisiones funcionales: resueltas y abiertas (DP-01..DP-12) | `docs/decisiones/decisiones-pendientes.md` |
| ADR — decisiones que cambian arquitectura | `docs/decisiones/adr/` |
| Instrucciones comunes para asistentes | `AGENTS.md` |
| **Spec del backend: requisitos, diseño y tareas** | `docs/specs-backend/` |
| Detalle de producto y reglas de negocio | `docs/context/01-producto-y-consignas.md` |
| Convenciones técnicas del backend | `docs/context/02-backend-convenciones.md` |
| Decisiones técnicas tomadas / abiertas | `docs/context/03-decisiones-tecnicas.md` |

**Precedencia ante conflicto:** ver `AGENTS.md`. Las decisiones técnicas y los ADR
registran las revisiones aprobadas respecto de las propuestas iniciales. La ERS citada
en la documentación no está en el repositorio; no asumir que se leyó.

## Stack del backend (acordado con el usuario)

- **Java 25 (LTS)** + **Spring Boot 4.x** (Spring Framework 7, Spring Security 7, Hibernate 7)
- **PostgreSQL en Supabase** (no Neon, como decía el doc de arquitectura)
- **Flyway** como única fuente de evolución del esquema — `ddl-auto` nunca en `update`
- **Spring Security + JWT** propio (NO se usa Supabase Auth; Supabase es sólo Postgres)
- **Deploy en Render** vía Docker
- API versionada bajo `/api/v1`, JSON sobre HTTPS

## Estructura de paquetes del backend

Base: `com.ztech.crm`. **Monolito modular**: módulo de negocio al primer nivel, capas
adentro. ADR-001 registra el cambio respecto de la propuesta inicial de capas
`api/application/domain/infrastructure/web`; la arquitectura ya incorpora esta decisión.

```text
backend/src/main/java/com/ztech/crm/
├── shared/{config,security,exception,validation,audit}/
├── tenancy/          # Tenant
├── access/           # User, Role, login
├── customers/        # Company, Contact
├── offerings/        # Venue (salón) y EventService — el "producto o servicio" de la consigna
├── catalogs/         # Stage, ActivityType, Origin, LossReason
├── opportunities/    # Opportunity, StageHistory, reserva del salón
├── activities/       # Activity — alta append-only e históricos paginados
└── assistance/       # resumen comercial efímero; ADR-007
```

Cada módulo de negocio tiene adentro:

```text
<modulo>/
├── controller/   # @RestController, sólo HTTP → service. Nunca lógica de negocio.
├── service/      # casos de uso, @Transactional, autorización de alcance
├── domain/       # entidades JPA + enums/ del módulo
├── dto/          # request/ y response/. NUNCA se expone una entidad JPA.
├── repository/   # Spring Data JPA + specification/ para filtros
└── mapper/       # entidad ↔ DTO
```

**Regla de límites entre módulos:** un módulo sólo consume el `service` de otro módulo.
Nunca su `repository`, su `domain` ni sus DTOs internos. Sin dependencias circulares.
Dirección permitida: `activities → opportunities → {customers, offerings, catalogs} → access → tenancy`.

Migraciones: `backend/src/main/resources/db/migration/V1__initial_schema.sql`, etc.

## Estado del backend y cómo retomar

**Cambio archivado el 13/10/2026 (apply iniciado el 09/10):** el cambio
[`completar-crm-entrega-final`](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/tasks.md)
conserva el registro de entrega final; sus nueve specs están sincronizadas. ADR-006 ya tiene migración V3, API y pantallas
de múltiples embudos y etapas configurables. Se incorpora administración de usuarios en el
frontend sobre el backend existente, sólo para `ADMIN`. La evidencia actual se
registra en la [validación del apply](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-2026-10-09.md);
los resultados fechados de septiembre siguientes siguen siendo antecedentes.
El corte de clientes incorpora CUIT con dígito válido y unicidad canónica, formato
de email/sitio y blancos opcionales. V2 normaliza CUIT existentes sin inventar
verificadores ni fusionar colisiones. Los formularios incluyen origen y responsables
por nombre, incluso inactivos. SELLER consulta relacionados por oportunidad propia y
edita sólo asignados directos, tanto en acciones visibles como en URL de formulario.
El backend valida tenant y actividad del origen y permite conservar la referencia
inactiva anterior. La baja por estado preserva oportunidades, actividades y autoría,
comprobada por HTTP/SQL. La pausa anterior cerró §3–§4. La continuación incorpora §5: ABM ADMIN de los cuatro
catálogos, lecturas paginadas y selectores por búsqueda/ID; referencias históricas
conservadas en clientes/oportunidades. §6 agrega embudos propios con predeterminado,
plantillas, orden atómico, etapas históricas y pipeline fijo de oportunidad.
§7 incorpora selección remota de clientes y alta breve contextual con datos
obligatorios del evento, sin inventar fechas. §8 implementa asignación, cierre,
reapertura, versión esperada y reservas concurrentes con snapshots V4; el registro
de actividades y la navegación histórica quedaron completados en §9. Ver el
[punto de retomada](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/retomada.md)
y la [validación de §9 del 10/10](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-2026-10-10.md)
para la evidencia reciente. La continuación del 10/10 cerró también §10: carga
por ruta, panel acotado, lecturas por lotes, caché por sesión y errores/logs
correlacionados. La [validación de §10](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-punto-10-2026-10-10.md)
y la [comparación de carga](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/evidencias/comparacion-carga-punto-10.md)
registran 5 unitarias/108 IT, 45 frontend, 21 E2E y 48 muestras válidas.
La continuación de §11 completó 11.1–11.7: recorrido integrado por interfaz, rutas
con tres roles, regresión EF/PDF/reglas e instalación/artefactos. Estado: 71/72 tareas;
5 unitarias/109 IT, 45 frontend y 24 E2E correctos.
La [validación de §11](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-punto-11-2026-10-10.md)
registra los resultados de esa sesión. El usuario confirmó testing integral local
satisfactorio el 13/10 y autorizó archivar con §11.8 pendiente; ver el
[cierre](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/cierre-2026-10-13.md).
No se certifica el ambiente remoto desde pruebas locales.

**Producción — 19/10/2026:** se publicó `81454a8` en Render y Vercel, con
Flyway V2–V5 aplicado en Supabase. API con salud `UP`, contrato de 69 operaciones,
CORS y rechazo anónimo comprobados; páginas públicas e ingreso revisados a 390 px.
La demostración comercial autenticada remota sigue pendiente. Ver el
[registro del despliegue](../despliegue-produccion-2026-10-19.md) para destinos,
evidencia y límites; las notas de septiembre siguientes conservan su estado histórico.

`requirements.md`/`design.md`/`tasks.md` (`docs/specs-backend/`) ya fueron aprobados
por el usuario e implementación arrancó — no son un borrador esperando luz verde.

**Estado registrado el 14/09/2026**: Fases 0-4 hechas y estabilización post-E1 en curso.
El esquema incorpora seguridad de contraseña temporal, responsables obligatorios, modelo
ampliado de empresas/salones/oportunidades, tipos de evento, servicios asociados y la
restricción GiST y un seed comercial coherente. Desde el reinicio del 17/09/2026,
todo se instala con la migración consolidada V1. El backend ya aplica el alcance
comercial de `SELLER` a clientes, oportunidades, tablero y actividades mediante puertos
entre módulos; expone filtros DP-10, catálogos de lectura e historial de etapas. El
frontend autentica contra la API, conserva sesión, fuerza el cambio inicial de clave e
integra todas las rutas privadas conservadas sin datos comerciales en memoria.
El deploy y la validación en Render siguen pendientes, a cargo del usuario; también
quedan pruebas de acceso cruzado para los demás módulos y el resto de tareas desde Fase 5.
Consultar `tasks.md` y ejecutar los checks pertinentes antes de afirmar el estado actual.

**Para retomar**: abrir `docs/specs-backend/tasks.md` — tiene el detalle exacto de
qué se hizo en cada fase, qué falta (Fase 5 en adelante), bugs reales encontrados y
corregidos en el camino (para no repetirlos), y alcance dejado afuera a propósito con su
razón. Para continuar la implementación, revisar los pendientes y sus dependencias según
el pedido del usuario; una tarea documental no implica iniciar la siguiente fase de código.

**Importante**: el usuario pidió explícitamente no hacer commits ni pushes a git en
ningún momento de este proyecto, salvo que lo pida de nuevo explícitamente.

## Reglas que no se negocian

1. Toda regla de negocio y permiso **se valida en el backend**. El frontend no es seguridad.
2. Los controladores devuelven **DTOs**, jamás entidades JPA.
3. **Multi-tenancy activa**: toda entidad de negocio lleva `tenant_id`, el tenant sale
   del usuario autenticado (nunca del request) y toda lectura/escritura se filtra por él.
4. **Historial de etapas es append-only**: cambio de etapa + registro de historial en **una sola transacción**.
5. **Baja lógica** en todo registro con historia comercial. Nunca `DELETE` físico.
6. Contraseñas con `PasswordEncoder` con salt. Nunca texto plano ni cifrado reversible.
7. Las migraciones aplicadas son **inmutables**: se corrige con una versión nueva.
8. Los errores no revelan datos de otro tenant ni stacktraces al cliente.

## Modelo — lo ya decidido

**Etapas del embudo** (DP-06), tabla `stages` configurable, precargada por Flyway:

| # | Etapa | `kind` |
|---|---|---|
| 1 | Consulta recibida | `OPEN` |
| 2 | Necesidad relevada | `OPEN` |
| 3 | Visita al salón | `OPEN` |
| 4 | Propuesta enviada | `OPEN` |
| 5 | Negociación | `OPEN` |
| 6 | Ganada / Reservado | `WON` |
| 7 | Perdida | `LOST` |

Una oportunidad `ABIERTA` sólo puede estar en una etapa `OPEN`. Pasar a `WON`/`LOST` cierra
la oportunidad y exige fecha real de cierre (y motivo de pérdida si es `LOST`).

**Oportunidad** (DP-05/07/09): **un solo salón** (`venue_id`, no N:N), rango
`[event_start,event_end)`, tipo de evento obligatorio y `attendee_count`. V1 incluye
la asociación N:N simple con `EventService`, la validación de capacidad y la exclusión
GiST para reservas ganadas; cierre y concurrencia se incorporaron en §8.

**Producto o servicio** (ADR-004): módulo `offerings/` con `Venue` (salón: capacidad,
tarifa, se reserva) y `EventService` (catering, audio, decoración: precio, no se reserva).

**Supuestos de E1 para el esquema** (DP-03 parcial): moneda única ARS `NUMERIC(15,2)` ·
`timestamptz` en UTC · `Company` con CUIT opcional único por tenant · `Contact` con
`company_id` nullable y documento opcional único por tenant · email del usuario único a
nivel sistema y usado como credencial (DP-03, `docs/specs-backend/design.md` §3.2).

## Entregas

- **E1 — 24/09**: login, ABM empresas/contactos, ABM oportunidades, embudo por etapa con
  cambio persistido. Roles completos, actividades e historial **no** son criterio de E1.
- **Final — 12/11**: todo el CRM (roles, catálogos configurables, actividades, historial,
  cierres, filtros/paginación, especialización real por industria). IA opcional y al final.

## Convenciones de trabajo

- Código, nombres de clases, tablas y columnas **en inglés**; UI y mensajes al usuario **en español**.
- Tablas y columnas en `snake_case`; clases en `PascalCase`.
- Cortes verticales: migración → dominio → service → controller → test.
- Commits en imperativo, con scope: `feat(opportunities): ...`, `docs: ...`.
- Actualizar `docs/` en el mismo PR que cambie un contrato, tecnología o regla crítica.

## Resumen comercial IA — decisión 31/10/2026

[ADR-007](../decisiones/adr/ADR-007-resumen-comercial-ia.md) incorpora el coordinador
`assistance → {activities, opportunities}`, sin dependencias inversas. REST JDK,
resumen efímero y configuración apagada por defecto. Implementación y validaciones
se siguen en [las tareas del cambio](../../openspec/changes/incorporar-resumen-comercial-ia/tasks.md);
no implica despliegue ni validación real de Gemini.

Implementación local del 31/10: backend, contrato, panel automático y verificaciones
independientes completados (28/31 tareas). En la continuación, el usuario confirmó
Free Tier y autorizó configurar una clave sólo en `.env` local, ignorado por Git.
Una generación mínima real pasó la validación; siguen pendientes las cuotas reales,
el corpus de fidelidad y la evaluación del fallback. Sin despliegue. Ver
[evidencia y retomada](../../openspec/changes/incorporar-resumen-comercial-ia/validacion-2026-10-31.md).

Publicación posterior autorizada el 31/10: commit de aplicación `a84f76a` publicado
en la rama de trabajo y en `main`. Vercel y Render se actualizan con la configuración
IA del backend; fallback vacío, sin migraciones nuevas. Estado y evidencia en el
[registro de despliegue](../despliegue-produccion-2026-10-31.md).
El usuario se encarga de las pruebas funcionales en producción; no se completan por
inferencia los pendientes de fidelidad, cuotas ni demostración comercial remota.
