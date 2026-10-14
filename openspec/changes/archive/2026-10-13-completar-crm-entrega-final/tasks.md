# Tasks

Implementación iniciada por solicitud de apply el 09/10/2026. Las marcas indican sólo tareas verificadas. Referencias: [diseño](design.md), [matriz de fuentes y cobertura](trazabilidad.md) y `specs/*/spec.md`.

Orden: §1 → §2 → §3 → §4 → §5 → §6 → §7 → §8 → §9 → §10 → §11. Cada corte debe dejar backend, contrato y frontend compatibles. No ejecutar ninguna escritura de Git; las referencias históricas a tags no son tareas de esta entrega. No instalar IA ni funciones fuera de alcance.

## 1. Fundamentos del cambio y línea base

- [x] 1.1 Registrar ADR-006 (o siguiente ID libre) sobre múltiples embudos, propiedad de etapas y migración; verificar coherencia con ADR-001/003, DP-06 y confirmación del usuario y actualizar índice ADR y requisitos/diseño afectados sin renumerar IDs existentes.
- [x] 1.2 Capturar línea base de panel, lista, detalle y alta con dataset reproducible, caché fría/caliente y servidor despierto/frío separados; entregar mediciones de al menos tres corridas por condición con solicitudes, bytes, tiempos y consultas SQL, sin afirmar mejoras todavía.
- [x] 1.3 Preparar fixtures reutilizables de test con dos tenants, tres roles, clientes compartidos y oportunidades de distintos responsables; verificar que permiten probar listado/ID sin crear un endpoint de alta de organizaciones ni depender de credenciales reales.
- [x] 1.4 Ejecutar suites existentes en entorno local de prueba y registrar resultados propios: backend `./mvnw.cmd clean verify` con JDK 25/Docker y frontend `npm test`, `npm run lint`, `npm run build`; documentar fallas previas concretas sin atribuirse resultados históricos.

## 2. Usuarios y acceso administrativo

- [x] 2.1 Agregar guardas por rol y navegación administrativa sólo para `ADMIN`, conservando guardas de sesión/contraseña; verificar con Testing Library acceso por URL y menú de los tres roles.
- [x] 2.2 Implementar listado de usuarios con búsqueda, rol, estado, paginación y detalle usando la API actual; verificar filtros persistidos en URL y estados vacío/carga/error.
- [x] 2.3 Implementar alta/edición, roles, baja lógica y reset con contraseña temporal; verificar recorrido UI→API→login, cambio obligatorio, revocación y protección de último admin/desactivación propia con pruebas pertinentes.
- [x] 2.4 Ampliar los negativos de administración de usuarios para los tres roles y segundo tenant por listado/ID/escritura; verificar `403`/`404` y conservación de autoría histórica en `*IT`.
- [x] 2.5 Actualizar README frontend, mapa de rutas y contexto de acceso con lo implementado; verificar enlaces, `<Seo noIndexar>`, navegación y build del corte sin documentar funcionalidades futuras como actuales.

## 3. Empresas y contactos completos

- [x] 3.1 Completar validación backend de CUIT con dígito y normalización canónica, email/URL y blancos opcionales conforme DP-03; verificar casos válidos, inválidos, equivalentes con guiones y duplicados por tenant con pruebas de comportamiento.
- [x] 3.2 Normalizar CUIT existentes mediante migración nueva con detección previa de colisiones; probar instalación vacía y actualización con datos V1, y que una colisión falla sin fusionar ni perder registros.
- [x] 3.3 Agregar origen al alta/edición de empresa, resolver responsable por nombre y reemplazar la ayuda que menciona backend; verificar guardado/consulta del origen, herencia y reasignación autorizada en clientes reales de prueba.
- [x] 3.4 Alinear acciones de edición con alcance: vendedor modifica sólo asignados directos y puede consultar relacionados por oportunidad propia; comprobar UI y `SellerScopeIT`, incluyendo acceso por ID y listado de otro tenant.
- [x] 3.5 Ampliar prueba de baja lógica de empresa/contacto con oportunidades y actividades y comprobar que `INACTIVO`/`NO_CONTACTAR` conserva las relaciones y su consulta autorizada.
- [x] 3.6 Exportar contrato si cambiaron campos/validaciones, regenerar tipos y actualizar requisitos/contexto/Postman del corte; verificar fixtures, ejemplos CUIT y formularios compatibles mediante suite pertinente y build.

## 4. ABM de salones y servicios adicionales

- [x] 4.1 Implementar DTOs, servicios y HTTP de alta, detalle, edición y baja lógica de salones/servicios sólo `ADMIN`; validar capacidad, importes y estado y comprobar `403`, `404` cross-tenant y ausencia de borrado físico con `*IT` (BE-OFF-02/04).
- [x] 4.2 Paginar y filtrar oferta con `q`, estado, tamaño 20/máximo 100 y sort permitido; adaptar consumidores/tipos en el mismo corte y comprobar búsqueda de elementos fuera de los primeros 20.
- [x] 4.3 Distinguir nuevas asociaciones activas de conservación de relaciones previas inactivas en edición de oportunidad; verificar edición de notas sin pérdida de salón/servicios y rechazo de nuevas asociaciones no disponibles (BE-OFF-05).
- [x] 4.4 Proteger reservas ganadas frente a reducción de capacidad mediante puerto de consulta en `offerings/service`, sin repositorios ajenos; verificar conflicto al invalidar reserva y revalidación de oportunidades abiertas al operarlas.
- [x] 4.5 Implementar pantallas españolas de ABM de salones y servicios con guardas, estados y formularios accesibles; comprobar el ciclo completo y selección comercial con pruebas pertinentes y recorrido a 390 px.
- [x] 4.6 Actualizar Swagger/export OpenAPI, tipos generados, Postman y documentación de oferta; verificar contrato runtime, frontend build y pruebas de oferta del corte.

## 5. Catálogos comerciales administrables

- [x] 5.1 Implementar ABM de origen, motivo de pérdida, tipo de actividad y tipo de evento con detalle, filtros/paginación, unicidad por tenant y baja lógica; comprobar tipos mínimos semilla y negativos de rol/tenant en `*IT` (BE-CAT-03..06).
- [x] 5.2 Validar referencias activas en nuevas asociaciones y permitir conservar las inactivas previas en clientes/oportunidades; verificar IDs ajenos `404`, motivo/tipo inactivo rechazado y lectura histórica sin pérdida.
- [x] 5.3 Agregar pantallas de configuración por catálogo sólo para administrador; adaptar selectores a listados paginados y probar creación, edición, búsqueda y desactivación sin romper formularios existentes.
- [x] 5.4 Actualizar contrato exportado, tipos, Postman y documentación de catálogos; ejecutar pruebas del corte y build, incluyendo tipo de evento obligatorio y orígenes de empresa/contacto.

## 6. Múltiples embudos y etapas configurables

- [x] 6.1 Crear migración incremental de `pipelines`, backfill por tenant y `pipeline_id` en etapas/oportunidades, FKs compuestas, índices y orden por embudo; verificar V1→última con IDs e historial conservados, tenants independientes y reejecución sin cambios.
- [x] 6.2 Implementar ABM de embudo, predeterminado único, baja lógica y autorización de administrador; verificar activación incompleta rechazada, sustitución de predeterminado atómica y negativos `403`/`404` por rol/tenant.
- [x] 6.3 Implementar ABM de etapas por embudo y puerto de consulta de uso desde `opportunities`; probar tipo inmutable en etapas usadas, prohibición de moverlas y conservación de referencias al desactivar (BE-CAT-02).
- [x] 6.4 Implementar reordenamiento atómico con conjunto completo, unicidad diferible y bloqueo del embudo; verificar intercambios de posiciones, IDs duplicados/ajenos y dos configuraciones concurrentes sin orden inconsistente.
- [x] 6.5 Implementar consulta y copia de plantillas Eventos corporativos/Contratación breve y creación vacía inactiva; verificar independencia de IDs/etapas, mínima configuración para activar y ausencia de precarga automática de embudos adicionales.
- [x] 6.6 Incorporar embudo fijo al alta/detalle/listado/filtros de oportunidad y validar todas las etapas contra él; comprobar rechazo de etapa de otro embudo y de modificación del embudo por `PUT`.
- [x] 6.7 Adaptar tablero con `pipelineId`, identidad del embudo, `kind` y `active`, manteniendo filtros DP-10; verificar columnas renombradas, cerradas e inactivas con oportunidades y alcance del vendedor.
- [x] 6.8 Agregar pantallas de embudos/etapas y selector de embudo en tablero/alta/filtros, con selección en URL y predeterminado visible; probar dos embudos del tenant y consulta histórica de uno inactivo en UI y HTTP.
- [x] 6.9 Exportar contrato, regenerar cliente y actualizar specs backend, arquitectura, contexto, Postman y ADR con el modelo efectivo; verificar suites de migración/embudos y frontend build del corte.

## 7. Alta rápida y formulario de oportunidades

- [x] 7.1 Incorporar selector de clientes con búsqueda/paginación bajo demanda reutilizable en formularios y filtros; verificar que encuentra empresa/contacto posteriores a página 0 y mantiene selección cargada por ID sin descargar todas las páginas.
- [x] 7.2 Filtrar contactos por empresa seleccionada, limpiar incompatibles y proteger contra respuestas tardías; probar cambios rápidos, cliente individual y rechazo servidor de relación contradictoria.
- [x] 7.3 Completar el enlace existente desde contacto cargando contacto/empresa/responsable por detalle autorizado; verificar alta desde contacto corporativo e individual fuera de los primeros 20 y `404` fuera de alcance.
- [x] 7.4 Implementar alta breve desde empresa/contacto con obligatorios del evento, embudo/etapa visibles y opcionales expandibles; probar herencia DP-02, cancelación sin escritura, persistencia y edición posterior sin inventar fechas.
- [x] 7.5 Mejorar disposición accesible de selección múltiple de servicios con CSS Modules/tokens; verificar foco, etiquetas, selecciones históricas y ausencia de superposición/scroll global a 390 px.
- [x] 7.6 Retirar aclaración de almacenamiento Argentina/UTC, conservar fechas en zona de negocio y aviso >50; verificar comportamiento con asistentes dentro/fuera de capacidad y conversión horaria consistente en guardado/lectura.
- [x] 7.7 Actualizar documentación de rutas/flujo contextual y mantener contrato generado; ejecutar pruebas de comportamiento del alta/selección y build, sin agregar tests que sólo repitan textos o CSS.

## 8. Reasignación, cierre, reapertura y reserva

- [x] 8.1 Ampliar historial con snapshots estructurados de futuros cierres y agregar versión esperada al contrato de edición/acciones; verificar migración incremental, tratamiento explícito de legacy y que no se modifican eventos históricos existentes.
- [x] 8.2 Implementar `/assign` sólo `ADMIN`/`SALES_MANAGER` para oportunidades abiertas y destinatario activo del tenant; verificar actor/fecha, alcance antes/después, rechazo a vendedor/cerrada y preservación de actividades (BE-OPP-04).
- [x] 8.3 Implementar ganar con etapa activa `WON` del embudo, valor final no negativo, fecha del servidor y transacción con historial; probar datos obligatorios, estado/etapa inválidos, rol/alcance y rollback HTTP real ante fallo histórico (BE-OPP-07).
- [x] 8.4 Implementar perder con etapa activa `LOST` y motivo activo obligatorio, fecha e historial atómicos; verificar negativos, ausencia de reserva y datos reconstruibles del cierre (BE-OPP-08).
- [x] 8.5 Implementar reapertura autorizada con nota y etapa `OPEN` del embudo activo, limpieza del cierre vigente y liberación de reserva; verificar `403` de vendedor, `409` de estado, historia previa conservada y edición genérica bloqueada mientras cerrada (BE-OPP-09).
- [x] 8.6 Agregar consulta de disponibilidad, revalidación de capacidad/disponibilidad y traducción específica de exclusión GiST a `409 VENUE_ALREADY_BOOKED`; verificar rangos contiguos, superpuestos, otro salón/tenant y conflicto entre embudos distintos (BE-OPP-10/11).
- [x] 8.7 Probar dos confirmaciones HTTP simultáneas de oportunidades distintas con mismo salón/rango usando barrera; exigir exactamente una ganada, otra `409`, una reserva y un historial de cierre, sin depender sólo del precheck.
- [x] 8.8 Traducir optimistic locking a `409` y validar versión esperada de formularios; comprobar con `*IT` dos escrituras de misma versión y petición tardía sin sobrescritura, sin duplicar `@Version` ya existente.
- [x] 8.9 Implementar UI de reasignar, ganar, perder y reabrir con permisos, separación de botones, datos de cierre y estados pendientes; probar conservación del formulario ante `409`, invalidaciones de caché y detalle/listado/tablero consistentes.
- [x] 8.10 Actualizar diseño histórico §9.1 para mantener `/stage` sólo abierto, exportar contrato/tipos/Postman y agregar diagramas de cierre/reserva con el flujo real; verificar pruebas del corte y build.

## 9. Actividades e historial íntegro

- [x] 9.1 Exponer contexto autorizado de oportunidad mediante record en `service` y retirar consumo de entidad desde `activities`; verificar dependencias sin acceso a domain/repository/DTO interno ajenos y mantener alcance por tenant/vendedor.
- [x] 9.2 Implementar POST de actividad con fecha no futura, tipo activo, descripción/resultado, actor/tenant del servidor y derivación coherente de asociaciones; probar hechos en cliente propio y oportunidad propia abierta/cerrada, contradicciones, inactivos y fuera de alcance (BE-ACT-01).
- [x] 9.3 Corregir visibilidad de actividades de oportunidades ajenas que comparten cliente, distinguiendo hechos con/sin oportunidad; verificar cliente compartido con dos vendedores por listado global y por empresa/contacto/oportunidad, incluido segundo tenant.
- [x] 9.4 Implementar alta contextual desde los tres detalles y actualizar las vistas asociadas tras guardar; comprobar que la misma interacción aparece en históricos aplicables sin duplicarse y no existe edición/borrado de actividad.
- [x] 9.5 Agregar paginación navegable de actividades y relaciones históricas de clientes, con orden estable por fecha/ID; verificar más de 20 registros, empates, autor desactivado y baja de empresa/contacto sin truncamiento (BE-ACT-02).
- [x] 9.6 Completar presentación de etapas y cierres sucesivos, notas y autores; verificar recorrido alta→cambio→ganada→reapertura→perdida conservando datos de ambos cierres (BE-ACT-03/04).
- [x] 9.7 Exportar contrato/tipos y actualizar colección Postman, documentación de actividades y diagrama de componentes con módulos efectivos; ejecutar pruebas del corte y frontend build.

## 10. Carga, rendimiento y manejo uniforme de errores

- [x] 10.1 Dividir carga de pantallas por ruta y evitar consultas comerciales de rutas inactivas o IDs inválidos; comprobar en red que panel/alta no descargan todos los datos del CRM y que errores de autorización no entran en ciclos de reintentos.
- [x] 10.2 Retirar dependencia del tablero completo y textos sobre API del panel, mostrando accesos y listas recientes acotadas con carga/error independientes; verificar que una falla de actividades no bloquea oportunidades y no se agregan estadísticas fuera de alcance.
- [x] 10.3 Reemplazar enriquecimiento por fila por lecturas en lotes en contratos de `service`, con mapas locales por request; comparar consultas SQL con 20/100 oportunidades y comprobar que no crecen por cada campo/registro ni cruzan tenants.
- [x] 10.4 Ajustar caché, claves por embudo/filtro/página, cancelación y estados de error; probar navegación repetida, respuesta obsoleta, mutación con invalidaciones y cambio de cuenta sin datos de sesión anterior.
- [x] 10.5 Repetir mediciones de 1.2 bajo iguales datos/condiciones y entregar comparación con solicitudes/bytes/tiempos/consultas; verificar eliminación de cargas completas innecesarias y explicar cualquier límite de hosting sin inventar un SLA.
- [x] 10.6 Revisar mensajes ProblemDetail, validación y logs de los nuevos flujos; comprobar correlación requestId/userId/tenantId sin secretos, SQL sensible ni datos ajenos y documentar resultados reales de observabilidad/rendimiento.

## 11. Regresión integrada y entrega

- [x] 11.1 Configurar Playwright y automatizar login→empresa/contacto→alta breve→embudo→cambio→actividad→cierre→recarga; verificar persistencia contra API/PostgreSQL de prueba, sin sustituir el backend por mocks en este recorrido.
- [x] 11.2 Ejecutar regresión sobre los 16 puntos EF y 13 PDF de trazabilidad y las 15 reglas generales; registrar evidencia por caso, incluidos roles, tenant por listado/ID, cierre incompatible, capacidad y concurrencia, dejando IA explícitamente fuera.
- [x] 11.3 Recorrer rutas tocadas con los tres roles a 390 px y escritorio, ambos temas, teclado y consola; verificar ausencia de scroll horizontal global/errores, SEO privado noindex y title/description/canonical conservados en páginas públicas.
- [x] 11.4 Ejecutar `./mvnw.cmd clean verify`, `npm test`, `npm run lint`, `npm run build` y E2E integrado tras los cortes; registrar resultados actuales y revisar cambios con `git diff --check` y `git status --short` sólo en modo lectura.
- [x] 11.5 Validar instalación vacía y actualización desde V1 con datos comerciales representativos de varios tenants; comprobar IDs, etapa/embudo, actividades, cierres y restricción GiST intactos, sin modificar V1 ni reiniciar bases existentes.
- [x] 11.6 Verificar artefactos mantenidos por corte: OpenAPI runtime/export, tipos generados, diagramas, Postman/env, README, Docker/Compose y `.env.example` sin secretos; ejecutar desde un entorno limpio los comandos documentados e importar/recorrer la colección sin ajustes manuales de IDs.
- [x] 11.7 Reconciliar tareas/contexto/arquitectura/AGENTS/CLAUDE/README con lo efectivamente implementado y las validaciones propias; comprobar enlaces y retirar afirmaciones obsoletas de maqueta sin completar checks no ejecutados.
- [ ] 11.8 Ensayar demostración integral en el ambiente publicado disponible, con login y flujo completo, roles, segundo embudo y reserva concurrente; registrar URLs, condiciones y resultados, o mantener esta tarea pendiente si no hay acceso/ambiente. No ejecutar deploy, tags ni otras escrituras de Git por inferencia de esta tarea.

El 13/10/2026 el usuario confirmó testing integral local satisfactorio y autorizó
archivar con 11.8 pendiente. No se informó un ensayo en ambiente publicado.
Ver [cierre y límites de validación](cierre-2026-10-13.md).

## Workflow follow-up

- Cambio archivado por solicitud del usuario el 13/10/2026, con nueve specs sincronizadas.
- Conservar 11.8 pendiente hasta disponer de evidencia del ensayo publicado.
