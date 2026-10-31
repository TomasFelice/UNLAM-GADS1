# Validación del apply — 31/10/2026

## Alcance implementado

ADR-007 y DP-12 registran el coordinador `assistance → {activities, opportunities}`.
El endpoint obtiene contexto autorizado en una transacción breve `REPEATABLE_READ`,
invoca REST Gemini fuera de transacción y revalida permiso y huella antes de responder.
Fuentes y cobertura se construyen desde el snapshot, sin escrituras comerciales.
Configuración apagada por defecto, modelos explícitos, fallback vacío, cuotas por
proceso, respuesta acotada, retries y plazos limitados. No hay migraciones.

El detalle frontend revalida acceso/versión y genera automáticamente con una consulta
específica, deduplicación, TTL de cinco minutos en memoria, revisión de fuentes y
reintento manual. Las mutaciones comerciales agrupan invalidaciones y serializan
la actualización. El contrato estático se exportó desde runtime local y se regeneraron
los tipos; no se escribieron DTOs de resumen duplicados en frontend.

## Pruebas de esta sesión

| Validación | Resultado |
|---|---|
| `./mvnw.cmd clean verify` con JDK 25 y Docker | Código 0; 20 unitarias y 114 IT, sin fallos/errores/omitidas |
| Unitarias y empaquetado posteriores a ajustes de configuración/validador | Código 0; 20 unitarias |
| `OpenApiExportIT` y copia de `target/openapi.yaml` | Export real de `/v3/api-docs.yaml`; 70 operaciones |
| `npm run generar:api` | Tipos derivados del contrato actualizado |
| `npm run build` | Correcto |
| `npm run lint` | Correcto |
| `npm test` | 64 pruebas, 16 archivos, correctas |
| E2E específicos de resumen | 2 correctos; tres roles, ambos temas, 390 px/escritorio |
| `npm run test:e2e` completo | 25/27 correctos inicialmente; dos verificaciones corregidas y reejecutadas, ambas correctas (detalle abajo) |
| `openspec validate incorporar-resumen-comercial-ia --strict` | Correcto |
| `git diff --check` y revisión de enlaces locales | Correctos; sin enlaces locales rotos en Markdown modificado |

Los IT nuevos cubren accesos por tenant/vendedor, cliente compartido, corte de 50
en consulta y desempate por ID, conteos/cobertura, ausencia de escrituras por SQL,
snapshot consistente ante inserción concurrente, reasignación/etapa/actividad durante
la generación, salud con función deshabilitada/configuración inválida y saneamiento
de logs/errores. El proveedor es HTTP local simulado. Se verifica que no exista
transacción durante la llamada externa.

Las unitarias cubren Unicode, presupuesto, huella, esquema/referencias, transporte
HTTP limitado (incluida lectura lenta), cuotas con reloj, concurrencia de admisión,
clasificación, retries/fallback y plazo sin esperas reales. Vitest cubre acceso
revalidado, StrictMode/remontajes, TTL, foco/reconexión, caché por ruta/sesión, espera
de cuota, errores retenidos, cada mutación comercial y respuestas tardías.

Playwright específico recorre generación sin clic → fuentes por teclado → salir y
volver sin duplicar → registrar actividad → actualización; además prueba credencial
upstream rechazada sin cerrar sesión, cuota, timeout y apagado. Sin errores de consola
en el recorrido exitoso, sin scroll horizontal y conservando noindex. Las capturas
del panel a 390 y 1440 px se inspeccionaron visualmente. Los fallos HTTP intencionales
del recorrido negativo no se confunden con fallos del recorrido exitoso.

La regresión completa ejecutó 27 E2E en 7,6 minutos. Detectó dos ajustes necesarios:
la colección Postman no incluía todavía el endpoint nuevo, y el test de entrega
buscaba un único `status` global aunque ahora hay carga independiente de IA. Se
agregó la solicitud Postman (sin body/clave, validando éxito o apagado) y se acotaron
las aserciones de actividad a la región Historial comercial; la fuente puede mostrar
el mismo fragmento sin que eso duplique actividades. No se retiraron controles.
`npx playwright test entrega-integrada.spec.ts postman-entrega.spec.ts` ejecutó ambos
casos corregidos contra una API y PostgreSQL nuevos: **2/2 correctos**. En conjunto
los 27 escenarios quedaron comprobados; no se presenta esa reejecución como una
segunda corrida completa sin fallos. La revisión de rutas comprobó 576 combinaciones
de rol/ruta/tamaño/tema y el test público conservó title/description/canonical.
Se comprobó también que el JAR de producción excluye launcher, stub y controles E2E.

## Límites y pendientes

- Docker falló inicialmente por `dockerInference`; el usuario lo inició y después
  se ejecutaron los IT contra PostgreSQL de Testcontainers. El intento alternativo
  local no llegó a iniciar ninguna base ni a tocar datos existentes.
- Al finalizar `clean verify`, Surefire informó que forzó la salida del fork después
  de 30 segundos de cierre de JVM. Failsafe registra 114 completados, cero fallos,
  cero errores y `timeout=false`; Maven terminó con código 0. No se oculta ese aviso.
- No se llamó a Google ni se utilizó la credencial expuesta. No se comprobó una
  credencial nueva/Free Tier en AI Studio; §7.2, §7.3 y §7.4 siguen pendientes.
  El fallback permanece vacío y la función apagada por defecto. El corpus ficticio
  y su guía están en el backend; mocks no prueban fidelidad ni disponibilidad real.
- No se certificó producción ni se cerró la demostración comercial autenticada
  remota pendiente. Sin commits, pushes, publicación ni cambios en facturación.
- Se conservaron los cambios preexistentes en README/backend/frontend, contexto,
  índice documental, tareas backend, despliegue y planificación OpenSpec.

## Retomada

Implementación y verificaciones independientes completas: **28/31 tareas**. Sólo
siguen abiertas §7.2–§7.4, por falta de una credencial nueva cuya configuración y
Free Tier hayan sido confirmados. Se conserva el cambio activo, sin archivarlo.

Con credencial **nueva**, configurada fuera del repositorio y Free Tier comprobado
en AI Studio, realizar la llamada mínima serial de §7.2 y evaluar el corpus de §7.3.
Registrar modelo, fecha, cuotas, número de llamadas, latencia y resultado sin claves
ni payload sensible. Probar el candidato alternativo en condiciones equivalentes
antes de habilitarlo (§7.4). No cambiar a pago ni reutilizar la clave expuesta.
Guía: [resumen comercial IA](../../../backend/docs/resumen-comercial-ia.md).

## Continuación — configuración local y prueba real del 31/10

El usuario proporcionó una credencial y autorizó configurarla; confirmó que el
proyecto figura en Free Tier sin facturación activa. Se guardó exclusivamente en
`backend/.env`, comprobado como ignorado por Git, con `AI_SUMMARY_ENABLED=true`,
primario `gemini-3.1-flash-lite` y fallback vacío. No se publicaron variables ni se
modificó producción. Para aplicar la configuración al CRM hay que reiniciar la API.

Se corrigió la validación local de formato para admitir el punto de las claves de
autorización. Una prueba con credencial **ficticia** verifica ese formato y el rechazo
de espacios/saltos de línea. `./mvnw.cmd test`: **21 unitarias correctas**.

Con datos exclusivamente ficticios, el cliente REST de producción y su prompt/schema:

| Comprobación real | Resultado |
|---|---|
| Consulta de modelos con cabecera de clave | HTTP 200; ambos candidatos figuran disponibles |
| Primera generación mínima del primario | Timeout a 10.302 ms, sin resultado entregado |
| Segunda generación mínima, serial | Correcta a 6.892 ms; 348 tokens de entrada, 144 de salida |
| Validación backend de la segunda respuesta | Cuatro secciones válidas; tres elementos con referencias autorizadas |
| Diagnóstico HTTP JDK de consulta de modelos | HTTP 200 a 1.956 ms |

Se realizaron dos intentos de generación, sin fallback, sin datos del CRM y sin
imprimir ni versionar la clave. La muestra no acredita fidelidad del corpus ni
disponibilidad futura. La consulta de modelos no demuestra que cada uno tenga cuota
de generación en el proyecto. No se registraron valores de cuotas de AI Studio:
§7.2 queda parcialmente verificado; §7.3 y §7.4 siguen pendientes. Esta continuación
actualiza el estado de las verificaciones reales que estaban pendientes arriba.
