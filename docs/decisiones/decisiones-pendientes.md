# Registro de decisiones

Este registro evita que decisiones funcionales o técnicas se oculten en implementaciones aisladas. El responsable de cada tema debe documentar la resolución, fecha, participantes, alternativas y requisitos afectados. Si la decisión cambia arquitectura, también se crea un ADR en [`adr/`](adr/).

| ID | Tema | Estado | Fecha límite sugerida |
|---|---|---|---|
| DP-01 | Autorización y efectos de modificar/reabrir una oportunidad cerrada | **Aceptada** | 12/09 |
| DP-02 | Herencia, asignación y visibilidad de responsables comerciales | **Aceptada** | 12/09 |
| DP-03 | Diccionario de datos, formatos, unicidad, moneda y obligatoriedad | **Aceptada** | 12/09 |
| DP-04 | Roles que administran salones y efecto de su desactivación | **Aceptada** | 12/09 |
| DP-05 | Fecha de ocurrencia frente a fecha de registro de actividades | **Aceptada** | 12/09 |
| DP-06 | Etapas, transiciones, orígenes, pérdidas, actividades y tipos de evento | **Aceptada** | 12/09 |
| DP-07 | Semántica temporal de superposición, zona horaria y reaperturas | **Aceptada** | 12/09 |
| DP-08 | Aplicación del nicho de hasta 50 personas | **Aceptada** | 12/09 |
| DP-09 | Atributos especializados de E1 y un salón por oportunidad | **Aceptada** | 12/09 |
| DP-10 | Búsquedas, filtros, orden y tamaño de página | **Aceptada** | 12/09 |
| DP-11 | Stack, despliegue, pantallas, calidad, equipo y año | **Parcial** — stack cerrado | 11/09 |
| DP-12 | Incorporación y flujo de IA opcional | Diferida | Después del núcleo |

Las decisiones **técnicas** del backend (DT-00..DT-24) se registran aparte, en
`docs/context/03-decisiones-tecnicas.md`, y las que alteran arquitectura tienen su ADR.

## Prioridad inmediata

Las decisiones funcionales que bloqueaban las fases 5 a 9 quedaron aceptadas el
12/09/2026. Tras el reinicio de la base autorizado el 17/09/2026, V1–V4 se consolidaron
en una nueva V1. Las modificaciones posteriores requieren migraciones nuevas; la
inmutabilidad sigue siendo la regla para versiones aplicadas.

---

# Decisiones resueltas

## DP-01 — Cierre, edición y reapertura

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-OPP-07, BE-OPP-08, BE-OPP-09
- Decisión: una oportunidad cerrada no admite edición genérica ni cambio libre de
  etapa. Ganar y perder son casos de uso separados. Sólo `ADMIN` y `SALES_MANAGER`
  pueden reabrirla, indicando una nota y una etapa activa `OPEN`; la reapertura limpia
  `closed_at`, `final_value` y `loss_reason_id` y registra historial en la misma
  transacción.
- Consecuencias y pruebas necesarias: `409` ante estados incompatibles y pruebas de
  autorización, limpieza de cierre e historial atómico.

## DP-02 — Responsables, herencia y alcance comercial

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-SEC-06, BE-CUS-01..09, BE-OPP-01..04
- Decisión: empresas, contactos y oportunidades tienen responsable obligatorio. Al
  crear, el contacto hereda el de su empresa y la oportunidad el del contacto, luego
  de la empresa y finalmente el usuario actual. `ADMIN` y `SALES_MANAGER` pueden
  reemplazarlo; `SELLER` sólo puede asignarse a sí mismo. Un `SELLER` lee clientes
  asignados directamente o relacionados con oportunidades propias, pero sólo edita
  los asignados directamente. Si un contacto pertenece a una empresa, la oportunidad
  deriva esa empresa; si el request envía ambos ids deben coincidir.
- Migración de datos: se conserva el responsable existente; si falta se usa
  `created_by` cuando sea un usuario activo del tenant y, en último término, un
  `ADMIN` activo. La migración falla si no existe responsable válido.
- Consecuencias y pruebas necesarias: listados e ids directos deben respetar tenant y
  alcance; un recurso fuera del alcance responde `404`.

## DP-03 — Diccionario de datos final

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: clientes, usuarios, salones y oportunidades
- Decisión: se mantienen moneda ARS e instantes UTC con presentación en
  `America/Argentina/Buenos_Aires`. La empresa separa razón social y nombre comercial:
  el `name` existente se copia inicialmente en ambos. Teléfonos, direcciones y sitios
  son opcionales; email y URL se validan por formato y los blancos se normalizan a
  `null`. El CUIT opcional conserva unicidad por tenant y pasa a validar dígito
  verificador. La contraseña tiene 10–72 caracteres e incluye mayúscula, minúscula,
  número y símbolo.
- Consecuencias y pruebas necesarias: límites de longitud consistentes entre DTO,
  base, OpenAPI y formularios; migración incremental de datos existentes.

## DP-04 — Administración y estado de salones

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-OFF-01..05
- Decisión: sólo `ADMIN` administra salones, servicios y catálogos. El salón usa
  `DISPONIBLE`, `MANTENIMIENTO` o `INACTIVO`; `active=true` migra a `DISPONIBLE` y
  `false` a `INACTIVO`. Los no disponibles permanecen en datos históricos, no se
  ofrecen en nuevas asociaciones y pueden conservarse al editar una relación previa.

## DP-05 — Fecha de las actividades

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-ACT-01..03
- Decisión: `occurred_at` es una fecha real no futura indicada por el usuario;
  `created_at` y `created_by` los fija el servidor. Las actividades son append-only y
  una actividad de oportunidad deriva automáticamente contacto y empresa.

## DP-07 — Intervalos, zona horaria y superposición

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-OPP-10, BE-OPP-11
- Decisión: una oportunidad usa `event_start` y `event_end` como `timestamptz`, con
  `event_end > event_start`. La interfaz trabaja en `America/Argentina/Buenos_Aires` y
  la API persiste instantes UTC. La disponibilidad usa intervalos semiabiertos
  `[inicio, fin)`, por lo que dos eventos contiguos no se superponen. La migración
  histórica convertía `event_date` a inicio y fijaba el fin un día después; desde el
  reinicio del 17/09/2026, V1 crea directamente el rango. La restricción GiST aplica
  únicamente a oportunidades `GANADA` del mismo tenant y salón.
- Consecuencias y pruebas necesarias: validación de aplicación, restricción física y
  prueba concurrente donde exactamente una confirmación tiene éxito.

## DP-08 — Capacidad y nicho de hasta 50 personas

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-OPP-10
- Decisión: superar la capacidad del salón siempre bloquea con `422`. Superar 50
  personas sólo muestra una advertencia en la interfaz y no agrega otro bloqueo.

## DP-10 — Búsqueda, filtros y paginación

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: BE-CUS-09, BE-OPP-12 y listados administrativos
- Decisión: todos los listados aceptan `page`, `size` y `sort`, con tamaño 20 y máximo
  100. Empresas: `q`, `status`, `originId`, `salesRepId`; contactos suman `companyId`;
  oportunidades: `q`, `status`, `stageId`, `originId`, `salesRepId`, `venueId`,
  `companyId`, `contactId`, `eventFrom`, `eventTo`. Usuarios y maestros aceptan `q`
  más rol o estado aplicable. El embudo acepta los filtros comerciales sin paginación.

## DP-06 — Catálogos y transiciones del embudo comercial (resuelta)

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: embudo comercial de E1, cambio de etapa, historial de etapas
- Decisión: siete etapas precargadas, con **visita al salón** como etapa propia de la industria.

| # | Etapa | Tipo (`kind`) | Cierra la oportunidad |
|---|---|---|---|
| 1 | Consulta recibida | `OPEN` | no |
| 2 | Necesidad relevada | `OPEN` | no |
| 3 | Visita al salón | `OPEN` | no |
| 4 | Propuesta enviada | `OPEN` | no |
| 5 | Negociación | `OPEN` | no |
| 6 | Ganada / Reservado | `WON` | sí |
| 7 | Perdida | `LOST` | sí |

  `Stage` es una tabla configurable (`name`, `position`, `kind`, `active`), no un enum,
  porque la entrega final exige un embudo configurable. Se precarga por migración Flyway.
  Una oportunidad `ABIERTA` sólo puede estar en una etapa `OPEN`; mover a `WON` o `LOST`
  cierra la oportunidad y exige fecha real de cierre (y motivo de pérdida si es `LOST`).

- Alternativas consideradas: embudo de 6 etapas (descartado por parecerse al ejemplo genérico
  de la consigna y aportar poco a la especialización exigida); embudo de 9 etapas con
  "disponibilidad verificada" y "seña" separadas (descartado por exceso de columnas en el
  tablero y más transiciones que validar sin beneficio claro para el TP).
- Consecuencias y pruebas necesarias: migración `V1__initial_schema.sql`; validación de
  compatibilidad etapa/estado en el servicio de cambio de etapa; test de que una oportunidad
  abierta no puede quedar en etapa `WON`/`LOST` sin pasar por el caso de uso de cierre.
- Resolución final: entre etapas activas `OPEN` se puede avanzar, retroceder o saltear
  posiciones libremente. Ganar y perder se realizan sólo mediante sus casos de uso
  específicos. Orígenes, motivos de pérdida, tipos de actividad y tipos de evento son
  catálogos configurables por `ADMIN`; los inactivos se conservan en históricos pero no
  se ofrecen para nuevas asociaciones. Cada oportunidad tiene un tipo de evento
  obligatorio; los registros existentes migran a “Evento corporativo”.

## DP-09 — Un salón por oportunidad y atributos del evento en E1

- Estado: Aceptada
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: modelo de `Opportunity`, capacidad del salón, reserva
- Decisión: una oportunidad referencia **un único salón** mediante FK `venue_id` (no una
  relación N:N). Desde E1 la oportunidad incluye `event_date` y `attendee_count`. La
  validación de capacidad y la restricción de exclusión por solapamiento llegan en la etapa
  de especialización (04/11), sobre columnas que ya existen desde E1.
  Los `EventService` asociados a una oportunidad quedan fuera de E1.
- Alternativas consideradas: N:N salón↔oportunidad para eventos que ocupan dos salones
  (descartado: complica la reserva y la restricción de solapamiento sin caso real en el TP);
  postergar `event_date` y `attendee_count` a después de E1 (descartado: obliga a una
  migración de columnas nuevas sobre datos existentes justo antes de la especialización).
- Consecuencias y pruebas necesarias: `venue_id NOT NULL` en `opportunities`; la restricción
  GiST de 04/11 se apoya en `venue_id` + rango temporal + `tenant_id` filtrado por estado
  ganado; test de que `attendee_count` no supera la capacidad del salón.

## DP-03 (parcial) — Diccionario de datos para E1

- Estado: Parcial
- Fecha: 12/09/2026
- Participantes: Nehuen Ascoitia
- Requisitos afectados: migración `V1__initial_schema.sql`, validaciones de entrada
- Decisión: para E1 rige el siguiente supuesto, declarado explícitamente para no esconderlo
  en el código. Se corrige con una migración nueva si el equipo define otra cosa.
  - Moneda única ARS, `NUMERIC(15,2)`, sin campo `currency`.
  - Instantes en `timestamptz` (UTC); zona de negocio `America/Argentina/Buenos_Aires`.
  - `Company`: obligatorios nombre y estado. CUIT opcional, **único por tenant cuando está
    presente**. Email validado por formato, no único.
  - `Contact`: obligatorios nombre, apellido y estado. Documento opcional, único por tenant
    cuando está presente. `company_id` **nullable** (cliente individual).
  - `User`: email **único a nivel sistema** (no por tenant), es la credencial de login.
    El login recibe sólo email + password, sin selector de organización — ver la nota
    en `V1__initial_schema.sql`.
  - Textos libres (`observaciones`) sin límite duro más allá del tipo de columna.
- Pendiente: obligatoriedad y formato exacto de teléfono, dirección y sitio web; longitudes
  máximas por campo; si el CUIT se valida con dígito verificador.

## DP-11 (parcial) — Stack y despliegue

- Estado: Parcial
- Fecha: 12/09/2026 (actualiza el acuerdo previo)
- Participantes: Nehuen Ascoitia
- Decisión: monorepo, monolito modular, React/TypeScript/Vite, **Java 25 + Spring Boot 4**,
  PostgreSQL con Flyway, JWT, Docker, **Render** para la API y **Supabase** como PostgreSQL
  administrado (reemplaza a Neon). Ver [ADR-002](adr/ADR-002-plataforma-supabase-render.md).
- Pendiente: año formal de los hitos, diseño de pantallas, reparto del equipo y parámetros
  cuantitativos de calidad.

---

## Plantilla de resolución

```markdown
## DP-XX — Título

- Estado: Aceptada | Reemplazada
- Fecha:
- Participantes:
- Requisitos afectados:
- Decisión:
- Alternativas consideradas:
- Consecuencias y pruebas necesarias:
```
