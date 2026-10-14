# Spec Delta

## Purpose

Permitir que cada organización administre sus usuarios desde el CRM y que las operaciones respeten el rol, el tenant y el alcance comercial.

## ADDED Requirements

### Requirement: Administración de usuarios desde la interfaz
El sistema SHALL permitir a `ADMIN` listar, buscar, filtrar, crear, editar, desactivar y restablecer contraseñas de usuarios de su tenant, asignando `ADMIN`, `SELLER` o `SALES_MANAGER`. SHALL conservar las protecciones del último administrador activo y de desactivación propia.

#### Scenario: Alta y cambio inicial de contraseña
- **WHEN** un administrador crea un vendedor con contraseña temporal válida
- **THEN** el usuario puede ingresar y debe cambiar la contraseña antes de operar el CRM

#### Scenario: Desactivación o cambio sensible
- **WHEN** se desactiva un usuario o se restablece su contraseña
- **THEN** sus tokens anteriores dejan de habilitar operaciones y su autoría histórica sigue disponible

#### Scenario: Protección del acceso administrativo
- **WHEN** se intenta desactivar el usuario actual o quitar al último administrador activo
- **THEN** se rechaza la operación con un mensaje utilizable y se conserva el acceso administrativo

### Requirement: Permisos de navegación y operación
El sistema SHALL mostrar administración de usuarios, oferta, embudos y catálogos sólo a `ADMIN`. El backend SHALL rechazar operaciones administrativas de otros roles con `403`, aunque se invoquen directamente. `SALES_MANAGER` SHALL consultar el equipo completo y reasignar oportunidades; `SELLER` SHALL conservar el alcance de DP-02.

#### Scenario: URL administrativa directa
- **WHEN** un vendedor o responsable comercial abre una ruta administrativa o invoca su escritura HTTP
- **THEN** la interfaz presenta acceso denegado y el backend devuelve `403` sin ejecutar la escritura

#### Scenario: Supervisión comercial
- **WHEN** el responsable comercial consulta oportunidades y tablero
- **THEN** ve las del equipo de su tenant y dispone de reasignación sin obtener permisos de administración de usuarios

### Requirement: Aislamiento y credenciales seguras
El sistema SHALL resolver tenant y actor de la sesión, filtrar lecturas y escrituras y no aceptar que el request los reemplace. Los recursos ajenos SHALL responder `404` y los listados SHALL omitirlos. SHALL conservar hash seguro, política de contraseñas, revocación y ausencia de secretos en respuestas y logs.

#### Scenario: Usuario de otra organización
- **WHEN** un administrador consulta o intenta editar por ID un usuario de otro tenant
- **THEN** recibe `404` y el usuario ajeno no aparece en listados ni cambia

#### Scenario: Credenciales en administración
- **WHEN** se consulta una ficha de usuario o falla un inicio de sesión
- **THEN** no se expone contraseña ni hash y el error de login no revela si existe la cuenta
