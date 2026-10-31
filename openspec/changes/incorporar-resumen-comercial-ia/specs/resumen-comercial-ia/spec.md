# Spec Delta

## Purpose

Ayudar a retomar una negociación mediante una síntesis de información comercial registrada, con fuentes verificables, permisos existentes y fallos de IA aislados del uso normal del CRM.

## ADDED Requirements

### Requirement: Generación automática desde la oportunidad

El sistema SHALL solicitar automáticamente el resumen al abrir el detalle de una oportunidad autorizada, abierta o cerrada, sin requerir un botón inicial ni bloquear las otras secciones. SHALL generar si no hay resultado reutilizable ni solicitud equivalente en curso. El endpoint SHALL aceptar sólo la identidad de la oportunidad, obtener el contexto desde el backend y no modificar registros comerciales.

#### Scenario: Primera entrada al detalle
- **WHEN** el usuario entra a una oportunidad y se confirma su lectura autorizada, sin resumen reutilizable ni solicitud en curso
- **THEN** comienza una generación sin clic previo y se muestra su carga independiente, sin crear actividades, cambiar etapas ni modificar la oportunidad

#### Scenario: Ruta distinta o detalle no autorizado
- **WHEN** se consulta un listado, tablero u otra ruta, o el detalle resulta inválido o no autorizado
- **THEN** no se solicita generar el resumen de esa oportunidad

### Requirement: Reutilización persistida sin generaciones duplicadas

El sistema SHALL persistir el último resumen exitoso por tenant y oportunidad con su huella de contexto y versión de prompt. SHALL autorizar y consultar vigencia al volver al detalle, incluso tras recarga o reinicio. SHALL reutilizar el resultado sin TTL mientras coincidan huella y prompt; sólo generará si falta o cambió alguno de ellos. SHALL deduplicar solicitudes en curso y no consultar por foco, reconexión ni temporizadores. La caché del navegador SHALL eliminarse al cambiar de sesión.

#### Scenario: Remontaje y retorno rápido
- **WHEN** se remonta el panel o se vuelve al mismo detalle con un resumen persistido, acceso vigente y contexto sin cambios
- **THEN** se comparte la solicitud pendiente o se reutiliza el resumen, sin otra llamada a Gemini

#### Scenario: Resumen ausente o contexto actualizado
- **WHEN** se entra al detalle sin resumen persistido o con un contexto diferente al guardado
- **THEN** se solicita automáticamente una generación nueva y no se presenta el anterior como actualizado

#### Scenario: Foco y reconexión
- **WHEN** se vuelve a enfocar la ventana, se reconecta la red o transcurren cinco minutos con el detalle montado
- **THEN** no se dispara otra generación por esos eventos; se conserva el resultado sin consumir proveedor

### Requirement: Alcance comercial y aislamiento

El sistema SHALL autorizar antes de consultar al proveedor y antes de devolver el resultado. ADMIN y SALES_MANAGER SHALL resumir oportunidades de su tenant; SELLER sólo las propias. Un ID ajeno o fuera del alcance SHALL responder `404` sin llamar a Gemini. Un usuario no autenticado SHALL recibir `401`.

#### Scenario: Oportunidad de otro tenant o vendedor
- **WHEN** se solicita por ID directo resumir una oportunidad no visible
- **THEN** se responde `404` y se comprueba que el proveedor no recibió ninguna solicitud

#### Scenario: Cambia la asignación durante la generación
- **WHEN** un vendedor pierde acceso a la oportunidad mientras responde Gemini
- **THEN** se descarta el resultado y se responde `404` sin revelar resumen ni fuentes

#### Scenario: Supervisión y acceso anónimo
- **WHEN** supervisión consulta una oportunidad del tenant o una solicitud llega sin autenticación
- **THEN** la primera conserva su lectura autorizada y la segunda recibe `401` sin consumir cuota de IA

### Requirement: Contexto registrado y acotado

El sistema SHALL analizar datos actuales de la oportunidad, sus observaciones, sus actividades y su historial de etapas. SHALL excluir actividades generales del cliente y de otras oportunidades. SHALL imponer límites de cantidad y texto con selección temporal determinista y distinguir fechas del hecho, registro y transición.

#### Scenario: Cliente compartido
- **WHEN** existen notas generales del cliente y actividades de otras oportunidades del mismo cliente
- **THEN** no se incorporan al contexto ni a las fuentes de esta generación

#### Scenario: Empates temporales e historial extenso
- **WHEN** varias actividades comparten fecha o el historial excede los límites configurados
- **THEN** se aplica un desempate estable por ID, se analiza sólo el subconjunto permitido y no se descarga todo el historial para recortarlo después

### Requirement: Cobertura en la API y ausencia de datos

El resultado SHALL indicar fecha de generación, instante del contexto, cantidades analizadas y totales disponibles de actividades y etapas, rango temporal y recortes de texto o registros. La interfaz SHALL decir «No consta en la información analizada» cuando corresponda y SHALL evitar afirmar que se revisó toda la historia. Los metadatos de cobertura y fechas se conservan en la API, sin mostrarse en el panel.

#### Scenario: Historial recortado
- **WHEN** sólo se analizan las actividades más recientes o un texto se recorta
- **THEN** la respuesta de la API informa la cobertura y conserva exactamente los fragmentos utilizados

#### Scenario: Sin actividades
- **WHEN** la oportunidad no tiene actividades registradas
- **THEN** el resumen se limita a los datos y etapas disponibles y no inventa conversaciones ni acuerdos

### Requirement: Síntesis comercial fundamentada

El resumen SHALL organizarse en necesidad del cliente, situación actual, acuerdos y objeciones, e información pendiente o contradictoria. SHALL distinguir hechos registrados de asuntos por confirmar. SHALL usar el estado estructurado como situación actual y no inventar precios, acuerdos, condiciones ni disponibilidad.

#### Scenario: Necesidades contradictorias
- **WHEN** dos actividades registran preferencias incompatibles sin una resolución explícita
- **THEN** la evaluación del resultado verifica que ambas se reconozcan como contradicción por confirmar y no se elija una arbitrariamente

#### Scenario: Cierre y reapertura
- **WHEN** una oportunidad tiene un cierre histórico y una reapertura posterior
- **THEN** el resumen distingue ese cierre del estado actual y no presenta la negociación como todavía cerrada

### Requirement: Fuentes verificables y respuesta validada

Cada afirmación del modelo SHALL referenciar fuentes del contexto enviado. El backend SHALL validar esquema, límites y pertenencia de referencias, y completar las fuentes desde sus propios datos autorizados. La interfaz SHALL mostrar únicamente el aviso «Generado con IA. Revisá la información y sus fuentes.» y las cuatro secciones del resumen, sin fechas, cobertura, rangos ni desplegables de fuentes (ajuste solicitado el 31/10/2026). SHALL renderizar el contenido como texto.

#### Scenario: Fuente fuera de la página visible
- **WHEN** una afirmación usa una actividad que no está en la página actual del historial
- **THEN** la API conserva su descripción, resultado y fechas enviados al modelo, y el panel muestra la afirmación sin desplegables de fuentes

#### Scenario: Referencias inexistentes o formato inválido
- **WHEN** el modelo devuelve JSON inválido, campos inesperados, referencias no enviadas o límites excedidos
- **THEN** no se muestra un resumen exitoso y se responde `502` con código `AI_INVALID_RESPONSE`, sin devolver el cuerpo del proveedor

### Requirement: Notas tratadas como datos

El sistema SHALL tratar observaciones, actividades y salida de Gemini como contenido no confiable. No SHALL ejecutar instrucciones incluidas en notas, habilitar herramientas del modelo, interpretar HTML ni seguir enlaces generados. La clave y los datos técnicos de autenticación SHALL permanecer fuera del contexto y de las respuestas.

#### Scenario: Instrucción maliciosa en una actividad
- **WHEN** una nota pide ignorar reglas, revelar credenciales o consultar oportunidades ajenas
- **THEN** no se ejecutan acciones ni consultas adicionales y la evaluación verifica que no se sigan esas instrucciones en la síntesis

### Requirement: Operación gratuita con modelos explícitos

El sistema SHALL utilizar sólo modelos Gemini de texto identificados explícitamente y verificados con nivel gratuito en el proyecto. SHALL excluir Flash 3.8, aliases `latest`, modelos preview y cambios automáticos a servicios pagos. La función SHALL estar desactivada por defecto; el CRM SHALL iniciar sin credencial de Gemini.

#### Scenario: Configuración deshabilitada o incompleta
- **WHEN** la IA está apagada o se habilita sin credencial válida de configuración
- **THEN** el resto del CRM funciona y la generación responde `503 AI_NOT_CONFIGURED` sin llamar al proveedor

#### Scenario: Modelo fuera de la lista permitida
- **WHEN** se configura Flash 3.8, un alias móvil o un modelo no permitido
- **THEN** la función queda indisponible con diagnóstico técnico sin iniciar una llamada ni afectar la salud general del CRM

### Requirement: Recuperación acotada frente a fallos

El sistema SHALL limitar la generación completa a 40 segundos y tres intentos salientes como máximo, incluyendo la alternativa. SHALL reintentar sólo errores transitorios y respetar la espera indicada si cabe en el plazo. No SHALL rotar claves/proyectos ni modelos para eludir cuotas, ni reintentar errores de credenciales, formato o bloqueo de contenido.

#### Scenario: Sobrecarga transitoria
- **WHEN** Gemini devuelve `503` y queda presupuesto de tiempo e intentos
- **THEN** se aplica espera creciente con variación aleatoria y se permite usar la alternativa gratuita previamente configurada

#### Scenario: Cuota diaria agotada
- **WHEN** Gemini identifica una cuota diaria agotada o no se puede clasificar su `429`
- **THEN** no se recorre la lista de modelos para evadir el límite y se devuelve `429 AI_QUOTA_EXCEEDED` con espera conocida o indicación de intentar más tarde

#### Scenario: Tiempo máximo o indisponibilidad persistente
- **WHEN** se agota el plazo o los intentos
- **THEN** se responde respectivamente `504 AI_TIMEOUT` o `503 AI_UNAVAILABLE` y siguen disponibles datos, etapas y actividades de la oportunidad

### Requirement: Protección de cuota y duplicados

El sistema SHALL limitar las llamadas salientes y las generaciones simultáneas en el backend, contando reintentos y alternativas. SHALL rechazar duplicados en curso sin consumir cuota adicional. La interfaz SHALL informar la espera y ofrecer «Reintentar» ante un fallo, sin reintentos automáticos por montaje, foco o reconexión, y bloquear el doble clic de ese control.

#### Scenario: Doble clic y solicitudes concurrentes
- **WHEN** llegan dos generaciones simultáneas del mismo contexto o se excede el límite local
- **THEN** sólo se inicia trabajo admitido y las solicitudes rechazadas reciben `429 AI_RATE_LIMITED` con `retryAfterSeconds`

#### Scenario: Fallo de la generación automática
- **WHEN** termina con error el ciclo acotado de generación y el resultado de error permanece en memoria
- **THEN** se muestra el error con «Reintentar» cuando corresponda, sin nuevos ciclos por remontar o enfocar; el clic respeta las esperas y los límites existentes

### Requirement: Frescura y sesión del resultado

El sistema SHALL descartar con `409 AI_CONTEXT_CHANGED` un resultado cuyo contexto cambió durante la generación. El resumen SHALL persistirse y reemplazarse sólo tras una generación válida para el contexto actualizado. Un fallo SHALL conservar el último éxito sin entregarlo como vigente. Cambiar de sesión SHALL vaciar la caché del navegador y revalidar acceso antes de consultar la persistencia. Una mutación local confirmada SHALL iniciar una actualización automática si el detalle sigue activo. Navegar SHALL ocultar el resumen sin impedir su reutilización persistida para la oportunidad de origen. No SHALL mostrarse una respuesta tardía de otro contexto o usuario.

#### Scenario: Actividad nueva durante la generación
- **WHEN** se registra una actividad mientras se está generando el resumen
- **THEN** el resultado anterior se descarta; si el cambio fue local y el detalle sigue activo, se solicita una actualización automática serializada; un `409` sin mutación local conocida ofrece «Reintentar» sin iniciar un bucle

#### Scenario: Cambio comercial local tras el resumen
- **WHEN** se confirma una actividad nueva, edición, reasignación, transición, cierre o reapertura en el cliente
- **THEN** se invalida el resumen afectado y se actualiza automáticamente sólo si su detalle sigue activo y autorizado; si está cerrado, se generará en la próxima entrada

#### Scenario: Cambio de cuenta o de ruta
- **WHEN** una solicitud termina después de cerrar sesión o navegar a otra oportunidad
- **THEN** su resultado no aparece ni se conserva para la nueva cuenta u oportunidad; sólo puede completar la caché de origen si su sesión y contexto siguen siendo válidos

### Requirement: Trazas y evaluación sin credenciales reales

El sistema SHALL registrar sólo metadatos operativos de IA, sin clave, prompts, notas, respuesta íntegra ni datos personales. Las pruebas automáticas SHALL usar un proveedor simulado. La validación manual real SHALL utilizar una credencial nueva y datos ficticios, registrar modelo/cuotas/fecha y distinguirla de los resultados simulados.

#### Scenario: Fallo del proveedor
- **WHEN** se produce un error de Gemini que contiene información técnica sensible
- **THEN** la respuesta y los logs conservan sólo códigos controlados, correlación, duración e intentos y no el cuerpo ni cabeceras de autenticación

#### Scenario: Evidencia de entrega
- **WHEN** se valida la funcionalidad
- **THEN** se documentan permisos, fidelidad, fuentes, errores y prueba real si se realizó, sin presentar un mock como prueba de disponibilidad de Gemini
