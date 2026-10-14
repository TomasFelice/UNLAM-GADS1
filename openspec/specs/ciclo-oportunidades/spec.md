# ciclo-oportunidades Specification

## Purpose

Completar el recorrido comercial de una oportunidad con responsables, cierres e historial confiables y reservas de salón sin conflictos simultáneos.

## Requirements

### Requirement: Reasignación explícita de responsable
El sistema SHALL permitir a `ADMIN` y `SALES_MANAGER` reasignar una oportunidad abierta a un usuario activo del tenant mediante una operación explícita. SHALL registrar actor y fecha, actualizar su alcance y conservar historial y actividades. `SELLER` SHALL recibir `403` al intentar reasignar y una oportunidad cerrada SHALL exigir reapertura antes de modificarse.

#### Scenario: Redistribución de una negociación
- **WHEN** el responsable comercial reasigna una oportunidad abierta de un vendedor a otro
- **THEN** el nuevo vendedor puede consultarla y el anterior deja de verla, sin que desaparezcan sus actividades ni autores

#### Scenario: Destino inactivo o ajeno
- **WHEN** se reasigna a un usuario inactivo o de otro tenant
- **THEN** se rechaza la operación sin cambiar responsable; el ID ajeno responde `404`

### Requirement: Cierre ganado y perdido
El sistema SHALL cerrar oportunidades abiertas mediante operaciones específicas. Ganar SHALL exigir etapa activa `WON` del embudo y valor final no negativo; perder SHALL exigir etapa activa `LOST` y motivo de pérdida activo. SHALL registrar fecha real del servidor, actor e historial en la misma transacción y mostrar los datos de cierre en la interfaz.

#### Scenario: Ganar una oportunidad
- **WHEN** se confirma un cierre válido con salón disponible y capacidad suficiente
- **THEN** se persisten estado `GANADA`, etapa, fecha, valor final y registro histórico como una sola operación

#### Scenario: Perder una oportunidad
- **WHEN** se confirma una pérdida con motivo válido
- **THEN** se persisten estado `PERDIDA`, etapa, fecha, motivo e historial sin bloquear el salón

#### Scenario: Datos insuficientes o fallo de historial
- **WHEN** faltan datos obligatorios, la etapa es incompatible o falla la escritura histórica
- **THEN** se rechaza la solicitud y no queda un cierre parcial; una transición incompatible devuelve `409`

### Requirement: Reapertura autorizada y conservación de cierres
El sistema SHALL bloquear edición genérica y cambios libres de etapa sobre oportunidades cerradas con `409`. Sólo `ADMIN`/`SALES_MANAGER` SHALL reabrir con nota obligatoria y etapa activa `OPEN` del mismo embudo activo. SHALL limpiar datos del cierre vigente, conservar los cierres previos en el historial y liberar una reserva ganada atómicamente.

#### Scenario: Reabrir una ganada
- **WHEN** un administrador reabre una ganada con nota y etapa válida
- **THEN** queda `ABIERTA` sin fecha, valor final ni motivo de cierre vigentes, se conserva el cierre anterior y su rango deja de bloquear el salón

#### Scenario: Reapertura no autorizada
- **WHEN** un vendedor solicita reapertura o se reabre una oportunidad ya abierta
- **THEN** se devuelve respectivamente `403` o `409` sin agregar historial ni modificar datos

### Requirement: Capacidad y ausencia de reservas superpuestas
El sistema SHALL validar asistentes contra capacidad y reservar sólo al ganar. Dos ganadas del mismo tenant y salón no SHALL superponerse en `[inicio, fin)`, aunque pertenezcan a embudos diferentes. Exceder capacidad SHALL devolver `422`; una reserva superpuesta SHALL devolver `409` con mensaje de disponibilidad.

#### Scenario: Confirmaciones simultáneas
- **WHEN** se confirman simultáneamente dos oportunidades distintas con el mismo salón y rangos superpuestos
- **THEN** exactamente una gana y la otra recibe `409`, conserva su estado anterior y no agrega historial de cierre

#### Scenario: Rangos contiguos y embudos distintos
- **WHEN** un evento empieza exactamente al terminar otro
- **THEN** ambos pueden ganarse; cambiar de embudo no permite superponer rangos del mismo salón

#### Scenario: Capacidad modificada
- **WHEN** la capacidad del salón se reduce por debajo de los asistentes antes del cierre
- **THEN** la confirmación vuelve a validar la capacidad y devuelve `422`

### Requirement: Conflictos de escritura y mensajes utilizables
El sistema SHALL evitar que escrituras simultáneas sobre la misma oportunidad sobrescriban cambios. SHALL devolver `409` en conflictos de versión y presentar recuperación mediante actualización de datos, sin duplicar cierres ni historial. Los errores no SHALL revelar SQL, stacktraces ni datos ajenos.

#### Scenario: Dos cambios sobre la misma oportunidad
- **WHEN** dos transacciones parten de la misma versión e intentan modificarla
- **THEN** sólo una actualización se conserva y la otra recibe `409` con indicación de recargar
