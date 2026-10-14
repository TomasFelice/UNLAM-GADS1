# Estado archivado — actualizado el 13/10/2026

El usuario confirmó testing integral local satisfactorio y solicitó el cierre
el 13/10/2026. Cambio archivado con nueve specs principales sincronizadas y
**71/72 tareas**: §1–§10 y **11.1–11.7** implementados y verificados;
**11.8 pendiente** con autorización expresa. Ver [cierre](cierre-2026-10-13.md).
[Tasks](tasks.md) conserva el progreso y la
[validación de §11](validacion-punto-11-2026-10-10.md) registra la sesión del 10/10.
La [validación de §10](validacion-punto-10-2026-10-10.md),
la [validación de §9 del 10/10](validacion-2026-10-10.md) y la
[del 09/10](validacion-2026-10-09.md) conservan evidencia anterior sin reatribuirla.

## Implementación cerrada

- §1–§4: ADR-006, línea base/fixtures, administración de usuarios, CUIT V2,
  clientes con alcance e historia y ABM ADMIN de salones/servicios.
- §5–§6: cuatro catálogos paginados, selectores por ID, V3 embudos propios,
  predeterminado, plantillas, etapas históricas y orden atómico. Pipeline fijo
  de oportunidad; tablero/filtros por embudo. Puertos sin ciclos entre módulos.
- §7: selectores remotos y alta contextual desde clientes con fechas reales,
  herencia, opcionales expandibles y protección ante respuestas tardías.
- §8: V4 snapshots, versión esperada, asignación/cierre/reapertura, disponibilidad,
  GiST y conflictos recuperables. Cierres sucesivos conservan valor/motivo originales.
- §9: OpportunityContext autorizado como record de service; alta append-only de
  actividades, asociaciones derivadas, actor/tenant del servidor y fecha no futura.
  Visibilidad de oportunidad ajena corregida aun con cliente compartido. Alta en los
  tres detalles; históricos y relaciones de clientes paginados con desempates por ID.
  V5 indexa cronología. Bajas y autores inactivos conservan lectura; historia de etapas
  presenta cambios, notas, autores y ambos cierres con reintento de consulta.
- OpenAPI runtime/export, tipos derivados, Postman, README y diagrama de componentes
  actualizados. Se preservan todos los cambios previos sin commit.
- §10: pantallas lazy por ruta, panel de cinco oportunidades/ocho actividades sin
  tablero, errores independientes y reintento. Enriquecimiento por lotes y colecciones
  sólo de IDs autorizados; 9 SQL con 20/100 oportunidades ADMIN. Caché, cancelación,
  invalidaciones y cambio de cuenta cubiertos, incluso respuesta tardía 401.
  ProblemDetail y logs correlacionados sin datos sensibles, SQL deshabilitado.
  [Comparación de carga](evidencias/comparacion-carga-punto-10.md): panel 745→28 SQL,
  lista 157→44; 48 muestras válidas, límites y corrección de muestreo documentados.

## Validación propia de esta continuación (§11)

- Backend: `clean verify` con **5 unitarias + 108 IT**; después de extender los
  fixtures y la prueba de BCrypt, `verify` completo con **5 unitarias + 109 IT**.
  Cero fallos/errores/omitidos. Instalación vacía, actualización V1→V5 con datos
  comerciales de dos tenants, reejecución y GiST verificados sin modificar V1.
- Frontend: **45 pruebas**, lint y build correctos; **24 E2E** completos contra
  API/PostgreSQL efímeros. Login por UI, clientes, alta breve, tablero, actividad,
  cierre y recarga persistida. **576 combinaciones** de 48 rutas × tres roles ×
  390/1440 px × dos temas; teclado, consola, scroll y SEO verificados.
- Newman importa colección/environment: **99 solicitudes y 109 aserciones**.
  Se corrigieron paginación de origen, IDs fijos y query efectiva de URL.
  Cobertura de las 69 operaciones runtime; YAML equivalente y tipos sin cambios.
- Copia limpia de frontend: npm ci/test/build y lint explícito correctos.
  Imagen Docker y Compose arrancados con PostgreSQL efímero, health/login,
  migraciones y GiST correctos; sin conexión remota ni publicación.
- Matrices de 16 EF, 13 PDF y 15 reglas, informes JSON y capturas revisadas en
  la validación enlazada. Documentación reconciliada, enlaces locales/OpenSpec
  estricto/diff/status correctos. Build habitual restaurado y servicios cerrados.

## Validación anterior (§10, histórica)

- Backend clean verify: **5 unitarias + 108 IT** correctas. Tras ajustar configuración
  de logs: **5 unitarias + 2 IT** focalizadas correctas. Cierre lento de fork en la
  suite completa documentado; Maven BUILD SUCCESS, cero fallos/errores/omitidos.
- Frontend: **45 pruebas**, lint y build correctos, sin chunk >500 kB; **21 E2E**.
  Panel revisado visualmente a 390/1440 px; ambos temas, teclado, consola, noindex
  y metadatos públicos verificados. OpenAPI igual salvo puerto efímero.
- Medición local con Chromium/dataset/condiciones de §1.2: 48 muestras iniciales
  y 24 repetidas al corregir la frontera de navegación lazy, 48 válidas en el informe.
  Cero red en todas las vueltas calientes finales. Google Fonts externo puede variar.
- Build habitual restaurado, servicios efímeros cerrados, OpenSpec strict y
  comprobaciones de enlaces/diff/status. Sin avances en §11.

## Validación anterior (§9, histórica)

- Backend: clean verify con **5 unitarias + 106 IT**, cero fallos/errores/omitidos.
  Luego, negativo adicional de oportunidad del segundo tenant: **5 unitarias + 7 IT**
  pertinentes correctas. Incluye páginas con empates y relaciones de más de 20 registros.
- Frontend: **36 pruebas**, lint sin advertencias y build correcto.
- Regresión completa: **20 E2E** correctos contra API/PostgreSQL efímeros. Tres roles,
  tres altas contextuales, oportunidad cerrada, 30 actividades/26 oportunidades/26
  contactos de un cliente, bajas y páginas posteriores. Ciclo completo con cambio,
  ganada, reapertura y pérdida. Postman captura referencias sin ajustes manuales.
- Rutas a 390/1440 px, ambos temas, teclado, consola, noindex y metadatos públicos.
  El aviso de bundle >500 kB estaba pendiente en esta captura de §9; se resolvió en §10.
- Capturas móvil/escritorio revisadas, build habitual restaurado, contrato/tipos
  regenerados, OpenSpec strict y comprobaciones de enlaces/diff/status correctos.

## Entorno y límites

JDK 25 y Docker Desktop disponibles.
Testcontainers usa PostgreSQL 16 efímero; sin .env ni Supabase remoto. Después de clean,
generar target/baseline-classpath.txt según README frontend y esperar la compilación
antes de iniciar E2E. Instalar Newman aislado en `backend/target/newman` siguiendo
el README frontend. El launcher usa API 18080 y preview 14173, los abre/cierra solo.
E2E requiere build con VITE_API_URL=http://localhost:18080/api/v1; restaurar build
habitual al finalizar y no cambiar dist mientras preview ejecuta pruebas.

Se completaron 11.1–11.7. **11.8 queda pendiente para el usuario**, quien autorizó
el archivo con ese límite. El cierre sólo sincroniza specs y archiva documentación.
Sin commits, staging, ramas, pushes, tags ni deploy.
Conservar .agents/, .claude/, openspec/ y PDF sin seguimiento.
