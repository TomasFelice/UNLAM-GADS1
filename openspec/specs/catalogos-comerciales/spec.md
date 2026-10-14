# catalogos-comerciales Specification

## Purpose

Permitir configurar los maestros comerciales de cada organización conservando su significado y disponibilidad en el historial del CRM.

## Requirements

### Requirement: Catálogos administrables
El sistema SHALL permitir a `ADMIN` crear, listar, buscar, editar, activar y desactivar orígenes, motivos de pérdida, tipos de actividad y tipos de evento del tenant. SHALL rechazar nombres vacíos y duplicados del mismo catálogo y tenant y ofrecer paginación y orden validado en administración.

#### Scenario: Nuevo motivo de pérdida
- **WHEN** un administrador crea un motivo válido
- **THEN** puede seleccionarse en un cierre perdido y aparece en su detalle e historial autorizado

#### Scenario: Duplicado o rol insuficiente
- **WHEN** se repite un nombre del mismo catálogo o un vendedor intenta modificarlo
- **THEN** se devuelve respectivamente conflicto `409` o permiso insuficiente `403` sin escritura

### Requirement: Asociaciones activas e historia conservada
El sistema SHALL aceptar nuevas asociaciones sólo a entradas activas del tenant y resolver las inactivas ya asociadas en detalles e históricos. SHALL permitir conservar referencias anteriores en una edición permitida sin ofrecerlas como nuevas selecciones. No SHALL eliminar físicamente entradas con historia comercial.

#### Scenario: Origen inactivo en una empresa existente
- **WHEN** se desactiva un origen y se edita otro dato de una empresa que lo utiliza
- **THEN** el origen anterior sigue visible y puede conservarse, pero no seleccionarse en una empresa nueva

#### Scenario: Referencia enviada directamente
- **WHEN** se envía por HTTP un origen, tipo o motivo ajeno al tenant o inactivo para una nueva asociación
- **THEN** se rechaza sin crear referencias cruzadas; el ID ajeno devuelve `404`

### Requirement: Tipos mínimos y tipo de evento obligatorio
El sistema SHALL disponer inicialmente de llamada, correo, mensaje, reunión presencial/virtual, demostración, envío de propuesta, nota interna y otro configurable. Cada oportunidad nueva SHALL referenciar un tipo de evento activo; la administración SHALL incluir este catálogo además de los cuatro mencionados en el checklist final.

#### Scenario: Registro de interacción y evento
- **WHEN** un usuario registra una actividad o crea una oportunidad
- **THEN** puede elegir los tipos aplicables y no puede guardar la oportunidad sin tipo de evento válido
