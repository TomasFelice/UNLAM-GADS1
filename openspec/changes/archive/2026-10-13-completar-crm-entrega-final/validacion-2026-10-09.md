# Validación del apply — 09/10/2026

Resultados ejecutados en esta sesión, en Windows con Temurin 25.0.4.1, Docker Desktop
29.0.1 y PostgreSQL 16 de Testcontainers. No se usaron Supabase ni credenciales reales.
El estado completo se mantiene en [tasks.md](tasks.md).

## Suites previas a los cambios de comportamiento

| Comando | Resultado propio |
|---|---|
| `backend/`: `./mvnw.cmd clean verify` | Correcto: 5 unitarias y 40 IT, ninguna fallida/omitida; 2 min 53 s |
| `frontend/`: `npm test` | Correcto: 14 pruebas en 6 archivos |
| `frontend/`: `npm run lint` | Correcto con advertencia previa `react(incompatible-library)` en `PaginaOportunidadFormulario.tsx` |
| `frontend/`: `npm run build` | Correcto; advertencia previa de chunk JS de más de 500 kB: 553,08 kB, gzip 164,87 kB |

Faltaban dependencias locales del frontend: el primer intento no encontró `vitest`,
`oxlint` ni `tsc`. Se ejecutó `npm ci` con el lockfile existente y se repitieron los
tres checks satisfactoriamente. No se detectaron tests fallidos de la línea base.
El export generado por `OpenApiExportIT` queda en `backend/target/openapi.yaml`;
no cambia el contrato versionado en este corte.

## Fixtures reutilizables

`backend/src/test/java/com/ztech/crm/support/CommercialFixtures.java` instala dos
tenants independientes adicionales al seed V1. Cada uno tiene administrador,
responsable comercial, dos vendedores y empresa/contacto compartidos con oportunidades
y actividades de distintos responsables. Usa PasswordEncoder y una contraseña pública
exclusiva de pruebas; no agrega endpoints ni código de producción.

`./mvnw.cmd -Dit.test=CommercialFixturesIT verify`: correcto, 5 unitarias existentes
y 1 IT nuevo. El IT autentica las ocho cuentas, comprueba listados de oportunidades
según rol/responsable, acceso al cliente compartido y `404` por ID a oportunidad y
empresa del otro tenant en ambas direcciones. Esto valida la infraestructura de
fixtures; no sustituye los negativos específicos de escrituras y actividades pendientes.

## ADR y documentación

[ADR-006](../../../../docs/decisiones/adr/ADR-006-multiples-embudos.md) registra el modelo
aceptado de múltiples embudos. Índice ADR, DP-06 y requisitos/diseño backend distinguen
la ampliación aceptada de su implementación pendiente. No se modificó V1 ni el runtime.

## Línea base de carga

Tarea 1.2: se capturaron [48 mediciones](evidencias/linea-base-carga.md), tres por
combinación de ruta, servidor y caché, con solicitudes/bytes/tiempos/SQL. El informe
describe reproducción, arranque JVM separado y límites locales. Aún no hay comparación
posterior ni cambios de rendimiento implementados.

## Administración de usuarios — corte §2

Se implementaron menú y guardas por rol, listado/detalle y alta/edición/baja/reset
sobre el contrato existente, con tipos derivados del export. Los filtros permanecen
en URL; se corrigió `useBusquedaUrl` para que montar un listado no resetee su página.
El cierre de sesión ahora centraliza su destino/mensaje: el E2E detectó y permitió
corregir una carrera entre la guarda y el cambio de contraseña que devolvía al
formulario de clave después de volver a ingresar.

- `UserAdministrationIT`: 2 IT correctos con política de cambio obligatoria habilitada.
  Comprueba listado/ID/alta/edición/reset para vendedor y responsable comercial (`403`),
  consulta/escrituras cross-tenant (`404`) en ambas direcciones, último admin, baja
  propia, revocación y conservación de autoría en actividades/historial tras baja.
- `npm test`: 23 pruebas correctas en 8 archivos (incluye guardas y listado de usuarios).
- `npm run lint`: correcto; permanece sólo la advertencia previa del formulario de oportunidades.
- `npm run build`: correcto; persiste advertencia de bundle >500 kB, pendiente de §10.
- Playwright: 4 E2E correctos contra API/PostgreSQL reales de prueba. Alta→login temporal
  →cambio obligatorio→nuevo login, edición, reset, revocación y baja; roles comerciales
  rechazados por URL/menú/HTTP; las cuatro rutas de usuarios a 390 px en ambos temas,
  foco por teclado, consola sin errores y noindex; metadatos presentes por ruta pública.

No se modificó OpenAPI: el corte consume endpoints existentes. Configuración E2E
incluida para estos flujos; el recorrido comercial de la tarea 11.1 sigue pendiente.

## Validación de clientes y migración — tareas 3.1/3.2

La API valida CUIT opcional con dígito módulo 11 y formato completo con/sin guiones,
comprueba unicidad y persiste sin guiones. Empresas/contactos normalizan opcionales
antes de Bean Validation; email no vacío debe tener formato válido y el sitio debe
ser URL absoluta HTTP/HTTPS. No se agregaron límites de longitud arbitrarios.

`CompanyControllerIT` tiene 10 casos correctos y `ContactControllerIT` 6: dígito
incorrecto, presentación incompleta, resto cero y resto uno, equivalentes por alta
y edición, actualización propia, dos tenants, blancos y email/URL válidos e inválidos.
Se corrigieron ejemplos de CUIT del DTO, placeholder y dos requests de Postman;
no se afirma haber recorrido toda la colección.

V2 conserva identidades y relaciones, normaliza presentación y detecta colisiones
por tenant bajo bloqueo antes de actualizar. No reescribe V1 ni inventa verificadores
para sus CUIT ficticios incorrectos. Al editar esos registros, debe corregirse u
omitirse el CUIT opcional. `MigrationV1IT` fija explícitamente `target("1")` para
seguir comprobando la instalación histórica.

El primer ensayo de `CuitMigrationIT` detectó que el test asumía IDs del seed en
orden fijo: SQL no garantiza ese orden. Se corrigió para buscar las empresas por
CUIT; después se corrigió su preparación para asignar el blanco a una sola fila,
respetando el índice V1. Se conservaron las aserciones de rollback y relaciones.

Regresión final de backend: `./mvnw.cmd clean verify dependency:build-classpath
-Dmdep.outputFile=target/baseline-classpath.txt -Dmdep.includeScope=test`, correcta
en 3 min 12 s: **5 unitarias + 50 IT**, ninguna fallida/omitida. Incluye instalación
vacía V1/V2, actualización desde V1, normalización de espacios/tabulaciones/saltos,
equivalentes permitidos en otro tenant, reejecución sin cambios y rollback de colisión
sin pérdida de registros. V1, IDs, relaciones, actividades, historial y GiST se conservan.

Se exportó `target/openapi.yaml` al contrato versionado y se ejecutó `npm run generar:api`.
Frontend: 23 tests correctos, lint con la advertencia previa y build correcto con la
advertencia previa de bundle (565,36 kB). La colección Postman es JSON válido y sus ocho
cuerpos son JSON válidos sustituyendo placeholders; no se ejecutó toda la colección.
Los enlaces locales de la documentación modificada se comprobaron sin enlaces rotos.
E2E repetido después de 3.1/3.2: **4 correctos**, 39,5 s, contra API/PostgreSQL de
prueba con V2. El build de prueba usó puerto 18080; luego se restauró un build normal.
`openspec validate completar-crm-entrega-final --strict` y `git diff --check` correctos.
Pausa solicitada por el usuario: 11/72 tareas completas, siguiente tarea 3.3;
ver [retomada](retomada.md). Sin commits, pushes, deploy ni escrituras remotas.

## Retomada: datos comerciales — tarea 3.3

Se retomó desde 11/72 sin descartar los cambios existentes. La tarea 3.3 agrega origen
al formulario de empresa y nombres de responsables en listado/detalles autorizados.
El listado resuelve identidades por lote del tenant; no amplía `/users/assignable`.
Se usa el contrato `access/service/UserIdentity` para los nuevos consumidores.
Empresas/contactos rechazan orígenes ajenos (404) o nuevas asociaciones inactivas
(422); permiten conservar la referencia inactiva anterior. No se modificó V1/V2.

- Primera verificación: 5 unitarias y 19 IT correctos (CompanyControllerIT,
  ContactControllerIT, CustomerManagementIT y OpenApiExportIT).
- Tras agregar nombres al listado: 5 unitarias y 3 IT correctos
  (CustomerManagementIT y OpenApiExportIT); contrato exportado y tipos regenerados.
- Tres pruebas nuevas de formulario correctas: origen enviado, errores de campo,
  referencia histórica y reintento sin pérdida de datos. El primer ensayo falló por
  localizadores exactos que incluían el asterisco del label; se corrigieron a roles
  accesibles. Build y lint correctos con las advertencias previas.
- E2E de 3.3 correcto contra API/PostgreSQL efímero: origen guardado tras recarga,
  contacto con herencia, reasignación por SALES_MANAGER y consulta como vendedor.
  Se corrigió la pérdida de la empresa preseleccionada al cargar opciones asíncronas.
  Seis rutas de clientes a 390 px, ambos temas, foco, noindex, sin scroll horizontal
  ni errores de consola. Capturas en `frontend/test-results/` (artefactos locales).
- Regresión frontend posterior: 26 tests correctos en 9 archivos y build correcto.

## Retomada: alcance de clientes — tarea 3.4

La interfaz oculta Editar para un vendedor que sólo consulta el cliente por una
oportunidad propia. Una URL directa al formulario muestra una ficha de consulta,
sin controles de escritura; ADMIN/SALES_MANAGER conservan edición del tenant.
Se preserva SEO privado durante carga, errores y acceso de consulta.

- `SellerScopeIT`: 2 IT correctos, más las 5 unitarias existentes. Edición de empresa
  y contacto asignados directamente, rechazo de reasignación del vendedor (403),
  consulta de relacionados y rechazo de su edición (404). Los tres roles omiten
  clientes de otro tenant en listados y reciben 404 por lectura/escritura directa;
  las filas ajenas permanecen intactas.
- Frontend: 26 tests y build correctos. Playwright: 2 E2E correctos contra API y
  PostgreSQL efímero; incluye la regresión de 3.3 y detalle/URL de edición de empresa
  y contacto directos/compartidos en los tres roles. Capturas revisadas en claro y oscuro.

## Retomada: baja lógica — tarea 3.5

`CustomerManagementIT`: 3 IT y las 5 unitarias existentes correctos. La nueva prueba
cambia empresa/contacto relacionados a `INACTIVO` y luego `NO_CONTACTAR` por HTTP;
conserva exactamente las filas de oportunidades, actividades e historial de etapas,
incluidos IDs, asociaciones y autoría. Comprueba consulta por administrador y vendedor
autorizado, y 404 al historial del cliente desde otro tenant. No requiere cambiar el
comportamiento de baja existente. El primer ensayo de la prueba asumía IDs planos en
el detalle de oportunidad; se corrigió para comprobar sus objetos anidados del contrato.

## Retomada: cierre de clientes — tarea 3.6

Regresión propia del corte: 5 unitarias y 22 IT correctos, sin fallos ni omisiones
(`CompanyControllerIT`, `ContactControllerIT`, `CustomerManagementIT`, `SellerScopeIT`,
`OpenApiExportIT`). OpenAPI exportado desde runtime con las respuestas y validaciones
de origen documentadas; tipos frontend regenerados desde ese archivo.

Frontend: 26 tests, lint y build correctos; persisten sólo las advertencias anteriores
de React Compiler en oportunidades y bundle >500 kB (570,72 kB en este build).
Playwright completo: 7 E2E correctos en 1 min 6 s contra API/PostgreSQL efímero.
Incluye los cuatro casos anteriores de usuarios/SEO, los dos flujos de clientes y
los ocho requests de Companies/Contacts de Postman ejecutados por HTTP con sustitución
de variables. Verifica nombres y origen en ambos detalles. La colección prepara el
origen activo antes de clientes, conserva ejemplos CUIT válidos y sus ocho cuerpos
son JSON válido tras sustituir variables. No se afirma haber importado/ejecutado toda
la colección en Postman ni cubierto sus flujos futuros (11.6 sigue pendiente).

README backend/frontend, requisitos, diseño y contexto reflejan el corte. Enlaces
locales de documentación comprobados sin rutas rotas; `openspec validate --strict`
y `git diff --check` correctos. `git status --short` revisado y cambios previos
preservados. Sin commits, pushes, deploy ni escrituras remotas.

Progreso al cerrar clientes: 15/72.

## Oferta comercial: backend de administración — tarea 4.1

POST/PUT de salones y servicios implementados con autorización ADMIN en service,
DTOs validados, tenant/actor de sesión, Location y baja lógica. Se reutiliza el
esquema vigente sin modificar migraciones. Capacidad positiva, importes opcionales
no negativos con precisión NUMERIC(15,2), estado/actividad obligatorios y equipamiento
no vacío. Las lecturas comerciales se mantienen para los tres roles.

5 unitarias y 4 IT correctos (`OfferingAdministrationIT`, `OpenApiExportIT`). Cubren
alta/edición/inactividad, relaciones comerciales y auditoría conservadas, 403 de
ambos roles comerciales y 404/omisión cross-tenant en lectura/escritura/listados.
El ensayo inicial detectó que un enum desconocido se traducía a 500: se agregó el
manejador específico de cuerpo ilegible, con 400 y mensaje genérico sin valores
internos. Las pruebas de estado/booleano inválido ahora pasan. Contrato exportado
y tipos regenerados. No hay todavía pantallas de administración de oferta.

Progreso: 16/72. Siguiente tarea: 4.2, paginación y consumidores de oferta.

## Oferta paginada — tarea 4.2

GET de salones y servicios devuelve PageResponse, búsqueda q, estado/actividad,
tamaño 20/máximo 100, sort permitido y desempate por ID. Se adaptaron las pruebas,
Postman, contrato, tipos, listado y selectores de formulario/filtros. Selecciones
por ID conservadas al buscar o cambiar de página; sin descargar todas las páginas.

Primera regresión: 5 unitarias y 18 IT, con un fallo por parámetro enum inválido
convertido a 500. Corregido con manejador de parámetros: segunda ejecución,
5 unitarias y 5 IT correctos. Las otras 13 IT habían pasado antes de esa corrección.
Frontend: 26 tests, lint y build correctos; desaparece la advertencia de watch
del formulario al usar useWatch. Persiste bundle >500 kB.
Un E2E nuevo pasó con 26 salones/servicios: página siguiente, búsqueda remota,
selección estable, filtros en URL/recarga y 390 px en claro/oscuro sin errores de
consola ni scroll horizontal. Captura clara inspeccionada. Progreso: 17/72.

## Oferta histórica, capacidad y administración UI — tareas 4.3–4.6

Edición de oportunidad conserva salón/servicios anteriores no disponibles, pero
rechaza nuevas asociaciones inactivas. Omitir serviceIds conserva; [] quita.
El frontend identifica oferta histórica y resuelve servicios por ID en detalle.
VenueInfo es contrato de service y OpportunityVenueResponse es DTO HTTP propio.
VenueReservationPort, implementado en opportunities sin repositorios ajenos,
rechaza con 409 reducir capacidad por debajo de una reserva ganada. Abiertas se
revalidan al editar. Ganar/revalidar disponibilidad y concurrencia de confirmación
siguen pendientes del corte de cierres.

ADMIN tiene alta, detalle, edición y baja por estado de salones/servicios. Lectura
comercial habilitada a los tres roles; guardas y acciones respetan permisos.
Listados, búsqueda, estados y paginación usan API y URL; errores de campo conservan
datos ingresados. UI en español, CSS Modules/tokens y Seo noindex.

Validación propia: 5 unitarias y 20 IT correctas (oferta, oportunidades, tablero,
SellerScope y export OpenAPI). El primer intento no compiló por una aserción de
test con comodín genérico; corregida antes de ejecutar la suite satisfactoria.
Después de documentar 409 en Swagger se reexportó con 5 unitarias y 1 IT correctas.
Contrato runtime copiado y tipos regenerados. Frontend: 28 pruebas, lint y build
correctos; persiste bundle >500 kB (592,86 kB antes del último build normal).

Tres E2E de oferta correctos contra PostgreSQL efímero: 11 requests de Postman,
26 salones/servicios con búsqueda fuera de página 0 y ciclo UI de administración,
baja y edición histórica de oportunidad. Ocho rutas de oferta recorridas a 390 px,
ambos temas, teclado/noindex, sin scroll horizontal ni errores de consola.
SELLER/SALES_MANAGER consultan detalle pero no tienen Editar ni acceso a formularios.
Las corridas iniciales detectaron expectativas de test incorrectas (asterisco de
campo obligatorio y texto En mantenimiento); se corrigieron. Una corrida quedó
invalidada porque se cambió el build durante preview; no se cuenta como evidencia.

Postman incluye crear/consultar/editar/desactivar oferta con IDs capturados y sin
alterar las variables de selección comercial; los requests se ejecutaron vía
Playwright, no dentro de la aplicación Postman. Documentación backend/frontend,
requisitos/diseño, arquitectura y tareas backend actualizados. §5 no se inició.

## Cierre seguro solicitado por el usuario

§4 cerrado y pausa solicitada; progreso **21/72**, siguiente **5.1**. No se inició
implementación de catálogos. Regresión final: **10 E2E correctos** (usuarios,
clientes y oferta); build normal restaurado y correcto, bundle 592,86 kB.
41 enlaces locales de 13 Markdown afectados comprobados sin rutas rotas;
OpenSpec strict válido. Puertos de prueba 18080/14173 sin listeners al finalizar.
Punto de retomada actualizado para continuar en otro chat. Sin commits, pushes,
deploy ni escrituras remotas; cambios anteriores preservados.

## Límites restantes vigentes

Las suites existentes no cubren los flujos aún pendientes de este cambio. Estos
resultados no implican aprobación de la entrega completa ni verificación remota.

## Continuación desde el punto de retomada — catálogos §5

Se implementó el ABM ADMIN de orígenes, motivos de pérdida, tipos de actividad y
tipos de evento. Detalles y lecturas comerciales por tenant; paginación de 20/máximo
100, búsqueda literal por nombre, filtro de actividad, sort permitido y desempate
por ID. Nombre recortado, no vacío, único por catálogo/tenant, con restricción DB
como defensa concurrente. Baja/activación mediante PUT; sin DELETE ni cambios a V1/V2.

Los nuevos contratos entre módulos usan CatalogEntryInfo. Clientes conservan la
validación de origen; oportunidades incorporan origen y permiten conservar tipo
inactivo previo. ActivityResponse conserva su JSON con un DTO propio para tipo.
assertSelectable de motivo/tipo de actividad se probó como contrato de service:
los endpoints de cierre y registro siguen pendientes en §8/§9.

UI ADMIN bajo /app/configuracion, cuatro catálogos con listado/alta/detalle/edición.
SelectorCatalogo busca/pagina bajo demanda y mantiene elegidos por detalle autorizado.
Los detalles comerciales consultan nombres por ID, incluidos inactivos y registros
fuera de página 0. Se adaptaron formularios/filtros existentes, mocks y SellerScopeIT.

Validación propia: 5 unitarias y 26 IT correctas en el corte
(CatalogAdministrationIT, CustomerManagementIT, SellerScopeIT, CommercialReadIT,
OpenApiExportIT). Incluye semilla mínima, tres roles, dos tenants por listado/ID y
escritura, unicidad, validación/paginación, conservación y rechazo de referencias.
Primer intento: 4 fallos de test por asumir que active=no es inválido; Spring lo
acepta como false. Se usa DESCONOCIDO como negativo y la repetición pasó.

Frontend: 30 pruebas, build y lint correctos. Tres E2E nuevos correctos contra
API/PostgreSQL efímeros: cuatro ABM; 17 rutas administrativas a 390/1440 px en ambos
temas, teclado/noindex y acceso denegado de los otros roles; 26 entradas de origen y
tipo de evento, selección/guardado histórico y nueve rutas comerciales a 390 px;
16 requests mantenidos de Postman ejecutados desde Playwright y rutas runtime/export.
Sin errores de consola ni scroll global. Captura móvil revisada visualmente.
La primera corrida E2E recargó antes del debounce de búsqueda; se corrigió para
esperar q en URL antes de recargar y se repitió satisfactoriamente. El primer comando
con npm perdió --grep en PowerShell y no encontró tests; no se cuenta como validación.

Contrato exportado de runtime, tipos regenerados y documentación backend/frontend
actualizada. El build normal está restaurado; persiste advertencia de bundle >500 kB.
La regresión completa de ese corte pasó con `clean verify`: 5 unitarias y 75 IT
correctas. Esa ejecución es anterior a §6. Sin bases remotas ni Git escrito.

## Continuación — múltiples embudos y etapas (§6)

V3 crea un embudo corporativo por tenant, conserva IDs/relaciones/historial y agrega
pipeline fijo en etapas/oportunidades, FKs compuestas y orden diferible por embudo.
No altera V1 ni la exclusión GiST. Tenants sin etapas reciben sólo DP-06; una
configuración parcial incompleta falla con diagnóstico y rollback, sin inventar
etapas. PipelineMigrationIT prueba instalación vacía, V1→última con varios tenants,
FKs, integridad y reejecución sin cambios.

ADMIN configura nombre/actividad/predeterminado y etapas/orden. Un puerto consulta
uso comercial para conservar kind; predeterminado único y clases activas mínimas
se protegen en transacciones. Se bloquea tenant→embudo→etapa; el uso comercial
mantiene bloqueos compartidos durante la asociación. Plantillas se copian con IDs
independientes y nuevos procesos nacen inactivos. OpportunityResponse/detail incluye
pipelineId fijo y tablero identidad/kind/active, con etapas cerradas e inactivas.

PipelineAdministrationIT: seis pruebas por HTTP real de tres roles/dos tenants,
activación incompleta, baja/predeterminado, plantillas, uso sólo histórico, orden
completo y dos configuraciones simultáneas, alta incompatible/transferencia y tablero
con etapa renombrada/inactiva, cerradas con oportunidades y alcance del vendedor.
Primera corrida detectó tres errores de test: URL doblemente escapada para búsqueda,
verbo incorrecto PATCH en /stage y asumir orden de evaluación de FKs. Corregidos;
repetición de nueve IT migración/configuración pasó. Se agregó caso de columna
cerrada con oportunidades y pasó con exportación (siete IT).

Regresión completa propia posterior de backend: clean verify correcto, **5 unitarias
y 84 IT**, cero fallos/errores/omitidos. Frontend: 30 pruebas, lint y build correctos.
El E2E inicial pasó contra API/PostgreSQL efímeros: dos embudos, copias independientes,
predeterminado, renombrado/orden, selección URL, consulta y edición histórica,
ocho rutas a 390/1440 px en ambos temas, teclado/noindex y roles. La advertencia
previa de setState en efecto se eliminó; se adaptó un mock de listado de embudos.

Se mantiene carpeta Postman de 14 solicitudes con captura de IDs y restitución del
predeterminado. Runtime/export y tipos se regeneraron; ADR, arquitectura, specs y
READMEs reflejan el modelo efectivo. La regresión E2E completa pasó: **15 pruebas**, incluidas 14 solicitudes Postman de
embudos. Primera corrida completa: 13 correctas y dos fallos por datos compartidos
de los tests (origen fuera de página 0 y tipo de evento desactivado por otro caso).
Se buscó el origen y se creó tipo propio activo; repetición de 15 correcta. Captura
móvil revisada; build normal restaurado. Persiste aviso de bundle >500 kB.
No se usaron bases remotas ni escrituras Git.

## Continuación — selección de clientes y alta contextual (§7)

SelectorCliente reutiliza tipos/consultas generados, búsqueda y páginas de 20 bajo
demanda, detalle por ID y filtro de contactos por empresa. Se integra en formularios
y filtros comerciales. Cambiar empresa limpia contacto; un contador de selección
impide que una consulta tardía restaure una relación anterior. El enlace existente
desde contacto deriva empresa/responsable por detalle autorizado; se agregó el de
empresa. Alta breve con obligatorios reales, primera etapa OPEN visible del embudo,
opcionales expandibles conservados y cancelación sin escritura. No inventa horarios.

El formulario presenta errores de campo y resumen accesible, estado pendiente y
servicios con etiquetas/foco/separación. Se retiró la explicación UTC de la interfaz;
las fechas mantienen interpretación de Argentina. Más de 50 advierte; 81 supera el
salón de 80 y se bloquea; 60 se guarda correctamente.

Se reprodujo un defecto existente: ContactSpecifications.contains hacía una unión
implícita interna con company, excluyendo individuales aun si coincidía su nombre.
OpportunityCreationIT mostró totalElements=0 cuando se esperaba 1. Se corrigió a
LEFT JOIN; la prueba y la regresión comercial pasaron. El corte ejecutó **5 unitarias
y 11 IT** (OpportunityCreationIT, CommercialReadIT, SellerScopeIT, OpenApiExportIT).
Cubre herencia contacto/empresa, individual, SELLER propio, contradicción, tres roles
/otro tenant, ausencia de parciales, fechas reales y capacidad/round-trip temporal.

Frontend: **32 pruebas**, lint y build correctos. Dos pruebas nuevas verifican página
posterior/selección por ID y respuesta tardía sin restaurar empresa. El E2E de alta
breve pasó con API/PostgreSQL: 26 empresas y 26 contactos, clientes corporativo e
individual por ID, herencia, capacidad 60/81, fechas, recarga/edición, cancelación,
expandir/cerrar sin perder opcionales y selección filtrada en página posterior.

Primer intento E2E usó un build anterior porque un error de tipado en el test impidió
compilar; se corrigió y se agregó corte del comando ante fallo del build. Luego se
ajustó búsqueda por etiqueta para no exigir igualdad con su texto decorativo. Otra corrida esperó el contacto
en página 0, pero la búsqueda también coincidía con el nombre de empresa y devolvía
26: se recorrió la siguiente página y pasó. No se redujeron los criterios de selección.
La repetición integrada pasó: **16 E2E**, incluida búsqueda individual para
verificar también la corrección de backend. Contrato HTTP sin nuevos campos en
este corte; exportación/runtime y tipos previos permanecen compatibles.

La regresión de 16 detectó scroll horizontal en detalle de empresa al agregar la
tercera acción, y un fixture de embudos que suponía que toda oportunidad tenía
empresa. Se habilitó distribución en varias filas en CabeceraPagina y ancho mínimo
0 en los selectores; el fixture admite cliente individual. Se repite la regresión
sin quitar ninguna comprobación de ancho ni recorrido.

La última repetición de las 16 pruebas pasó. Se acotó al contenido principal una
búsqueda de autor que también coincidía con el usuario en la cabecera. Captura
390 px revisada; build normal restaurado correctamente. Se conserva el aviso de
bundle >500 kB. No se atribuye una suite backend completa posterior a §7: en este
corte se ejecutaron los 11 IT indicados; la última completa sigue siendo la de §6.

## Continuación — ciclo comercial y reservas (§8)

V4 agrega snapshots append-only para eventos nuevos. Legacy conserva todos sus
campos con eventType LEGACY/snapshotComplete false, sin inventar cierres. La prueba
V1→última compara campos previos, cierres actuales, GiST y reejecución. PUT/stage y
acciones requieren versión leída; respuesta incluye versión tras flush.

OpportunityLifecycleService implementa assign/win/lose/reopen, roles/alcance, fecha
servidor, cierre/historial atómicos y liberación al reabrir. Disponibilidad consultable;
confirmación revalida capacidad/estado bajo bloqueo compartido de salón. Actualización
de oferta usa exclusivo, manteniendo confirmaciones compatibles entre sí. Exclusión
GiST por tenant/salón/rango, independiente del embudo. Advice no registra SQL sensible.

Pruebas propias: los siete IT de migraciones de CUIT/embudos/snapshots pasaron.
La primera corrida del ciclo tenía errores de test: estado NO_DISPONIBLE inexistente
y stubbing sobre proxy transaccional MANDATORY. Se corrigieron a MANTENIMIENTO y spy
del target; pasaron ocho IT. La suite completa posterior detectó un desenlace real
adicional de la carrera: CannotAcquireLockException devolvía 500. Se agregó traducción
a 409 CONCURRENT_WRITE_CONFLICT; SQLSTATE 23P01 del constraint sigue devolviendo
VENUE_ALREADY_BOOKED. No se eliminó barrera ni se serializaron las confirmaciones.
Repetición pertinente: **5 unitarias y 11 IT del ciclo**, cero fallos; incluye tres
carreras sobre oportunidades distintas y una sobre igual versión, tardía/generic edit,
rollback por trigger, contiguos/otro salón/tenant/embudo, cierre perdido sin reserva,
permisos, capacidad actual y conservación de valor/motivo tras reapertura/renombre.

Frontend: **32 pruebas**, lint sin advertencias y build correctos. Dos E2E del ciclo
pasaron contra API/PostgreSQL, con cierre ganado→reapertura→pérdida→recarga, permisos,
versiones de edición/acciones y formularios conservados ante 409. Incluyen 390/1440,
ambos temas, teclado/noindex y 16 solicitudes Postman con captura real de IDs/versiones.
Captura móvil oscura revisada. Primer E2E reveló redondeo de centavos en presentación;
se corrigió con formato exacto para cierres. El runner Postman asumía dos OPEN en el
fixture de una sola etapa abierta: ahora crea su propio embudo de plantilla breve,
independiente, y recorre una transición real. No se redujeron los checks de historia.

La repetición completa clean verify y regresión integrada del corte se registran
al concluir. Aún no se atribuyen esos resultados ni se marca §8.10 por este registro.

Cierre de §8: **clean verify correcto, 5 unitarias + 99 IT**, cero fallos/errores/omitidos.
Se compilaron luego los mensajes de validación en español y se preparó classpath E2E.
Regresión completa: **18 E2E correctos**, incluidas todas las rutas de cortes previos,
Postman, roles/temas/ancho/SEO y nuevos cierres/409. Export runtime de OpenApiExportIT
copiado, tipos regenerados y build normal restaurado. Continúa aviso bundle >500 kB.
23 enlaces locales comprobados; openspec strict, diff --check y status de lectura.
Sin acceso ni escritura a bases remotas, sin escrituras Git.
