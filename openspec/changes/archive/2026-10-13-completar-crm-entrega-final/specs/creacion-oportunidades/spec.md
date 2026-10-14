# Spec Delta

## Purpose

Reducir el esfuerzo para crear oportunidades manteniendo datos obligatorios, relaciones coherentes y una interfaz comercial accesible.

## ADDED Requirements

### Requirement: Alta rápida desde un cliente
El sistema SHALL ofrecer alta breve desde empresa o contacto con cliente preseleccionado, responsable heredado según DP-02 y campos obligatorios: título, embudo, etapa inicial abierta, salón, tipo de evento, inicio, fin y asistentes. SHALL dejar opcionales para edición posterior sin inventar fechas, capacidad ni crear registros incompletos.

#### Scenario: Desde contacto corporativo
- **WHEN** se elige Nueva oportunidad en un contacto con empresa
- **THEN** se precargan contacto y empresa coherentes, se propone su responsable y se puede guardar completando sólo los datos obligatorios

#### Scenario: Desde contacto individual
- **WHEN** se crea una oportunidad desde un contacto sin empresa
- **THEN** se conserva ese contacto como cliente y no se exige crear una empresa

#### Scenario: Cancelar o completar más datos
- **WHEN** se cancela el alta breve o se despliegan campos opcionales
- **THEN** cancelar no deja registros parciales y ampliar el formulario conserva lo ya ingresado

### Requirement: Selección coherente y completa de clientes
El sistema SHALL buscar empresas y contactos con paginación, incluyendo registros posteriores a los primeros 20. Al elegir empresa SHALL mostrar sólo sus contactos autorizados; al cambiar empresa SHALL limpiar contactos incompatibles. Al partir de contacto SHALL cargarlo por ID y derivar su empresa, sin depender de que esté en la primera página.

#### Scenario: Empresa con contactos en páginas posteriores
- **WHEN** se selecciona una empresa y se busca un contacto que no está entre los primeros 20
- **THEN** puede seleccionarse y no aparecen contactos de otra empresa o fuera del alcance

#### Scenario: Cambio de empresa
- **WHEN** se cambia la empresa después de elegir un contacto incompatible
- **THEN** se limpia la selección y el backend también rechaza una combinación contradictoria enviada directamente

### Requirement: Formulario de oportunidad utilizable
El sistema SHALL presentar los servicios adicionales con etiquetas, selección múltiple, foco visible y separación suficiente; conservar selecciones históricas permitidas y mostrar errores junto al campo y en un resumen accesible. SHALL separar las acciones de cierre, indicar envíos en curso y evitar confirmaciones duplicadas.

#### Scenario: Servicios y acciones a 390 px
- **WHEN** se utiliza el formulario y el detalle a 390 px de ancho con teclado
- **THEN** los servicios pueden seleccionarse, los botones no se superponen y la página no genera scroll horizontal global

#### Scenario: Error de reserva
- **WHEN** un cierre devuelve `409` por disponibilidad
- **THEN** se conserva la información ingresada y se muestra un mensaje comprensible con posibilidad de revisar y reintentar

### Requirement: Textos comerciales y fechas
El sistema SHALL reemplazar las alusiones a backend/API y persistencia UTC señaladas en alta de empresa, panel y oportunidad por lenguaje comercial. SHALL seguir interpretando y mostrando las fechas en la zona de negocio sin exigir al usuario conocer su almacenamiento. Superar 50 asistentes SHALL advertir sin bloquear por sí solo.

#### Scenario: Textos de ayuda
- **WHEN** se abre alta de empresa, panel o edición de oportunidad
- **THEN** no aparecen las aclaraciones técnicas del PDF y la asignación automática se explica según su efecto comercial

#### Scenario: Evento de más de 50 asistentes
- **WHEN** se ingresan 60 asistentes para un salón con capacidad 80
- **THEN** se muestra advertencia y puede guardarse; exceder la capacidad real sigue bloqueado por backend
