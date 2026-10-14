# Validación de §10 — 10/10/2026

Continuación autorizada desde §9, con límite en §10. Se conservan los cambios
anteriores del checkout. Esta evidencia corresponde a esta sesión; la validación
de §9 en [validacion-2026-10-10.md](validacion-2026-10-10.md) sigue siendo histórica.

## Carga, consultas y caché

- `Rutas.tsx`: pantallas divididas con `lazy`/`Suspense`, detrás de las guardas.
  Red real confirma que el panel no descarga código de formularios, usuarios ni
  tablero inactivos. El build deja de advertir un chunk superior a 500 kB.
- Panel: sólo `/opportunities?page=0&size=5&sort=createdAt,desc` y ocho actividades.
  Sin tablero, indicadores ni ayuda técnica. Cada lista tiene carga/vacío/error
  y reintento propios; un 403 de actividades deja las oportunidades utilizables.
- IDs inválidos no solicitan detalle/historial/disponibilidad. Un 403 tiene una
  única solicitud automática. Volver al panel dentro de `staleTime` no consulta.
- Enriquecimiento de oportunidades mediante lotes por service y mapas locales;
  servicios adicionales mediante IDs autorizados de la página, sin N+1 de
  colecciones ni joins entre módulos. Referencias históricas conservadas.
- `OpportunityLoadIT`: 100 oportunidades y referencias distintas por fila.
  ADMIN: **9 SQL con 20 y 100 filas**, tablero **11 SQL**. SELLER: **11/10 SQL**,
  respectivamente; la página de 100 contiene sus 50 oportunidades y puede omitir
  el count al no llenarse. Incluye SQL de autenticación; no crece por fila/campo.
  Listado/ID del segundo tenant excluidos y nombres del vendedor ajeno ausentes.
- Pruebas frontend de claves por embudo/filtro/página, búsqueda cancelada,
  reutilización, invalidaciones y cierre/nuevo login. Respuestas tardías 200/401
  de otra cuenta se descartan sin poblar caché ni cerrar la sesión vigente.

## Errores y observabilidad

`ObservabilityIT` captura eventos reales de Logback y HTTP 400/401/403/404/409/422:
validación, rol insuficiente, ID ajeno, transición incompatible, fecha futura y
actividad sin cliente. `requestId` coincide entre header, ProblemDetail y MDC;
los eventos autenticados tienen actor/tenant propios y el rechazo anónimo no
hereda ninguno. No hay cuerpos, contraseñas, JWT, emails, SQL ni datos ajenos en
los mensajes capturados; las respuestas no incluyen trace/exception/SQL/actor.

Los logs registran método/ruta sin query/estado/duración y códigos de error.
MDC se limpia en `finally`, también al exigir cambio inicial de contraseña.
Los errores de framework usan mensajes genéricos españoles. SQL/binds/diagnósticos
de Hibernate deshabilitados en configuración base y local; medición SQL sólo en test.

## Verificación ejecutada

- JDK Temurin 25.0.4.1 y Docker Desktop; PostgreSQL 16 efímero, sin `.env` ni base remota.
- Backend `clean verify`: **5 unitarias + 108 IT**, sin fallos, errores ni omitidos.
  Maven terminó `BUILD SUCCESS`. Failsafe informó cierre forzado del fork tras
  30 segundos durante el apagado de los muchos contextos/Hikari; las pruebas ya
  habían finalizado. No se atribuye ese diagnóstico a una operación del CRM.
- Frontend: **45 pruebas**, lint y build correctos. E2E: **21/21** correctos contra
  API/PostgreSQL reales efímeros, incluidos los nuevos negativos de carga.
- Recorridos a 390/1440 px, ambos temas, teclado, consola y noindex; metadatos
  públicos correctos. Capturas propias del panel móvil/escritorio revisadas.
- OpenAPI runtime/export iguales salvo puerto efímero. No cambió el contrato HTTP
  en §10; no se reemplaza el export sólo para cambiar un puerto de prueba.
- Tras el ajuste final de configuración de logs: **5 unitarias + 2 IT** focalizadas
  correctas, nuevamente BUILD SUCCESS. Build habitual restaurado tras medir.
- OpenSpec `validate --strict` y `git diff --check` correctos; 40 enlaces locales
  de diez documentos afectados resueltos. `git status --short` revisado, conservando
  los cambios previos del checkout. Progreso final: 64/72; los ocho checks de §11
  siguen pendientes.

## Comparación reproducible (§10.5)

La [comparación completa](evidencias/comparacion-carga-punto-10.md) presenta solicitudes,
bytes totales/API/JS, SQL, navegación, espera/servidor/descarga y arranque separado.
Mismo Chromium 153.0.8010.12, dataset y condiciones de §1.2; tres corridas por ruta,
servidor y caché. Se ejecutaron 48 muestras iniciales y se repitieron las 24 de
servidor despierto al detectar chunks de Nosotros dentro de la ventana siguiente.
La corrección espera heading propio y red quieta antes de medir; conserva las
fuentes y no descuenta bytes manualmente. El informe usa 48 muestras válidas.

Panel: 745→28 SQL y aproximadamente 350.594→246.000 bytes totales. Lista: 157→44
SQL, aproximadamente 312.139→267.437 bytes. Las vueltas calientes finales tienen
cero solicitudes/bytes/SQL. Detalle y alta no mejoran todos sus conteos/tiempos
respecto del inicio del apply: se documenta el costo de funcionalidades de §2–§9.
Los totales incluyen fuentes externas de Google Fonts; API y JS propios se separan.

## Límites

Estos checks validan §10 y su regresión técnica; no completan el checklist integral
de §11, ni la demostración publicada. Sin commits, staging, branches, pushes, tags,
deploy, archive o sync. Las mediciones locales no representan latencia ni suspensión
de Render/Supabase y no establecen SLA.
