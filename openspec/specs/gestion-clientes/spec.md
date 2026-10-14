# gestion-clientes Specification

## Purpose

Completar la gestión de empresas y contactos preservando datos comerciales, responsables y vínculos históricos dentro del alcance autorizado.

## Requirements

### Requirement: Datos completos y validaciones vigentes
El sistema SHALL permitir consultar y editar los datos de empresa/contacto exigidos por la consigna y DP-03, incluido origen comercial. SHALL validar CUIT opcional con dígito verificador y unicidad por tenant, documento opcional único, email y URL por formato y normalizar blancos opcionales. Las validaciones autoritativas SHALL ejecutarse en backend.

#### Scenario: Origen de empresa
- **WHEN** se crea o edita una empresa eligiendo un origen activo del tenant
- **THEN** el origen se persiste y aparece al consultar el detalle y filtrar el listado

#### Scenario: CUIT inválido o equivalente duplicado
- **WHEN** se envía un CUIT con dígito incorrecto o uno existente con otra presentación de guiones
- **THEN** se rechaza respectivamente como entrada inválida o conflicto sin crear un duplicado

#### Scenario: Datos opcionales
- **WHEN** se guardan email, teléfono o sitio web vacíos
- **THEN** quedan sin valor; un email o sitio web no vacío con formato inválido se rechaza

### Requirement: Responsable y alcance de clientes
El sistema SHALL aplicar DP-02: responsable obligatorio, herencia al crear un contacto de empresa y reemplazo autorizado para `ADMIN`/`SALES_MANAGER`. Un vendedor SHALL leer clientes asignados o relacionados con oportunidades propias y editar sólo los asignados directamente. La interfaz SHALL comunicar la asignación en vocabulario comercial.

#### Scenario: Cliente visible sólo por oportunidad
- **WHEN** un vendedor abre un cliente asignado a otro usuario pero relacionado con una oportunidad propia
- **THEN** puede consultarlo y su historial permitido, pero no editarlo ni cambiar su responsable

#### Scenario: Asignación autorizada
- **WHEN** un administrador o responsable comercial asigna un cliente a un usuario activo del tenant
- **THEN** el responsable queda persistido y el alcance posterior de cada vendedor se actualiza

### Requirement: Baja lógica e historia comercial
El sistema SHALL conservar empresas y contactos al cambiar a `INACTIVO` o `NO_CONTACTAR`, junto con oportunidades, actividades y autores históricos. SHALL permitir navegar el historial completo mediante paginación y mantener filtros y orden en listados sin exposición entre tenants.

#### Scenario: Baja con relaciones existentes
- **WHEN** se inactiva una empresa o contacto con oportunidades y actividades
- **THEN** sus relaciones continúan consultables por usuarios autorizados y no se eliminan registros físicos

#### Scenario: Historial con varias páginas
- **WHEN** un cliente tiene más actividades u oportunidades que el tamaño de una página
- **THEN** el usuario puede navegar hasta los registros restantes sin repetir ni omitir elementos por la paginación
