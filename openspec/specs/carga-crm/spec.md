# carga-crm Specification

## Purpose

Mantener el CRM utilizable durante la carga inicial y la navegación, limitando el trabajo solicitado y preservando errores, permisos y frescura de los datos.

## Requirements

### Requirement: Carga comercial acotada por pantalla
El sistema SHALL solicitar datos comerciales sólo de la ruta activa y búsquedas o selecciones iniciadas por el usuario. Listados y selectores SHALL usar páginas acotadas sin recorrerlas automáticamente todas. El panel SHALL poder mostrar sus listas recientes sin esperar a descargar el tablero completo; el tablero SHALL limitarse al embudo elegido y sus filtros.

#### Scenario: Entrada al panel
- **WHEN** un usuario inicia sesión y abre el panel
- **THEN** no se cargan listas completas de clientes, catálogos de administración ni oportunidades de todos los embudos para mostrar actividad reciente

#### Scenario: Búsqueda de cliente
- **WHEN** se escribe una búsqueda o se cambia de página del selector
- **THEN** se solicitan sólo resultados acotados para esa búsqueda y no todas las páginas existentes

### Requirement: Reutilización y actualización de datos
El sistema SHALL reutilizar consultas equivalentes vigentes y actualizar las vistas afectadas tras altas, edición, reasignación, transición y cierre. La reutilización SHALL distinguir sesión, filtros, página y embudo y no mostrar datos de un usuario anterior tras cambio de sesión.

#### Scenario: Volver a una pantalla
- **WHEN** se vuelve a una pantalla con datos equivalentes aún vigentes
- **THEN** se muestran sin duplicar solicitudes innecesarias y una mutación posterior invalida las vistas pertinentes

#### Scenario: Cambio de cuenta o responsable
- **WHEN** se cambia de cuenta o se reasigna una oportunidad
- **THEN** las vistas ya abiertas se ajustan al alcance actualizado y no conservan datos comerciales de la sesión anterior

### Requirement: Carga y errores independientes
El sistema SHALL mostrar estados de carga, vacío y error por sección y ofrecer reintento cuando corresponda. Una respuesta tardía de una búsqueda anterior no SHALL reemplazar la selección actual. SHALL evitar solicitudes con identificadores inválidos y presentar expiración de sesión sin ciclos de reintento de errores de autorización.

#### Scenario: Falla la actividad reciente
- **WHEN** carga el listado de oportunidades y falla la sección de actividades del panel
- **THEN** la información disponible permanece utilizable y la actividad ofrece su propio error y reintento

#### Scenario: Respuestas fuera de orden
- **WHEN** el usuario cambia rápidamente de empresa o búsqueda y responde después la consulta anterior
- **THEN** sólo se presentan las opciones correspondientes a la selección vigente

### Requirement: Verificación reproducible del rendimiento
La entrega SHALL incluir una comparación de carga inicial y navegación repetida con los mismos datos y condiciones, registrando tiempos, cantidad de solicitudes y volumen transferido. SHALL distinguir espera del servidor de descarga y renderizado; no SHALL atribuir mejoras a mediciones de ambientes o condiciones diferentes.

#### Scenario: Comparación antes y después
- **WHEN** se valida la mejora de carga
- **THEN** se documentan ruta, dataset, condiciones de caché y servidor, al menos tres corridas por condición y resultados antes/después, además de verificar que se eliminó la carga completa innecesaria
