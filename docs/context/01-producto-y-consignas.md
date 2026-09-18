# Producto y consignas — destilado de `docs/consignas/*.pdf`

Fuente: `Consigna_Trabajo_Practico.pdf`, `Definiciones-Generales.pdf`,
`Modulos_Principales.pdf`, `Entregas-CRM.pdf`. Los PDF son la **autoridad funcional**;
este documento es su resumen compartido para cualquier asistente.

## Tipo de CRM elegido

**CRM especializado**: salones de eventos corporativos. La consigna exige que la
especialización produzca **cambios reales en el modelo de datos y en el proceso
comercial**, no sólo en nombres, colores o textos. Nuestra especialización se apoya en:

- `Venue` (salón) con **capacidad**, tarifa y estado.
- **Datos del evento** en la oportunidad (fecha/rango, cantidad de personas).
- **Reserva del salón**: una oportunidad ganada bloquea el salón; no puede haber dos
  rangos superpuestos para el mismo salón. Validación en aplicación + restricción física
  en PostgreSQL (exclusión GiST).
- Validación de **capacidad** del salón contra la cantidad de asistentes.
- Nicho declarado: eventos de hasta ~50 personas (alcance exacto en DP-08).

## Roles (mínimo exigido: 3)

| Rol | Puede |
|---|---|
| `ADMIN` | Crear/modificar usuarios, asignar roles, configurar etapas, tipos de actividad, motivos de pérdida y orígenes. Ve toda la información. Asigna y reasigna contactos y oportunidades. |
| `SELLER` (vendedor) | Registra empresas y contactos. Consulta **sus** clientes asignados. Crea/actualiza oportunidades, cambia de etapa, registra actividades, consulta historial, marca ganada/perdida. |
| `SALES_MANAGER` (responsable comercial) | Consulta la información de **todo el equipo**. Supervisa oportunidades abiertas, ve el embudo, consulta historial, asigna y reasigna oportunidades, revisa ganadas y perdidas. |

## Entidades y datos mínimos exigidos

### Company (Empresa)
Razón social / nombre comercial · CUIT (si corresponde) · industria o actividad · email ·
teléfono · dirección · sitio web (si corresponde) · **estado** · responsable comercial ·
origen · observaciones.

### Contact (Contacto)
Nombre · apellido · documento (si corresponde) · cargo · email · teléfono ·
empresa relacionada (**opcional**: puede ser cliente individual) · responsable comercial ·
estado · origen · observaciones.

### Estados de Company/Contact (mínimo)
`POTENCIAL`, `CLIENTE`, `INACTIVO`, `NO_CONTACTAR`.
La consigna define la **baja lógica como cambio de estado**: "cuando una empresa o
contacto deje de utilizarse, deberá modificarse su estado para conservar sus
oportunidades y actividades anteriores".

### Product / Service (Producto o servicio)
Exigido explícitamente por la consigna, tanto en E1 (precargado) como en la entrega final
(gestión completa). En nuestro dominio se materializa como el **salón** y/o servicios
asociados — ver decisión DT-03 en `03-decisiones-tecnicas.md`.

### Opportunity (Oportunidad)
Título · empresa **o** contacto relacionado · responsable comercial · producto/servicio ·
valor estimado · **etapa actual** · probabilidad de cierre (opcional) · fecha estimada de
cierre · fecha real de cierre · origen · **estado** · observaciones · motivo de pérdida
(si corresponde). Más los atributos del evento (fecha del evento, cantidad de personas).

**Estados**: `ABIERTA` / `GANADA` / `PERDIDA`.

### Stage (Etapa comercial)
Tabla configurable (la entrega final exige "embudo comercial **configurable**"), con
**orden** y marca de etapa abierta/ganada/perdida. Ejemplo genérico de la consigna:
Nuevo contacto → Contactado → Necesidad relevada → Propuesta enviada → Negociación →
Ganada → Perdida. Nuestras etapas concretas: DP-06.

### StageHistory (Historial de etapas)
Oportunidad · etapa anterior · nueva etapa · fecha y hora · usuario que hizo el cambio ·
observación (si corresponde). **Append-only.** No alcanza con guardar la etapa actual.
Debe permitir responder: en qué etapa empezó, qué etapas atravesó, cuándo ocurrió cada
cambio, quién lo hizo, y cuándo y cómo finalizó.

### Activity (Actividad)
Representa una interacción **que ya ocurrió** (hecho histórico, NO una tarea pendiente).
Tipo · fecha y hora · usuario que la registró · empresa o contacto relacionado ·
oportunidad relacionada (si corresponde) · descripción · resultado.

Tipos mínimos: llamada, correo electrónico, mensaje, reunión presencial, reunión virtual,
demostración, envío de propuesta, nota interna, otro configurable.

Se muestran en orden cronológico en el detalle de contacto, de empresa y de oportunidad.

### Catálogos configurables (ADMIN)
`Stage`, `ActivityType`, `Origin` (origen comercial), `LossReason` (motivo de pérdida).

## Reglas generales de negocio (las 15 de la consigna)

1. Toda oportunidad tiene un responsable.
2. Toda oportunidad está asociada, como mínimo, a una empresa **o** un contacto.
3. Toda oportunidad tiene una etapa actual.
4. Una oportunidad abierta debe estar en una etapa abierta.
5. Una oportunidad ganada registra fecha de cierre y valor final.
6. Una oportunidad perdida registra fecha de cierre y motivo de pérdida.
7. Cada cambio de etapa se conserva en el historial.
8. Cada actividad registra el usuario y la fecha.
9. Las actividades se relacionan con una empresa, contacto u oportunidad.
10. Los registros con historial comercial **no se eliminan físicamente**.
11. Los vendedores sólo acceden a la información permitida por su rol.
12. Los permisos se validan en el **backend**, no sólo en el frontend.
13. Las contraseñas se almacenan con un mecanismo seguro.
14. Una oportunidad cerrada no se modifica sin autorización.
15. Los cambios importantes permiten identificar al usuario que los realizó.

### Reglas adicionales del cambio de etapa
- Una única etapa actual por oportunidad.
- La etapa debe ser compatible con el estado de la oportunidad.
- Una oportunidad abierta no puede estar en etapa ganada o perdida.
- Una oportunidad cerrada no puede volver a una etapa abierta **sin autorización** (DP-01).
- Si se modifica una oportunidad cerrada, el cambio queda registrado.

## Casos de uso mínimos (17)

1. Iniciar sesión · 2. Crear/modificar usuario · 3. Crear/modificar empresa ·
4. Crear/modificar contacto · 5. Detalle de empresa · 6. Detalle de contacto ·
7. Crear oportunidad · 8. Asignar oportunidad a un vendedor · 9. Modificar oportunidad ·
10. Cambiar de etapa · 11. Registrar actividad · 12. Consultar historial comercial ·
13. Marcar ganada · 14. Marcar perdida · 15. Consultar embudo · 16. Buscar y filtrar
empresas, contactos y oportunidades · 17. Funcionalidad de IA (opcional).

## Fuera de alcance (explícito en la consigna)

Gestión de tareas · agenda comercial · recordatorios y notificaciones · indicadores y
estadísticas · exportación · integraciones · API pública para terceros · importación
automática · facturación · pagos · contabilidad · stock · campañas de marketing · envío de
mails desde el CRM · integración con WhatsApp · **soporte para múltiples organizaciones**.

> ⚠️ El último punto contradice la multi-tenancy que proponen los `docs/arquitectura/`.
> Resolución en DT-01 (`03-decisiones-tecnicas.md`).

## Orden obligatorio de desarrollo (consigna)

1. Análisis y elección del tipo de CRM · 2. Usuarios, alcance y requerimientos ·
3. Diseño de pantallas y modelo de datos · 4. **Usuarios, roles y permisos** ·
5. **Empresas y contactos** · 6. **Productos o servicios** · 7. **Oportunidades y embudo** ·
8. **Actividades e historial** · 9. Pruebas y corrección · 10. IA · 11. Pruebas finales.

## Entrega 1 — 24/09

Requerido: login funcional con al menos un usuario habilitado · crear/modificar empresas y
contactos con sus listados y detalles, y relacionarlos · productos/servicios **precargados**
· crear/modificar oportunidades relacionadas a empresa o contacto, con responsable y
producto/servicio, con listado y detalle · embudo agrupado por etapa con cambio de etapa
persistido (**etapas precargadas**, no hace falta configurarlas desde el sistema).

Demo esperada: login → registrar empresa y contacto → crear oportunidad → verla en el
embudo → cambiarla de etapa → comprobar que quedó guardada.

**No requerido en E1**: gestión completa de roles y permisos · actividades e historial
comercial · historial de cambios de etapa · configuraciones generales · cierre completo de
oportunidades · IA.

## Entrega final — 12/11

Gestión de usuarios · 3 roles con permisos · gestión completa de empresas y contactos ·
gestión de productos/servicios · asignación de responsables · gestión completa de
oportunidades · embudo configurable · cambio de etapas con historial · registro de
actividades · historial comercial de empresas, contactos y oportunidades · cierre
ganado/perdido · motivos de pérdida · gestión de etapas, tipos de actividad, orígenes y
motivos de pérdida · búsqueda, filtros y paginación · **adaptación real a la industria**.

> La consigna aclara que no se exige documentación adicional ni una arquitectura
> tecnológica determinada: **se evalúa el funcionamiento integral del producto**.
