# Despliegue de producción — 31/10/2026

## Versión y destinos

El usuario autorizó commits, pushes y despliegue de los cambios existentes. Commit
de aplicación: `a84f76ace33d261d863be90982b93d3319c7a73c`, publicado en
`feature/faltantesEntregaFinal` y en `main` por avance directo, sin reescribir historia.

- Frontend: <https://ztech-crm.benai.com.ar>, proyecto Vercel `frontend`.
  Despliegue `dpl_8AY34CNrPe31cL9RhAaFzmsDo3Td`, producción, `READY`.
- API: <https://unlam-gads1.onrender.com>, servicio
  `srv-dam9fmijnfac73ea65ng`. Despliegue
  `dep-dbj14afdnq4s73dmnceg`, correspondiente al commit de aplicación, `live` desde
  las 13:14:06 de Buenos Aires. Duración desde su inicio: 4m07s.
- Base existente: Supabase `zlduzfjmdmafpenidpss`, mediante la conexión de Render.
  El cambio no agrega migraciones ni modifica V1–V5.

## Configuración de IA

Se copiaron exclusivamente las ocho variables de IA de `backend/.env` al servicio
Render, mediante actualización por mezcla. La clave permanece fuera de Git,
documentación y frontend. Se conservaron las demás variables existentes.

| Variable | Configuración publicada |
|---|---|
| `AI_SUMMARY_ENABLED` | `true` |
| `GEMINI_API_KEY` | Configurada; valor omitido |
| `GEMINI_SUMMARY_MODEL` | `gemini-3.1-flash-lite` |
| `GEMINI_SUMMARY_FALLBACK_MODEL` | Vacío |
| `AI_SUMMARY_TIMEOUT_SECONDS` | `40` |
| `AI_SUMMARY_MAX_ATTEMPTS` | `3` |
| `AI_SUMMARY_MAX_CALLS_PER_MINUTE` | `3` |
| `AI_SUMMARY_MAX_CALLS_PER_DAY` | `20` |

Se mantuvo una instancia y no se activó facturación ni se modificaron cuotas del
proveedor. La confirmación de Free Tier y la llamada real local corresponden a la
[sesión previa](../openspec/changes/incorporar-resumen-comercial-ia/validacion-2026-10-31.md).
Este despliegue no completa la evaluación de fidelidad ni habilita el fallback.
Para apagar la función, guardar `AI_SUMMARY_ENABLED=false` y desplegar la aplicación
actual. Un simple reinicio de Render conserva las variables del despliegue anterior.

## Secuencia y límites

La actualización de variables mediante el conector Render inició automáticamente
`dep-dbj13pnf3r2c73eluh80` sobre el antiguo `main` (`957032a`). Se publicó el commit
nuevo en `main` y se solicitó el despliegue correcto, conservando Auto-Deploy apagado.
El push también generó una compilación automática Vercel del mismo commit.

El conector Supabase no incluye el proyecto del CRM entre los proyectos accesibles;
no se realizaron cambios SQL ni se afirma una inspección directa de su historial.
La conexión y las migraciones se verifican mediante el arranque de Render.

Por indicación expresa del usuario, las pruebas funcionales en producción quedan
a su cargo. No se inició sesión, no se generaron resúmenes con datos remotos ni se
crearon registros comerciales. Las pruebas locales usan PostgreSQL efímero y un
proveedor Gemini simulado; no acreditan disponibilidad real del proveedor.

## Verificación de esta sesión

- Frontend: build, lint y 64 pruebas Vitest correctos.
- Backend: `clean verify` ejecutó 21 unitarias correctas y 114 IT, con un fallo en
  la aserción de saneamiento de logs. El entorno del proceso tenía `DEBUG=release`:
  Spring activó DEBUG y el SQL de creación de fixtures contenía el texto ficticio
  prohibido por la aserción. Sin esa variable, se repitió `CommercialSummaryIT`
  mediante Failsafe: cinco pruebas correctas y `BUILD SUCCESS`, sin cambios de código.
  No se presenta la primera corrida completa como exitosa.
- OpenSpec: `openspec validate incorporar-resumen-comercial-ia --strict`, correcto.
- Revisión del diff preparado: sin los secretos locales de base, JWT o Gemini.
- Dominio público frontend: HTTP `200`, bundle `index-CQJUsyT9.js`.
- El bundle publicado usa `https://unlam-gads1.onrender.com/api/v1`, sin la URL E2E.
- Logs de la nueva instancia Render: Flyway validó cinco migraciones, encontró
  `public` en V5 y confirmó que no había migraciones pendientes.
- Render confirmó el despliegue como `live`.
- `/actuator/health` posterior al estado `live`: HTTP `200`, `status: UP`.
- La consulta posterior de logs con nivel `error` falló con HTTP `503` del servicio
  de consulta Loki de Render; no se afirma ausencia de errores a partir de ella.
- `npm run test:e2e`: 27/27 correctos en 9,8 minutos. Incluye ambos recorridos IA,
  tres roles, rutas a 390 px/escritorio, temas, consola y metadatos públicos.
- `git diff --check`, revisión del estado y enlaces locales de los tres documentos
  de despliegue/contexto modificados: correctos.

El commit posterior que versiona este registro sólo modifica documentación; el
commit de aplicación verificado es el indicado arriba.
