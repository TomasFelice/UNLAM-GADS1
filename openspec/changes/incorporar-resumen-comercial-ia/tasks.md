# Tasks

Plan de implementación, no evidencia de trabajo ejecutado. Contrato funcional en [specs/resumen-comercial-ia/spec.md](specs/resumen-comercial-ia/spec.md), decisiones y límites en [design.md](design.md). No hay migraciones, commits, pushes ni publicación incluidos.

## 1. Decisiones y configuración segura

- [x] 1.1 Registrar ADR-007 (comprobar número libre) con el coordinador `assistance → {activities, opportunities}`, REST directo y resumen efímero; resolver DP-12 con la decisión del usuario y actualizar AGENTS/contexto/arquitectura afectados. Verificar grafo sin ciclos, referencias y distinción entre propuesta e implementación.
- [x] 1.2 Crear configuración backend con feature apagada, secreto sin valor por defecto, modelos explícitos permitidos, fallback vacío y límites de diseño. Verificar arranque y health sin clave, configuración inválida aislada de la salud general y exclusión de Flash 3.8/`latest`/preview mediante pruebas de configuración.
- [x] 1.3 Documentar en `backend/.env.example` y README únicamente placeholders y variables no sensibles, uso exclusivo de Free Tier y necesidad de reemplazar la clave expuesta. Verificar que ningún secreto llegue al frontend, OpenAPI, fixtures o archivos versionados y que no se active facturación.

## 2. Contexto autorizado y acotado

- [x] 2.1 Agregar proyección de oportunidad y etapas en servicios de `opportunities`, con estado actual, datos del evento/oferta, snapshots de cierres, conteos y ventana reciente de 50 transiciones. Verificar por IT que el corte se realiza en consulta, conserva fechas/orden y respeta tenant y responsable sin exponer entidades o DTOs HTTP a `assistance`.
- [x] 2.2 Agregar proyección de actividades en `activities.service`, con selección exclusiva por oportunidad, tenant/alcance, conteos y 50 hechos recientes con desempate por ID. Verificar por IT más de 50 registros, empates, cliente compartido, otro vendedor y otro tenant, inspeccionando también que sus fuentes no se filtren.
- [x] 2.3 Implementar ensamblador de snapshot en transacción breve de lectura y selección/presupuesto de texto, cobertura y huella canónica. Verificar unitariamente Unicode, campos ausentes, textos largos, contexto de 24.000 caracteres, rangos, truncamientos e inclusión de conteos; verificar por IT que el snapshot es consistente y la transacción termina antes de invocar al proveedor.
- [x] 2.4 Agregar fixtures ficticios de sin actividades, acuerdos, contradicción, cierre/reapertura e historia extensa; documentar selección y exclusiones en la documentación backend. Verificar que los textos, cuotas y casos de ejemplo no provengan de clientes reales ni dependan de nombres de catálogo para determinar estados.

## 3. Cliente Gemini y validación del resultado

- [x] 3.1 Implementar `SummaryGenerator` y transporte REST JDK contra `generateContent`, endpoint fijo HTTPS, clave en cabecera y cierre/cancelación de respuesta limitada a 64 KiB. Verificar con servidor HTTP local simulado la ruta/modelo, headers, cuerpo y ausencia de clave en URL/logs; comprobar timeouts y lectura limitada también con respuesta lenta o demasiado grande.
- [x] 3.2 Implementar prompt versionado y JSON Schema de cuatro secciones con fuentes, sin tools, búsqueda ni funciones. Agregar validación backend de forma exacta, 5 elementos por sección, texto hasta 700 caracteres y referencias de 1 a 5 fuentes autorizadas. Verificar respuestas correctas, vacías, malformadas, truncadas, campos inesperados y referencias inventadas mediante pruebas de cliente/servicio.
- [x] 3.3 Construir respuesta y fuentes exclusivamente desde el snapshot, con fechas, fragmentos y cobertura; mantener texto como datos no ejecutables. Verificar por prueba que el proveedor no puede inyectar fuentes, enlaces accionables, HTML interpretado ni metadatos de autorización.
- [x] 3.4 Agregar documentación del contrato de prompt, límites y riesgos semánticos junto al backend. Verificar correspondencia con la spec y con las pruebas; no afirmar que validar un ID de fuente demuestra fidelidad de la afirmación.

## 4. Cuotas, reintentos y observabilidad

- [x] 4.1 Implementar admisión de una generación simultánea y límites locales por proceso de 3 llamadas/minuto y 20/día, contando cada intento y ajustando a cuotas reales menores; liberar permisos en éxito, error y cancelación. Verificar con reloj inyectable concurrencia, doble solicitud, reinicio de ventana y ausencia de llamadas rechazadas; documentar alcance de una instancia y reinicio de contadores.
- [x] 4.2 Implementar plazo total máximo de 40 s, conexión de 3 s, intento de hasta 10 s, reserva de revalidación y hasta tres llamadas totales; espera creciente con jitter y alternativa sólo ante indisponibilidad transitoria. Verificar sin esperas reales el número exacto de llamadas/modelos, presupuesto agotado, `Retry-After` que cabe/no cabe y ausencia de retries anidados.
- [x] 4.3 Clasificar cuota diaria/desconocida, límite temporal conocido, credencial inválida, modelo no accesible, bloqueo de contenido, JSON inválido y errores transitorios. Verificar que no se usa fallback ante cuota diaria/`429` desconocido ni se reintentan errores permanentes; comprobar códigos CRM y `retryAfterSeconds` cuando corresponde.
- [x] 4.4 Agregar metadatos operativos con correlación, modelo, prompt-version, duración, intentos y tokens disponibles. Verificar captura de logs frente a cuerpos upstream con secretos ficticios, para comprobar que sólo se impriman campos controlados; documentar diagnóstico de cuota/sobrecarga sin payloads comerciales.

## 5. Endpoint y contrato compartido

- [x] 5.1 Crear controller de `assistance` para `POST /api/v1/opportunities/{id}/commercial-summary`, sin contexto de cliente, más orquestación fuera de transacción durante HTTP y errores `ProblemDetail`. Verificar HTTP real para tres roles, anónimo, ID inválido/ajeno, cuerpo no admitido, IA apagada, clave rechazada y proveedor indisponible sin cerrar la sesión del CRM.
- [x] 5.2 Revalidar autorización y huella antes de entregar la respuesta; descartar con `404` o `409 AI_CONTEXT_CHANGED`. Verificar mediante IT coordinados una reasignación, una actividad nueva y un cambio de etapa durante la espera del proveedor; comprobar que no se devuelve el resultado anterior.
- [x] 5.3 Verificar ausencia de escrituras comparando oportunidad, versión, actividades e historial por SQL antes/después de generación correcta y fallida; comprobar además que no se sostengan locks/transacción mientras responde el proveedor simulado.
- [x] 5.4 Documentar operación, respuestas y cobertura; exportar `backend/docs/openapi.yaml` desde runtime local y regenerar tipos con `npm run generar:api` en frontend. Verificar exportación con el mecanismo existente y que los tipos usados por el frontend deriven del contrato, sin duplicar DTOs.

## 6. Panel y revisión en frontend

- [x] 6.1 Implementar `apiResumenComercial.ts` y `ResumenComercial.tsx` con generación automática después de revalidar acceso al detalle, consulta específica sin retry, reutilización en memoria de cinco minutos por sesión/oportunidad y tipos generados. Verificar con Vitest primera entrada, retorno vigente/vencido, recarga sin caché, StrictMode/remontajes con promesa compartida, errores retenidos sin retry al montar y ausencia de generación por foco, reconexión, temporizador o rutas distintas; comprobar carga independiente y `401` vigente.
- [x] 6.2 Presentar las cuatro secciones, fecha/cobertura y fuentes desplegables con fragmentos exactos y recortes, usando componentes existentes, CSS Modules y tokens; eliminar el botón inicial y ofrecer «Reintentar» sólo ante fallos recuperables, respetando esperas y doble clic. Verificar con Vitest fuente fuera de página, sección vacía, error/reintento, ausencia de HTML interpretado y controles por teclado/foco; documentar generación automática, TTL y límites de frescura en frontend README.
- [x] 6.3 Integrar invalidación por actividad nueva, edición, reasignación, etapa, cierre y reapertura, con una actualización automática serializada sólo en el detalle activo y autorizado. Mantener resultados válidos de origen al navegar, agrupar señales de la misma mutación y limpiar caché/tareas al cambiar de sesión. Verificar Vitest para cada señal, actividad sin cambio de versión, mutación durante generación, cambio de cuenta/ruta, vuelta al mismo ID y respuestas tardías que no repueblan otra sesión ni otro contexto.
- [x] 6.4 Agregar E2E con proveedor HTTP simulado inyectado sólo en pruebas: entrar y obtener resumen sin clic, revisar fuentes, salir/volver sin generación duplicada, registrar actividad y actualizar automáticamente, error con reintento manual, cuota, timeout y función apagada. Verificar API/PostgreSQL efímeros, tres roles, 390 px/escritorio, ambos temas, sin errores de consola ni scroll horizontal, y conservación de SEO noindex del detalle.

## 7. Evaluación real y documentación operativa

- [x] 7.1 Preparar guía de configuración y corpus manual ficticio con criterios de fidelidad, contradicciones, datos faltantes, recortes e instrucción maliciosa; verificar enlaces de Google y que explique el tratamiento de datos de Free Tier sin prometer anonimización de texto libre.
- [ ] 7.2 Con credencial nueva configurada fuera del repositorio, comprobar en AI Studio Free Tier, cuotas reales y una generación mínima serial de `gemini-3.1-flash-lite`. Registrar fecha/modelo/latencia/estado sin clave ni payload sensible. Si no hay clave nueva o el proveedor no responde, dejar esta verificación pendiente y avanzar con las independientes; no activar facturación ni usar la credencial expuesta.
- [ ] 7.3 Evaluar el corpus de fidelidad con el primario viable y ajustar prompt/límites según evidencia; verificar cada afirmación con sus fuentes y rechazar invenciones críticas. Registrar llamadas y fallos sin atribuir disponibilidad futura a una muestra pequeña.
- [ ] 7.4 Probar `gemini-3.5-flash-lite` con el mismo esquema y casos mínimos antes de habilitarlo como alternativa; verificar Free Tier/cuota y documentar comparación bajo condiciones equivalentes. Si no está accesible, conservar fallback vacío y registrar esa limitación.
- [x] 7.5 Actualizar contexto y punto de retomada, documentación backend/frontend y privacidad si corresponde con el flujo de datos a Gemini, instalación, errores y apagado de la función. Verificar rutas/enlaces y distinguir validaciones propias, simuladas, históricas y reales; no certificar el entorno remoto ni dar por cerrado el pendiente comercial desplegado.

## 8. Validación integrada y entrega del cambio

- [x] 8.1 Ejecutar `./mvnw.cmd clean verify` desde backend con JDK 25 y Docker, incluyendo nuevas unitarias/IT y regresión comercial. Registrar resultado de esta sesión y límites; no requieren Google, Supabase ni credenciales externas.
- [x] 8.2 Ejecutar `npm run build`, `npm run lint`, `npm test` y `npm run test:e2e` desde frontend con preparación de README y proveedor simulado. Registrar resultados, recorrido de las rutas tocadas y conservación de metadatos públicos; no contar el mock como prueba de Gemini real.
- [x] 8.3 Ejecutar `openspec validate incorporar-resumen-comercial-ia --strict`, `git diff --check` y revisar `git status --short`; revisar delta final y documentación para asegurar que los cambios preexistentes se respetan y ningún secreto fue agregado. Entregar evidencia y pendientes sin commits, pushes ni despliegue.

## Workflow follow-up

- Aplicar este plan cuando el usuario pida implementación; esta propuesta sólo crea artefactos de planificación.
- Archivar y sincronizar specs cuando corresponda y se solicite, preservando las verificaciones reales que hayan quedado pendientes.
- La publicación requiere un pedido explícito separado; al realizarla, comprobar el flujo autenticado remoto y el apagado de IA sin afectar al CRM.

## Revisión posterior — persistencia solicitada el 31/10/2026

Las tareas anteriores conservan el registro de la implementación inicial. ADR-008
reemplaza su decisión de resumen efímero y reutilización de cinco minutos.

- [x] Persistir el último éxito por tenant/oportunidad mediante V6, con huella, versión de prompt y respuesta completa; conservar permisos y red fuera de transacciones.
- [x] Consultar vigencia al volver o recargar, sin regenerar si no cambió el contexto; actualizar contrato, tipos, privacidad y documentación técnica.
- [x] Verificar con PostgreSQL la reutilización entre roles, proveedor caído, cambio de notas, actividad sin incremento de versión, renombre de etapa y conservación del último éxito ante fallos.
- [x] Verificar build, 17 pruebas frontend y 2 E2E con recarga, retorno, tres roles, actividad, errores y privacidad; ver [evidencia de persistencia](validacion-persistencia-2026-10-31.md).
