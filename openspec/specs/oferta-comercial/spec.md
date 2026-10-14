# oferta-comercial Specification

## Purpose

Administrar la oferta de salones corporativos y servicios adicionales sin perder las referencias utilizadas en negociaciones anteriores.

## Requirements

### Requirement: Administración de salones y servicios
El sistema SHALL permitir a `ADMIN` crear, consultar, editar y dar de baja lógica salones y servicios de su tenant. Un salón SHALL incluir nombre, capacidad positiva, tarifa, ubicación, descripción, equipamiento y estado `DISPONIBLE`, `MANTENIMIENTO` o `INACTIVO`; un servicio SHALL incluir nombre, descripción, precio y actividad.

#### Scenario: Salón nuevo y mantenimiento
- **WHEN** un administrador crea un salón válido y luego lo cambia a mantenimiento
- **THEN** queda consultable, no se ofrece en nuevas asociaciones y conserva sus oportunidades anteriores

#### Scenario: Datos inválidos
- **WHEN** se intenta guardar capacidad no positiva o un precio negativo
- **THEN** se rechaza la solicitud y se señalan los campos sin alterar la oferta persistida

### Requirement: Conservación de asociaciones anteriores
El sistema SHALL rechazar nuevas asociaciones a salones no disponibles o servicios inactivos. SHALL permitir conservar una asociación previa al editar otros campos, identificar su estado y resolverla en históricos. Una confirmación ganada SHALL volver a exigir salón disponible y capacidad suficiente.

#### Scenario: Editar una oportunidad vinculada a un salón inactivo
- **WHEN** se modifica una observación conservando el salón previamente asociado
- **THEN** la edición abierta permitida conserva la relación histórica; seleccionar otro salón inactivo se rechaza

#### Scenario: Servicio desactivado
- **WHEN** se desactiva un servicio utilizado por una oportunidad
- **THEN** sigue identificado en el detalle anterior y no puede agregarse a otra oportunidad

### Requirement: Consulta administrativa y aislamiento
El sistema SHALL ofrecer listados administrativos con búsqueda, estado, paginación de tamaño 20 y máximo 100, y orden validado. Las escrituras SHALL exigir `ADMIN`; los IDs ajenos SHALL responder `404` y no aparecer en listados. Las lecturas comerciales SHALL seguir disponibles a los tres roles.

#### Scenario: Rol insuficiente y tenant ajeno
- **WHEN** un vendedor intenta administrar oferta o un administrador consulta un salón de otro tenant
- **THEN** recibe respectivamente `403` o `404` sin mutación ni datos ajenos

#### Scenario: Oferta fuera de la primera página
- **WHEN** hay más de 20 salones o servicios y se busca uno posterior
- **THEN** puede localizarse y seleccionarse sin descargar todo el catálogo comercial
