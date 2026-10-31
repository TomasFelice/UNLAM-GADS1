# Design

## Context

Motivación y alcance: [proposal.md](proposal.md). El código revisado ya ofrece `ActivityService.byOpportunity`, `OpportunityAccessService.getReadableContext` y `StageHistoryService.list`, pero sus resultados actuales no son un contrato de contexto IA: el primero pagina DTOs HTTP y el último carga el historial completo. El detalle frontend compone datos, etapas e historial en `PaginaOportunidadDetalle.tsx`; `HistorialComercial` pagina de a 20 y `LineaDeActividades` no tiene navegación directa por ID.

La dirección vigente es `activities → opportunities → {customers, offerings, catalogs} → access → tenancy`. Introducir una dependencia inversa para reunir actividades en oportunidades produciría un ciclo. El backend usa JWT propio, tenant del usuario, errores `ProblemDetail` y JPA con `open-in-view=false`. Las pruebas HTTP reales usan PostgreSQL de Testcontainers; frontend consume OpenAPI generado, TanStack Query y `solicitarApi`, que descarta respuestas de una sesión anterior.

La decisión del usuario es **Gemini gratuito**, sin activar facturación ni usar créditos del plan Pro. La credencial compartida se considera expuesta; no se guarda ni se utiliza en esta planificación. Los archivos con cambios locales anteriores quedan fuera del cambio de planificación.

## Goals / Non-Goals

**Goals:** síntesis automática al entrar al detalle autorizado, con carga independiente; contexto acotado y revisable; reutilización temporal y deduplicación para cuidar cuota; integración pequeña que no cambie las escrituras comerciales; pruebas deterministas separadas de la evaluación real.

**Non-Goals:** persistencia duradera del resumen, migraciones, conversaciones de seguimiento, propuestas, recomendaciones de oferta, ejecución de acciones, búsquedas web, RAG, embeddings, colas, infraestructura distribuida o garantía de disponibilidad de un proveedor gratuito. La caché temporal frontend vive sólo en memoria y no es persistencia comercial.

## Decisions

### 1. Módulo coordinador independiente y puertos de lectura

Crear `com.ztech.crm.assistance` con controller, service, dto y cliente de proveedor dentro del módulo. El controller pertenece a `assistance` aunque su ruta cuelgue de una oportunidad. No modificar `OpportunityController` para depender del asistente.

Registrar **ADR-007** antes de implementar, verificando que el número siga libre: `assistance → {activities, opportunities}` encima del grafo actual. Actualizar AGENTS/contexto/arquitectura en el apply, sin presentar esta propuesta como arquitectura ya vigente.

Agregar proyecciones inmutables públicas en `opportunities.service` y `activities.service` para lectura IA: datos necesarios, conteos y cortes acotados, con consultas y autorización propiedad de cada módulo. `assistance` consume esos servicios, nunca repositorios, entidades ni DTOs HTTP de otros módulos. Reutilizar las reglas de lectura existentes, pero no llamar al listado ilimitado de etapas ni cargar todas las páginas de actividades. No agregar métodos IA a los módulos inferiores sólo para hacer llamadas a Gemini.

Alternativas descartadas: oportunidades consultando actividades (ciclo), frontend armando el contexto (incompleto y manipulable), reutilizar DTOs HTTP como contrato modular (acopla presentaciones), microservicio/SDK de agentes (innecesario para una llamada de síntesis).

### 2. Snapshot, selección y límites de contexto

Un ensamblador obtiene un snapshot consistente en una transacción breve de lectura `REPEATABLE_READ`; devuelve sólo valores inmutables y termina la transacción antes de la red. No mantener conexión, lock ni transacción durante espera/reintentos. Mantener la orquestación en el hilo de solicitud; si el transporte usa tareas internas, no acceder allí a `TenantContext` ni repositorios.

Contenido permitido:

- Fuente `O`: título y observaciones de la oportunidad; estado, etapa/embudo actuales, datos del evento, salón y servicios seleccionados; importes y cierre sólo si constan. No tomar valores ausentes como cero ni probabilidades opcionales como hechos predictivos.
- Fuentes `A1…`: únicamente actividades con `opportunity_id` igual al solicitado y tenant/alcance autorizados. Tipo, descripción, resultado, fecha del hecho y fecha de registro; no correo ni datos de autenticación del autor.
- Fuentes `H1…`: transiciones/cierres/reaperturas de esa oportunidad con fecha, nota y snapshots comerciales disponibles. No confundir nombres actuales de catálogo con nombres históricos que el sistema no conservó.

Valores iniciales propuestos, configurables dentro de topes seguros:

| Límite | Inicial |
|---|---:|
| Actividades candidatas | 50 más recientes por `occurredAt DESC, id DESC` |
| Transiciones candidatas | 50 más recientes por `changedAt DESC, id DESC` |
| Descripción y resultado de cada actividad | 2.000 caracteres por campo |
| Nota de etapa | 1.000 caracteres |
| Observaciones de oportunidad | 4.000 caracteres |
| Contexto serializado de negocio enviado | 24.000 caracteres Unicode |
| Respuesta HTTP del proveedor | 64 KiB |
| Tokens máximos de salida | 2.048 |

Reservar primero la fuente actual; intercalar luego fuentes históricas desde las más recientes, en orden determinista, hasta el presupuesto. Serializar los registros elegidos en orden cronológico para el análisis. Recortar campos por puntos de código y marcar cada corte; no romper JSON ni Unicode. Incluir metadatos de presupuesto y de exclusión; el límite de caracteres **no equivale** a una garantía de tokens y debe contrastarse con la cuota TPM real.

La cobertura devuelve totales, cantidades finalmente incluidas, rangos separados de actividades/etapas, `partial`, `textTruncated` y reglas de selección. La generación usa ese mismo contexto al producir y revisar fuentes. No incorporar toda la historia de la empresa/contacto por compartir cliente. Omitir CUIT/documento, email, teléfono, domicilio, JWT y tenant técnico del prompt. Esto reduce datos enviados; no anonimiza automáticamente nombres o información personal escritos en texto libre.

Calcular una huella canónica interna del snapshot (versión de oportunidad, datos efectivos, conteos y fuentes ordenadas, sin incluir la hora de lectura). Después de Gemini, obtener nuevamente snapshot y autorización; si pierde alcance devolver `404`; si cambia la huella devolver `409 AI_CONTEXT_CHANGED`. Incluir conteos evita ignorar una nueva actividad fuera de la ventana temporal. Las fuentes antiguas append-only omitidas no necesitan ser cargadas para esta comprobación. El resultado representa el último snapshot validado; no elimina la carrera normal de una modificación posterior a la respuesta.

### 3. Proveedor, modelos y modo gratuito

Usar REST `generateContent` sobre HTTPS, endpoint fijo de Google y clave en cabecera `x-goog-api-key`, nunca en query string. Implementar `GeminiSummaryClient` con el cliente HTTP del JDK y el serializador JSON ya disponible en Spring Boot. No añadir Spring AI, LangChain ni SDK de agentes; una interfaz `SummaryGenerator` permite reemplazar el transporte en pruebas. Limitar bytes leídos y cancelar/cerrar la respuesta al superar tamaño o plazo. Los servidores HTTP de prueba se inyectan sólo en tests; el usuario HTTP no puede indicar URL de destino.

Configuración propuesta:

| Variable backend | Valor inicial / propósito |
|---|---|
| `AI_SUMMARY_ENABLED` | `false`; si está apagada no hay llamada ni impacto en health |
| `GEMINI_API_KEY` | sin valor por defecto; secreto nuevo, sólo backend |
| `GEMINI_SUMMARY_MODEL` | `gemini-3.1-flash-lite` |
| `GEMINI_SUMMARY_FALLBACK_MODEL` | vacío; candidato `gemini-3.5-flash-lite` tras prueba real |
| `AI_SUMMARY_TIMEOUT_SECONDS` | `40`; plazo total incluyendo ensamblado, esperas y revalidación |
| `AI_SUMMARY_MAX_ATTEMPTS` | `3`; total, no por modelo |
| `AI_SUMMARY_MAX_CALLS_PER_MINUTE` | `3`; límite local inicial, se ajusta a AI Studio |
| `AI_SUMMARY_MAX_CALLS_PER_DAY` | `20`; límite local inicial, incluye intentos fallidos |

Lista permitida inicial de modelos: los dos Flash-Lite anteriores. Configuración inválida deja la función indisponible, con log saneado y `503 AI_NOT_CONFIGURED`; el backend sigue arrancando. No usar `gemini-flash-latest`: su destino cambia. No usar 3.8 ni eludir la exclusión mediante aliases. No habilitar fallback hasta verificar que el candidato genera JSON válido en Free Tier para ese proyecto. No hay promoción automática a Paid Tier ni llamada a herramientas pagas. El código no puede demostrar que una clave arbitraria pertenece a un proyecto sin facturación: verificar **Free Tier** en AI Studio al configurar y documentarlo como requisito operativo.

Los candidatos son una selección razonable por perfil de tareas, **no una medición de carga**. La disponibilidad real, sus cuotas y la calidad deben evaluarse con la nueva clave antes de habilitar la función en el entorno de demostración.

### 4. Contrato de síntesis y fuentes

`POST /api/v1/opportunities/{id}/commercial-summary`, sin body; rechazar body no vacío o parámetros no admitidos. JWT y rol válido requeridos; autorizar ID antes de comprobar cuotas o invocar Gemini. Control de acceso de lectura para oportunidades abiertas y cerradas, sin conceder permisos de edición.

Enviar instrucción de sistema en español y datos serializados como contenido a analizar. Solicitar `application/json` con esquema explícito en `generationConfig`, sin herramientas, búsqueda, navegación o function calling. Separar claramente instrucciones de notas; priorizar estado estructurado para situación actual, declarar falta de datos y reconocer contradicciones sin resolverlas por suposición.

La salida del modelo contiene cuatro arrays: `clientNeeds`, `currentSituation`, `agreementsAndObjections`, `unconfirmedInformation`. Cada elemento contiene `text` (hasta 700 caracteres) y `sourceIds` (entre 1 y 5 tokens del contexto). Hasta 5 elementos por sección; arrays vacíos son válidos y la interfaz presenta ausencia de información con texto fijo. El esquema limita campos y tamaño. Validar en servidor también: estructura exacta, número de elementos, texto no vacío, tokens permitidos y ausencia de referencias desconocidas. No pedir al modelo URLs, fechas de generación ni contenido de fuentes.

El backend construye `CommercialSummaryResponse`: `opportunityId`, `generatedAt`, `contextAsOf`, `opportunityVersion`, `coverage`, `sections` y `sources`. `sources` proviene exclusivamente del snapshot, con tipo, ID original autorizado, título y fragmentos exactos utilizados; muestra fechas y banderas de recorte. El modelo usado y prompt-version se registran como metadatos operativos; no es necesario mostrarlos como texto de producto.

Validar referencias no demuestra que cada frase esté semánticamente respaldada. Esa limitación se aborda con prompt, evaluación manual y revisión del usuario; no etiquetar el resultado como «verificado automáticamente». Renderizar siempre texto escapado, sin HTML/Markdown ejecutable ni enlaces sugeridos por Gemini.

### 5. Errores, plazos y conservación de cuotas

Admisión por proceso: una generación simultánea, sin cola persistente. Rechazar otra con `429 AI_RATE_LIMITED`; guardar sólo identificadores/temporizadores de trabajo en curso. Ventana móvil por minuto y contador diario UTC locales; contar **cada intento saliente**, incluidos fallback y errores. El valor inicial de tres llamadas permite ejecutar los tres intentos de una generación, pero no implica que la cuota del proyecto los admita: reducirlo si AI Studio muestra un límite menor. Los límites del proveedor pueden usar otra zona o ámbito y son la autoridad efectiva; los locales no garantizan evitar `429`. Reiniciar la instancia reinicia contadores; no presentarlos como cuota distribuida o facturación. Documentar límite de una instancia y no añadir Redis para este TP.

Máximo de tres llamadas: primera al primario; segundo intento al primario ante transitorio; último al fallback habilitado ante indisponibilidad transitoria del primario, o al primario si no hay fallback. Timeout de conexión 3 s y por intento hasta 10 s, siempre recortados al tiempo restante del plazo total. Reservar hasta 5 s de los 40 s para revalidación y respuesta. Si no queda tiempo o admisión local, detener intentos. Una sola capa administra reintentos; no apilar retries HTTP/SDK/TanStack.

Espera exponencial con jitter (base 1 s, máximo 4 s), respetando `Retry-After`/retry delay de Google cuando sea conocido y quepa. `429` reintentable sólo si se identifica como límite temporal con espera conocida; para cuota diaria, cuota cero o causa desconocida, detener y devolver el error. No cambiar modelo ante cuota agotada. Reintentar `500`, `503`, `504` y fallos transitorios de transporte dentro del presupuesto. Credencial rechazada, modelo inexistente, `400`, bloqueo de seguridad, respuesta vacía o esquema inválido no disparan reintentos ni fallback. No cambiar de clave ni proyecto.

| HTTP CRM / código | Situación |
|---|---|
| `401 UNAUTHENTICATED` | sin sesión, formato vigente |
| `403 FORBIDDEN` | rol no habilitado, formato vigente |
| `404 NOT_FOUND` | oportunidad inexistente o fuera del alcance; conservar código vigente |
| `409 AI_CONTEXT_CHANGED` | contexto cambió mientras se generaba |
| `422 AI_CONTENT_BLOCKED` | proveedor bloqueó el contenido; sin cuerpo del proveedor |
| `429 AI_RATE_LIMITED` | admisión/límite local; incluir espera |
| `429 AI_QUOTA_EXCEEDED` | cuota de Gemini agotada o no clasificada |
| `502 AI_INVALID_RESPONSE` | JSON/esquema/referencias/tamaño/resultado vacío inválidos |
| `503 AI_NOT_CONFIGURED` | apagada, falta clave o modelo configurado no permitido |
| `503 AI_UNAVAILABLE` | indisponibilidad, credencial rechazada o modelo no accesible; diagnóstico sólo técnico |
| `504 AI_TIMEOUT` | plazo agotado |

Usar excepciones controladas que extiendan `ApiException`; mantener `ProblemDetail` y request ID. Para `429` incluir extensión JSON `retryAfterSeconds` cuando se conozca, más `Retry-After`; la UI consume la extensión sin requerir exposición CORS adicional. Ante espera desconocida mostrar «Intentá más tarde», sin inventar hora de restablecimiento. No convertir errores upstream de credenciales en `401` del CRM, ya que cerraría la sesión del usuario.

### 6. Interfaz efímera y revisión

Agregar `ResumenComercial.tsx` y su CSS Module en `features/oportunidades`. Montar/habilitar el panel después de confirmar acceso al detalle mediante una lectura actual de la oportunidad; si se vuelve a la ruta, revalidar esa lectura antes de mostrar un resumen reutilizado. No generar ni mostrar resúmenes para IDs inválidos, detalles sin autorización o pantallas distintas. El resto del detalle se muestra sin esperar a Gemini.

`apiResumenComercial.ts` usa tipos de OpenAPI, `solicitarApi` y una consulta TanStack específica que realiza el POST de generación. Aunque el endpoint sea POST, aquí no escribe negocio: la consulta permite reutilizar el resultado y compartir una promesa en curso. Clave por identificador opaco de sesión (no JWT), tenant, usuario, oportunidad y versión conocida. Invalidar además por actividades; no alcanza con la versión de oportunidad. No disparar una mutación desde un efecto por montaje, que podría duplicarse en StrictMode.

Configurar explícitamente `staleTime` y `gcTime` de cinco minutos, `retry: false`, `retryOnMount: false`, `refetchOnWindowFocus: false`, `refetchOnReconnect: false` y sin intervalo. El TTL de reutilización se mide desde el éxito de generación; el acceso y la versión se revalidan por la lectura del detalle antes de reutilizar. Una entrada sin resultado o con un éxito vencido inicia una generación; los errores retenidos no se reintentan al remontar. No iniciar actualización por mero paso del tiempo mientras el panel permanece montado. Una recarga completa pierde la caché y puede volver a generar, dentro de los límites del backend.

Estados: esperando validación del detalle, generando, generado, sin información por sección y error. No hay botón inicial «Generar resumen comercial» ni acción de regeneración sobre un éxito; sólo «Reintentar» ante errores recuperables. Mostrar fecha del contexto, cobertura parcial si aplica y «Generado con IA. Revisá la información y sus fuentes». Las esperas y los reintentos internos del backend se conservan; el cliente no añade otra capa de retries.

Cada afirmación referencia fuentes del resultado que se pueden desplegar en el mismo panel con título, tipo, fechas, fragmentos y recortes. No depender de enlaces a una fila aún no cargada del historial; no se agrega endpoint de detalle de actividad sólo para navegar a ella. La selección de fuente devuelve foco correctamente; controles accesibles por teclado.

Conservar resultados y solicitudes en curso en la caché en memoria de esa sesión durante su vida acotada, sin localStorage ni base de datos. Navegar oculta el panel y no borra inmediatamente su entrada: volver al mismo contexto puede reutilizarla. No cancelar una petición compartida por un simple desmontaje, incluido el de StrictMode; dejarla completar sólo para su clave de origen si sesión y contexto siguen vigentes. Por ello la consulta no consume el `AbortSignal` de TanStack para cancelar automáticamente por quedar sin observadores; sí usar cancelación propia o descarte lógico ante cambio de sesión o contexto. Cancelar en navegador no garantiza recuperar cuota ya consumida.

Al registrar actividad o modificar/reasignar/cambiar etapa/cerrar/reabrir la oportunidad, invalidar la entrada afectada y retirar de pantalla el resultado anterior. Si el detalle sigue activo y autorizado, iniciar una sola actualización automática sobre la revisión local más reciente; si está fuera de pantalla, regenerar en la próxima entrada. Agrupar las señales de una misma mutación comercial para no iniciar una generación por cada invalidación. Si existe una generación anterior en curso, marcarla obsoleta y esperar a que termine antes de lanzar su sustituta; no encadenar llamadas por cada rerender o cada señal intermedia. Las restricciones del backend siguen aplicando y pueden devolver `429`.

Los cambios remotos no observados no se detectan instantáneamente: una nueva versión de oportunidad observada invalida la entrada, mientras que una actividad remota puede no detectarse hasta la siguiente generación tras vencer la reutilización. Mostrar siempre `contextAsOf` y no presentar la caché como actualización en tiempo real. El TTL limita la reutilización al volver a la pantalla; no obliga a llamar de nuevo mientras el usuario lee un resumen ya mostrado.

La comprobación de sesión de `solicitarApi` se conserva; al ingresar, salir o expirar la sesión se vacían las entradas y se invalidan lógicamente sus tareas pendientes. Una respuesta tardía no repuebla la caché tras ese cambio ni aparece bajo otra oportunidad. `409` sin invalidación local conocida informa «La oportunidad cambió. Reintentá el resumen», sin bucle automático; `429` informa espera y habilita «Reintentar» cuando corresponda. El panel no reemplaza ni bloquea datos, actividades o acciones. Sin clave, la carga automática informa que la función no está habilitada; no es necesario un endpoint público de configuración.

### 7. Validación y operación

Pruebas unitarias de selección/presupuesto/huella, esquema y referencias, clasificador de fallos y plazos con reloj/espera inyectables. Cliente HTTP contra servidor local simulado para comprobar cabecera, `generateContent`, JSON, códigos, límite de bytes, timeout y número exacto de intentos. Ninguna prueba automática llama a Google.

IT por HTTP real con PostgreSQL efímero y proveedor simulado: tres roles, anónimo, ID ajeno, cliente compartido, revalidación tras reasignación, nueva actividad durante generación, payload inspeccionado y ausencia de escrituras mediante SQL. No existe listado nuevo: cubrir también el aislamiento en las consultas de fuentes/conteos. Los fallos transitorios, JSON malformado, cuota y feature apagado se prueban sin red externa.

Vitest para generación tras validación del detalle, carga independiente, fuentes fuera de página, StrictMode/remontaje compartiendo solicitud, TTL/retorno sin llamada nueva, entrada vencida, ausencia de generación por foco/reconexión/temporizador y reintento manual sin doble clic. Cubrir invalidaciones agrupadas, actualización serializada y descarte de respuesta tardía ante sesión/ruta. Playwright recorre login → oportunidad → resumen automático → revisar fuentes → salir/volver sin otro consumo → registrar actividad → actualización automática; prueba error/reintento sin perder acciones y 390 px/ambos temas. El launcher E2E inyecta el servidor simulado sólo en perfil de pruebas.

Evaluación manual separada con datos ficticios y credencial nueva, serial y de baja frecuencia: sin actividades; acuerdos consistentes; contradicción; cierre/reapertura; historial recortado; instrucción maliciosa; datos faltantes. Criterios: ninguna afirmación comercial crítica inventada, fuentes válidas para cada afirmación, cobertura honesta y estados correctos. Si un caso falla, ajustar prompt/modelo y repetir sólo los casos afectados; una respuesta con formato correcto no acredita calidad. Registrar modelo exacto, fecha, cantidad de llamadas, cuotas observadas, latencia y errores; no afirmar que tres respuestas prueban disponibilidad futura.

Logs: correlación actual, modelo, versión de prompt, duración, intentos, clase de error y uso de tokens si está disponible. Sin prompts, notas, textos de respuesta, claves, headers o cuerpos de error upstream. La opción gratuita puede utilizar contenido para mejorar productos: explicar el flujo en README/privacidad y limitar la demo a datos ficticios; filtrar campos no convierte texto libre en anónimo.

## Risks / Trade-offs

- [No hay garantía de capacidad en Free Tier] → modelos explícitos configurables, pocas llamadas, plazo/reintentos limitados y error recuperable. No afirmar que Flash-Lite tenga menor ocupación medida.
- [Generar al abrir aumenta consumo respecto del botón] → reutilización en memoria por cinco minutos, deduplicación, sin llamadas por foco/reconexión ni precarga de otras rutas; mantener los límites backend existentes.
- [Cuota cero o modelos inaccesibles en el proyecto] → prueba mínima serial y revisión de AI Studio antes de habilitar; se puede elegir otro de los dos candidatos sin cambiar código. No activar pago como solución implícita.
- [Alucinaciones y prompt injection] → sin herramientas ni autoridad de negocio, esquema/fuentes validados, evaluación y revisión humana; sigue existiendo riesgo semántico.
- [Recorte pierde acuerdos antiguos] → cobertura visible, fragmentos exactos y acceso al historial completo normal. No llamar «resumen de toda la negociación» si es parcial.
- [Contexto cambia o se pierde acceso] → snapshot consistente, revalidación y descarte; no cachear entre usuarios.
- [La caché no detecta todo cambio remoto] → revalidar acceso y versión al entrar, invalidar cambios locales, reutilización acotada y fecha del contexto visible; sin promesa de tiempo real.
- [Dos lecturas del contexto agregan trabajo] → proyecciones y conteos acotados, sin descarga de todas las páginas ni transacción durante red.
- [Límites locales se reinician y no coordinan réplicas] → alcance de una instancia documentado; revisar diseño antes de escalar horizontalmente.

## Migration Plan

1. Registrar ADR y resolución de DP-12 con el alcance elegido; actualizar límites modulares antes del código. No renumerar decisiones existentes.
2. Implementar backend y contrato con IA apagada por defecto; no hay migraciones Flyway.
3. Exportar OpenAPI desde runtime local, regenerar tipos e integrar panel; probar errores con IA apagada.
4. Ejecutar suites locales pertinentes y recorrido normal del CRM. No certificar producción con sus resultados.
5. Con la nueva clave fuera del repo, comprobar Free Tier, cuotas y candidato por prueba manual ficticia; habilitar fallback sólo si también se prueba. No usar la clave expuesta.
6. Si se solicita publicar, configurar variables sólo en backend, sin facturación; realizar smoke autenticado remoto y registrar límites. Esta planificación no autoriza publicar.
7. Rollback funcional: `AI_SUMMARY_ENABLED=false`; el CRM y sus datos siguen operativos. No hay esquema ni datos de resúmenes que revertir.

## Open Questions

- Cuotas efectivas y disponibilidad de los candidatos en el proyecto del usuario; se resuelven con AI Studio y nueva clave durante configuración, sin cambiar contrato ni habilitar pago.
- Latencia y fidelidad sobre el corpus ficticio; los valores iniciales y el orden de candidatos se ajustan dentro de estos límites según la evidencia, sin prometer un resultado no medido.

## Fuentes verificadas el 29/10/2026

- [Gemini 3.1 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite): perfil de baja latencia, texto y salidas estructuradas.
- [Precios y Free Tier](https://ai.google.dev/gemini-api/docs/pricing): candidatos gratuitos y tratamiento de datos; no equivale a disponibilidad para una cuenta concreta.
- [Versiones y aliases](https://ai.google.dev/gemini-api/docs/models): `latest` puede cambiar de destino.
- [Cuotas](https://ai.google.dev/gemini-api/docs/rate-limits): consultar límites reales en AI Studio; no copiar cifras antiguas como garantía.
- [Recuperación de fallos](https://ai.google.dev/gemini-api/docs/troubleshooting): backoff para errores reintentables.
- [Salidas estructuradas](https://ai.google.dev/gemini-api/docs/structured-output): formato JSON; no demuestra veracidad semántica.
