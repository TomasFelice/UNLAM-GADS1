# embudos-comerciales Specification

## Purpose

Permitir que una organización gestione varios procesos comerciales con embudos y etapas propios, sin perder oportunidades ni recorridos anteriores.

## Requirements

### Requirement: Varios embudos administrables por organización
El sistema SHALL permitir a `ADMIN` crear, listar, editar, activar y desactivar embudos con nombre y etapas propias. SHALL mantener un único embudo predeterminado activo por tenant. Los demás roles SHALL consultarlos sin configurarlos. La baja SHALL ser lógica; un embudo inactivo conserva sus oportunidades y no admite altas nuevas.

#### Scenario: Dos procesos comerciales
- **WHEN** el administrador crea dos embudos activos con etapas diferentes
- **THEN** cada uno puede seleccionarse para crear oportunidades y consultar su tablero sin mezclar etapas

#### Scenario: Baja del predeterminado
- **WHEN** se intenta desactivar el embudo predeterminado sin elegir otro activo
- **THEN** se rechaza con `409`; después de reemplazarlo puede desactivarse sin borrar su historial

### Requirement: Configuración consistente de etapas
El sistema SHALL permitir crear, renombrar, ordenar y desactivar etapas por embudo. Cada embudo activo SHALL tener al menos una etapa activa `OPEN`, una `WON` y una `LOST`, con posiciones únicas dentro del embudo. El tipo de una etapa utilizada SHALL ser inmutable; desactivarla no oculta oportunidades ni históricos anteriores.

#### Scenario: Reordenamiento completo
- **WHEN** el administrador intercambia posiciones de etapas
- **THEN** se aplica el orden completo de forma atómica sin posiciones duplicadas ni pérdida de referencias

#### Scenario: Etapa utilizada
- **WHEN** se desactiva una etapa con oportunidades o se intenta cambiar su tipo
- **THEN** la desactivación conserva las fichas y el historial, el tipo no cambia y la etapa deja de ser destino de nuevas transiciones

#### Scenario: Embudo incompleto
- **WHEN** se intenta activar un embudo sin alguna de las clases requeridas o quitar su última etapa activa de una clase
- **THEN** se rechaza con `409` y se explica qué configuración falta

### Requirement: Coherencia entre oportunidad y embudo
Cada oportunidad SHALL pertenecer a un embudo y usar únicamente sus etapas. Las transiciones y cierres SHALL validar esta pertenencia además de tenant, alcance y estado. El embudo SHALL quedar fijo tras el alta en este alcance; cambiarlo por edición genérica se rechaza.

#### Scenario: Etapa de otro embudo
- **WHEN** se crea, cambia de etapa, cierra o reabre usando una etapa de otro embudo del tenant
- **THEN** se devuelve `409` sin cambiar la oportunidad ni agregar historial

#### Scenario: Intento de transferencia
- **WHEN** se intenta reemplazar el embudo de una oportunidad existente
- **THEN** se rechaza explícitamente sin alterar su proceso ni su historial

### Requirement: Embudo inicial y plantillas independientes
El sistema SHALL conservar las etapas, oportunidades e IDs existentes en un embudo corporativo inicial por tenant. SHALL ofrecer plantillas opcionales de eventos corporativos y de proceso breve que creen copias editables independientes. No SHALL generar varios embudos no solicitados ni compartir etapas mutables entre organizaciones.

#### Scenario: Actualización con datos existentes
- **WHEN** se instala la nueva versión sobre V1 con registros comerciales
- **THEN** cada tenant obtiene un embudo inicial, conserva todos sus IDs y relaciones y puede reconstruir el mismo recorrido anterior

#### Scenario: Crear desde plantilla
- **WHEN** un administrador copia una plantilla y renombra una etapa
- **THEN** el cambio afecta sólo a su nuevo embudo; la plantilla y los otros embudos permanecen independientes

### Requirement: Tablero y filtros por embudo
El sistema SHALL permitir seleccionar embudo, conservarlo en la URL y filtrar por responsable, etapa, estado, origen, cliente, salón y rango del evento. SHALL usar el tipo de etapa, no su nombre, para decidir acciones. Sin selección explícita SHALL utilizar el predeterminado; las vistas históricas SHALL permitir consultar embudos inactivos.

#### Scenario: Etapas renombradas y filtro de cierre
- **WHEN** se renombra una etapa cerrada o se filtran ganadas/perdidas en su embudo
- **THEN** las oportunidades aparecen en las columnas correspondientes y las acciones respetan su tipo y estado

#### Scenario: Acceso de vendedor
- **WHEN** un vendedor consulta un tablero o manipula filtros e IDs de otro tenant
- **THEN** sólo ve oportunidades propias del tenant y los recursos ajenos responden `404`
