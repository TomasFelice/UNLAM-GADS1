# Design

## Context

La motivación y el alcance están en [proposal.md](proposal.md). La auditoría es estática, realizada el 09/10/2026; no se ejecutó la aplicación ni se asumieron como actuales las validaciones históricas de septiembre. [trazabilidad.md](trazabilidad.md) cruza las fuentes y el estado observado.

Estas observaciones describen el punto de partida de la propuesta. El apply posterior
implementó los cortes §1–§10; la [validación integrada de §11](validacion-punto-11-2026-10-10.md)
registra las ejecuciones actuales. El estado efectivo se consulta en [tasks.md](tasks.md),
sin interpretar los párrafos prospectivos siguientes como funcionalidades aún ausentes.
La publicación y el ensayo 11.8 quedan a cargo del usuario por su instrucción del 10/10.

El frontend ya usa API, sesión, tipos OpenAPI, TanStack Query, React Hook Form y Zod. `access` ya tiene ABM de usuarios, cambio obligatorio de contraseña y revocación. `offerings` y `catalogs` exponen lecturas; `activities` permite leer, pero no registrar. `OpportunityService` crea/edita, `ChangeStageService` mueve entre etapas abiertas y existe historial inicial. `Opportunity` ya tiene `@Version` y V1 ya contiene exclusión GiST.

El usuario confirmó varios embudos por organización con ABM y alta rápida con cliente preseleccionado. Es una ampliación del modelo de etapa única por tenant, no una decisión funcional pendiente. Se mantienen ADR-001..005 y DP-01..10; las secciones históricas parciales no desplazan las resoluciones aceptadas. El PDF original de consigna menciona GADS II y el contexto del repo GADS I: no modificar esa identificación en este cambio, porque no cambia el alcance funcional.

## Goals / Non-Goals

**Goals:**

- Completar por cortes verticales, reutilizando lo existente y verificando permisos en servidor.
- Introducir embudos sin ciclos entre módulos, pérdida de IDs ni reinicio de la base.
- Hacer comprobables atomicidad, concurrencia, historia y rendimiento bajo condiciones repetibles.
- Mantener un contrato único exportado y una matriz de cobertura para la entrega.

**Non-Goals:**

- Rehacer autenticación, stack, diseño visual completo o módulos ya funcionales.
- Incorporar transferencias de oportunidades entre embudos, automatizaciones, IA o nuevos indicadores.
- Crear registro público de organizaciones o ampliar permisos de vendedores.
- Ejecutar escrituras de Git. Los tags pendientes del checklist histórico quedan excluidos por instrucción expresa.

## Decisions

### 1. Propiedad de embudos y límites modulares

`Pipeline` vive en `catalogs/domain`, junto con `Stage`. `PipelineService` administra configuración y consultas; `opportunities` consume contratos en `catalogs/service`. Crear un módulo superior para embudos obligaría a mover etapas o introducir ciclos, sin un beneficio para el tamaño del proyecto.

Las validaciones de uso de etapas se resuelven con un puerto definido en `catalogs/service` e implementado en `opportunities`, siguiendo `CustomerVisibilityPort`. Se aplica también cuando oferta necesita comprobar si una reducción de capacidad invalida reservas ganadas: puerto en `offerings/service`, implementación en `opportunities`, sin leer repositorios ajenos.

Los contratos nuevos o tocados entre módulos utilizan records en `service`; `activities` recibe un contexto autorizado de oportunidad, no su entidad. Actualmente `OpportunityAccessService` devuelve `Opportunity` y algunos servicios consumen DTOs internos de otros módulos: son desviaciones observadas, no convenciones a extender. Corregir esas fronteras al tocar los flujos afectados, sin una refactorización general independiente.

La decisión quedó registrada el 09/10/2026 en [ADR-006](../../../../docs/decisiones/adr/ADR-006-multiples-embudos.md), al iniciar el apply solicitado. El modelo está aceptado; su migración y contratos siguen pendientes del corte §6.

### 2. Modelo e invariantes de embudos

| Elemento | Cambio propuesto |
|---|---|
| `pipelines` | `id`, `tenant_id`, `name`, `active`, `is_default`, auditoría; nombre único por tenant e índice único parcial para el predeterminado |
| `stages` | `pipeline_id NOT NULL`; sustituir unicidad `(tenant_id, position)` por `(tenant_id, pipeline_id, position)` |
| `opportunities` | `pipeline_id NOT NULL`, fijo tras crear; índice por tenant/embudo/estado |
| Integridad | FKs compuestas que aseguren tenant/embudo de etapa y oportunidad; no confiar sólo en IDs globales |
| `stage_history` | Mantener IDs previos y agregar datos estructurados del cierre para futuros eventos; ver decisión 6 |

El primer embudo de cada tenant queda activo y predeterminado. Nuevos embudos vacíos se guardan inactivos mientras se configuran. Activar exige etapas activas `OPEN`, `WON` y `LOST`. Desactivar el predeterminado exige designar otro primero, en transacción y con serialización de la configuración del tenant. No se pueden mover etapas ya creadas a otro embudo ni cambiar `kind` cuando tienen uso comercial. Los tipos de cierre se deciden por `kind`, nunca por texto del nombre.

La baja de un embudo o etapa conserva sus fichas e historial; bloquea nuevas asociaciones. El tablero muestra columnas históricas necesarias, señalando inactividad. Las oportunidades abiertas existentes pueden seguir operándose en su embudo inactivo usando etapas activas existentes; reabrir o crear exige embudo activo. No hay transferencia entre embudos en esta entrega: se evita introducir mapeos de etapas e historial ambiguos.

Reordenar etapas recibe una lista completa de IDs del embudo; valida conjunto, pertenencia y posiciones y aplica un intercambio atómico. Usar unicidad diferible dentro de la transacción y bloqueo del embudo para evitar colisiones intermedias o reordenamientos concurrentes. No desactivar la última etapa activa de una clase en un embudo activo.

### 3. Embudo inicial y sugerencias

Se recomienda **un embudo inicial**, no varios precargados: conserva el escenario comercial existente y evita tableros vacíos impuestos. Las siete etapas de DP-06 se mantienen para eventos corporativos.

El ABM ofrece creación desde cero o copia de una plantilla. Dos plantillas iniciales bastan:

| Plantilla | Etapas |
|---|---|
| Eventos corporativos | Consulta recibida, Necesidad relevada, Visita al salón, Propuesta enviada, Negociación, Ganada/Reservado, Perdida |
| Contratación breve | Consulta recibida, Propuesta enviada, Ganada/Reservado, Perdida |

Las plantillas son definiciones de sólo lectura de la aplicación, no embudos globales mutables con oportunidades. Copiarlas crea IDs propios del tenant en una transacción; modificar la copia no cambia la definición ni otros embudos. Se permite previsualizar etapas antes de copiar. Descartada la creación automática de varios procesos porque no hay evidencia de que todas las organizaciones los necesiten.

### 4. Contrato HTTP y coordinación con frontend

Base `/api/v1`; los endpoints siguientes son **propuestos**, no implementados:

| Área | Contrato propuesto |
|---|---|
| Embudos | `GET/POST /pipelines`, `GET/PUT /pipelines/{id}`, `POST /pipelines/{id}/default` |
| Plantillas | `GET /pipeline-templates`; `POST /pipelines` admite `templateKey` opcional |
| Etapas | `GET/POST /stages`, `GET/PUT /stages/{id}`, filtro `pipelineId`, `POST /pipelines/{id}/stages/reorder` |
| Oferta | Completar `POST/PUT` y listado paginado de `/venues` y `/event-services` |
| Catálogos | Completar `POST/PUT`, detalle y listado paginado de `/origins`, `/loss-reasons`, `/activity-types`, `/event-types` |
| Oportunidad | Agregar `pipelineId` a alta, respuesta y filtros; `POST /opportunities/{id}/assign`, `/win`, `/lose`, `/reopen` |
| Actividades | `POST /activities`; conservar lecturas global y por asociación con paginación |

Se conserva la baja por `PUT` del estado/actividad, sin `DELETE` físico. `/stage` permanece exclusivamente para `OPEN`, conforme BE-OPP-05 y DP-01/06. El diseño histórico §9.1 que propone delegación libre a cierres contradice la resolución posterior; actualizarlo al implementar, no abrir una segunda ruta de cierre.

Los listados de oferta y catálogos pasan de arrays a `PageResponse` con `q`, estado, `page`, `size` y `sort` validado; es un cambio de contrato coordinado. Los selectores consultan páginas bajo demanda, no descargan todo para filtrar en cliente. `/users/assignable` conserva su contrato existente; extender búsqueda/paginación si las mediciones o volumen de usuarios lo requieren sin alterar los permisos.

El tablero acepta `pipelineId` (predeterminado si se omite) y responde identidad del embudo y columnas con `kind`/`active`. El listado de oportunidades puede filtrar por embudo o mostrar todos los autorizados. `PUT` de oportunidad no cambia embudo, responsable, estado ni etapa; rechazar intentos de cambio de embudo explícitamente.

En cada corte: anotaciones Swagger, export desde runtime usando `OpenApiExportIT`/`target/openapi.yaml`, actualización de `backend/docs/openapi.yaml`, `npm run generar:api` y adaptación de consumidores. No escribir tipos equivalentes a mano. Desplegar backend/frontend compatibles juntos; no hay terceros conocidos que requieran una API legacy permanente.

### 5. Completar acceso, clientes y maestros

Reutilizar `UserService` y endpoints existentes en pantallas españolas de usuarios: listado, alta/edición, estado y contraseña temporal. Guardas de ruta y navegación por rol complementan `@PreAuthorize` de servicios. Mantener cambio obligatorio, `auth_version`, protección del último admin y aislamiento.

Para clientes: agregar origen al formulario de empresa, identificar responsable por nombre, presentar sólo acciones permitidas y conservar alta/edición actuales. Terminar DP-03: CUIT con dígito, representación canónica sin guiones para unicidad, formato de URL en backend y blancos opcionales a `null`. Usar nueva migración para normalizar CUIT existentes con prevalidación de colisiones; ante colisiones no fusionar ni borrar automáticamente. No introducir longitudes arbitrarias: conservar las vigentes y alinear esquema/DTO/OpenAPI/formularios donde haya límites definidos.

No tratar un campo omitido en edición como eliminación accidental de la referencia anterior. Distinguir conservar una relación inactiva de agregar otra nueva; esta regla afecta salón, servicios, origen y tipo de evento. Revalidar disponibilidad y capacidad al ganar. Reducir capacidad no puede invalidar una reserva ganada existente: rechazar con conflicto comercial; oportunidades abiertas se revalidan en su próxima edición/cierre.

### 6. Cierres, reserva, reasignación e historial

Casos de uso transaccionales propios para ganar, perder y reabrir, reutilizando autorización de alcance. Ganar valida oportunidad abierta, embudo/etapa coherentes, salón disponible, asistentes y `finalValue >= 0`. Perder exige motivo activo y etapa `LOST`. `closedAt` procede del reloj del servidor. Reabrir exige rol elevado, nota y destino `OPEN`, limpia cierre vigente y agrega historial. Asignar exige rol elevado y destino activo del tenant; sólo oportunidades abiertas, con `updated_by`/`updated_at`.

Conservar en `stage_history` el tipo de evento histórico y snapshots de estado destino, valor final y motivo del cierre. Al reabrir no se alteran filas anteriores. Para registros legacy sólo completar datos que puedan inferirse inequívocamente de la oportunidad actual; no inventar valores de ciclos anteriores. Se pueden agregar snapshots de nombres para el motivo si su edición posterior cambia la etiqueta. No usar actividades editables como sustituto del historial de etapas.

La consulta previa de disponibilidad se hace en `opportunities`, con tenant, salón, estado ganado y `start < otherEnd AND end > otherStart`, excluyendo el propio ID. GiST existente sigue siendo el árbitro final, **sin incluir embudo**: dos procesos comparten el mismo recurso físico. Traducir específicamente `no_overlapping_won_reservation`/SQLSTATE `23P01` a `409` y código `VENUE_ALREADY_BOOKED`; mantener traducción genérica para otras restricciones y agregar `409` de optimistic locking. Forzar el flush antes de completar cuando sea necesario para capturar fallos; no capturar y continuar dentro de una transacción invalidada.

`@Version` ya existe; probarlo, no agregarlo de nuevo. Para formularios abiertos durante cambios posteriores, exponer `version` y aceptar la versión leída en ediciones/acciones que parten de un detalle. Validar versión esperada además de la comprobación JPA para evitar que una petición tardía recargue silenciosamente la versión nueva. En creación no se exige versión. El conflicto conserva lo escrito por el otro usuario y ofrece recargar.

### 7. Actividades y consulta histórica

Agregar DTO de alta y operación de servicio con fecha real no futura, tipo activo y al menos una asociación. Obtener actor/tenant del contexto. Con oportunidad, derivar empresa/contacto de un contrato autorizado; con contacto, derivar empresa. Rechazar IDs explícitos contradictorios. Permitir registrar hechos sobre una oportunidad cerrada porque no cambia su estado; mantener el mismo alcance comercial.

Un vendedor puede escribir actividades en sus clientes asignados directamente o en sus oportunidades. Los clientes visibles sólo por oportunidad no habilitan escrituras generales. `ActivitySpecifications.visible` actualmente hace un OR entre empresa/contacto/oportunidad: revisar y corregir porque una actividad de oportunidad ajena podría entrar por una empresa compartida. Si hay `opportunity_id`, debe verificarse también el alcance de esa oportunidad; sólo hechos sin oportunidad usan el alcance del cliente.

Conservar append-only. Ordenar actividades por `occurredAt desc, id desc` y etapas por `changedAt asc, id asc`; agregar navegación de página a `useActividades` y paneles de detalle, que hoy fijan `page=0`. Historial de etapas puede mantener la lista completa actual mientras el volumen sea reducido; cualquier paginación posterior debe permitir reconstrucción íntegra.

### 8. Alta rápida, selección y presentación

Extender el flujo actual, no crear otro modelo de oportunidad. Desde empresa/contacto se precarga por ID autorizado, incluso fuera de la primera página. Mostrar título, embudo/etapa, salón, tipo, horario y asistentes; proponer embudo predeterminado y primera etapa activa de ese embudo, visibles y reemplazables. Responsable se hereda por DP-02 y se puede reemplazar según rol; datos opcionales se expanden o editan después. No inventar un rango de evento.

Reutilizar `companyId` del filtro backend para contactos y agregar un selector de búsqueda paginada. Un cambio de empresa limpia contacto incompatible; el contexto de un contacto corporativo deriva la empresa. Mantener valor seleccionado por consulta de detalle, no por presencia en página de opciones. Cancelar no escribe. El enlace de `PaginaContactoDetalle` ya existe y se completa/verifica, no se duplica.

Presentar servicios en opciones con etiqueta y espacio, reutilizando CSS Modules/tokens/Icono. Espaciar botones de ganar/perder y agregar formularios de cierre y reapertura con validación, foco y estados pendientes. Quitar los tres textos técnicos identificados en el PDF. Español, zona de negocio y aviso >50 se conservan. Mantener `<Seo noIndexar>` en las nuevas rutas y metadatos de páginas públicas en la regresión.

### 9. Rendimiento basado en evidencias

Observaciones del código, **sin medición runtime todavía**:

- `Rutas` importa todas las pantallas estáticamente.
- `PaginaPanel` solicita tablero completo, listado y actividades y espera el tablero para renderizar.
- `OpportunityService.toEnrichedResponse` resuelve cliente, usuario, salón y etapa por cada fila; existe riesgo de consultas N+1.
- Los formularios cargan página 0 de clientes y los catálogos sin paginar.
- La caché de consultas ya existe (`staleTime` global de 30 segundos); no proponer agregarla como si faltara.

Primero capturar línea base de panel, lista, detalle y alta con el mismo dataset, carga fría/caliente y servidor despierto/frío por separado. Registrar red, bytes, JS, render y número de consultas SQL, mínimo tres corridas por condición. No tomar el arranque del hosting como tiempo exclusivo del frontend.

Solución prevista: dividir código por rutas, renderizar secciones independientes, listas recientes acotadas y selección bajo demanda. Retirar del panel la dependencia del tablero y los indicadores fuera de alcance que la exigen; conservar accesos y listas recientes. No crear un módulo nuevo de estadísticas para resolver este problema.

Resolver nombres por lotes mediante contratos de lectura en `service` y mapas por request; no joins contra tablas de otro módulo ni caché global sin tenant. Medir el conteo de consultas con 20 y 100 filas para comprobar que deja de aumentar por cada campo y registro. Reutilizar catálogos vigentes, cancelar búsquedas obsoletas y usar claves con embudo/filtros/página. Invalidar detalle, listas, tablero, historial y clientes afectados según la mutación; limpiar caché al cambiar de sesión.

### 10. Estrategia de ejecución y validación

Seguir el orden de tareas: acceso/clientes → oferta/catálogos → embudos → oportunidades/cierres → actividades → rendimiento → regresión. Cada corte incluye contrato, interfaz y pruebas pertinentes; no dejar integración frontend para el final. El rendimiento tiene captura inicial antes de cambios y comparación después.

Unitarios para coordinación y reglas aislables; `*IT` HTTP real con PostgreSQL Testcontainers para permisos, transacciones, migraciones y concurrencia. Fixtures con segundo tenant mediante infraestructura de test: no hace falta un endpoint de alta de tenants y no debe seguir postergándose esa cobertura. La prueba concurrente usa oportunidades distintas del mismo salón con barrera de inicio y requests HTTP realmente paralelos; verifica una ganada, un `409` y un único historial de cierre. Probar aparte conflictos sobre una misma oportunidad.

Frontend: Vitest/Testing Library para comportamiento, selectores y errores; configurar Playwright porque hay dependencia/script pero no se encontraron archivos de configuración ni E2E. Automatizar el recorrido crítico y complementar con revisión a 390 px, teclado, contraste en ambos temas y consola. `npm run build`, `npm run lint`, `npm test`; backend `./mvnw.cmd clean verify` con JDK 25/Docker.

Actualizar docs, colección Postman, ejemplos sin secretos y diagramas sólo con funcionalidades implementadas. Verificar comandos del README desde entorno limpio. La consigna no exige documentación adicional, pero los artefactos BE-DOC existentes siguen siendo compromisos del proyecto, sin inventar entregables nuevos. La final se valida con los 16 puntos del checklist; el caso 17 (IA) queda fuera por ser opcional. Todas las operaciones Git de verificación son sólo lectura; ningún tag, branch, commit, add o push.

## Risks / Trade-offs

- Contrato de embudos y listados cambia → exportar y regenerar por corte y desplegar consumidores compatibles en una ventana coordinada.
- Configuración concurrente o desactivación deja proceso incompleto → transacciones, bloqueo de configuración e invariantes antes de activar.
- Normalización de CUIT descubre colisiones legacy → detener migración con diagnóstico y corregir datos concretos sin fusionarlos automáticamente.
- Nombres de catálogos cambian después de un cierre → conservar IDs y snapshots donde se necesita reconstruir el dato histórico.
- Ampliación grande antes del 12/11 → ejecutar los cortes ordenados de `tasks.md`, priorizando funcionalidad sobre plantillas adicionales o mejoras fuera de alcance.
- Ambiente publicado o credenciales no disponibles → completar local/CI y mantener pendiente la demostración remota con su limitación explícita.

## Migration Plan

1. Registrar el ADR de embudos y actualizar especificaciones afectadas al iniciar implementación. Capturar línea base y estado de datos sin mutarlos.
2. Crear migraciones nuevas desde V2; no tocar V1. Agregar embudos y columnas primero compatibles con el backfill; crear un embudo por tenant, relacionar etapas y derivar embudo de cada oportunidad por su etapa.
3. Validar tenant/relaciones antes de `NOT NULL`/FKs compuestas, sustituir unicidad del orden, crear índices y ampliar historial. Normalizar CUIT en migración separada con detección previa de colisiones. Mantener GiST intacto respecto del embudo.
4. Probar base vacía V1→última y actualización V1 con datos/IDs/historial, múltiples tenants y reejecución sin cambios. Probar datos de cierre legacy sin inventar eventos históricos.
5. Exportar contrato y regenerar cliente; verificar backend y frontend juntos con el seed y el recorrido final antes del ambiente publicado.
6. Para despliegue real, disponer de respaldo y ventana controlada; no ejecutar cambios remotos desde este workflow. Ante fallo de migración transaccional, conservar versión previa. Tras escrituras con varios embudos no volver a una API que ignora esa dimensión: corregir hacia adelante o restaurar respaldo completo sólo por decisión explícita del operador.

## Open Questions

Sólo quedan datos operativos diferibles: URLs efectivas y disponibilidad de Render/Vercel para la demostración, y nombres comerciales preferidos para las plantillas. Se usan los nombres propuestos y los entornos locales hasta contar con esos datos; no bloquean el código ni alteran el contrato funcional.
