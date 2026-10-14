# Frontend — zTech CRM

Frontend React del CRM de salones de eventos corporativos. La fundación de integración
consume el backend para autenticación, clientes, catálogos, oportunidades, embudo,
actividades e historial. Los datos comerciales proceden de la API; la caché en
memoria se limpia al cambiar de sesión.

Plan inicial y mapa de rutas de referencia: [`docs/planificacion/plan-frontend-maqueta.md`](../docs/planificacion/plan-frontend-maqueta.md).
El alcance integrado actual se registra en las secciones siguientes y en el cambio
[`completar-crm-entrega-final`](../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/tasks.md).
Decisiones abiertas: [`docs/decisiones/decisiones-frontend.md`](../docs/decisiones/decisiones-frontend.md).

## Ejecutar

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # verificación de tipos y build de producción
npm run preview  # sirve el build
npm run lint     # oxlint
npm test         # Vitest + Testing Library
npm run generar:api # regenera tipos desde backend/docs/openapi.yaml
npm run test:e2e # Playwright contra API y PostgreSQL locales de prueba (preparación abajo)
```

## Variables de entorno

| Variable | Para qué | Si falta |
|---|---|---|
| `VITE_SITE_URL` | URL absoluta del sitio, sin barra final. Alimenta el `canonical`, Open Graph, el `sitemap.xml` y el `robots.txt`. | Se usa `VERCEL_PROJECT_PRODUCTION_URL` si el build corre en Vercel; si no, `http://localhost:5173`. |
| `VITE_API_URL` | Base versionada de la API, por ejemplo `https://api.example.com/api/v1`. | En desarrollo usa `http://localhost:8080/api/v1`; en Vercel es obligatoria. |

`robots.txt` y `sitemap.xml` **no están versionados**: los genera `vite.config.ts` durante el build, para que el dominio viva en un solo lugar. Al agregar una ruta pública hay que sumarla a `RUTAS_PUBLICAS` en ese archivo.

Copiá `.env.example` a `.env.local` si querés fijar la URL en desarrollo.

## Despliegue

El hosting elegido es **Vercel** con integración de GitHub (ADR-005). La configuración vive en `vercel.json`: reescrituras de SPA (sin ellas, entrar directo a `/nosotros` da 404), caché inmutable para `/assets/*` y cabeceras de seguridad. La versión de entrega final se valida localmente; el usuario hará su publicación y demostración manualmente.

En Vercel, **Root Directory tiene que ser `frontend`**, porque el repositorio es un monorepo.

### Previsualizaciones y rama de producción

Vercel puede generar previsualizaciones de las ramas configuradas. La URL efectiva
está en **Deployments**; no se deduce su vigencia a partir de ejemplos históricos.

```text
https://<proyecto>-git-feature-frontend-<scope>.vercel.app
```

La rama de producción se elige en **Settings → Environments → Production → Branch
Tracking** (o **Settings → Git → Production Branch**, según la cuenta). Debe contener
`frontend/` y usar ese Root Directory. Revisar también las variables de API/sitio
antes de publicar versiones compatibles de frontend y backend. Esta configuración
no implica que el último cambio ya esté desplegado.

## Estructura

```text
src/
├── app/
│   ├── layouts/     LayoutPublico (sitio) y LayoutApp (CRM)
│   └── router/      Mapa de rutas y scroll al navegar
├── features/
│   ├── inicio/          Landing de producto
│   ├── institucional/   Nosotros, contacto, términos, privacidad, 404
│   ├── acceso/          Inicio de sesión
│   ├── usuarios/        Administración de usuarios, sólo ADMIN
│   ├── panel/           Pantalla principal del CRM
│   ├── empresas/        Listado, detalle, alta y edición
│   ├── contactos/       Listado, detalle, alta y edición
│   ├── salones/         Consulta paginada y ABM ADMIN
│   ├── servicios/       Consulta paginada y ABM ADMIN
│   ├── oportunidades/   Listado, detalle, alta, edición y selector de etapa
│   ├── embudo/          Tablero por etapa
│   └── actividades/     Línea de tiempo del historial comercial
└── shared/
    ├── components/  Componentes de interfaz reutilizables
    ├── data/        Formateo exclusivamente presentacional
    ├── seo/         Metadatos por ruta y datos estructurados
    └── styles/      Tokens, base y patrones de pantalla
```

## Convenciones

- **Idioma:** nombres de archivos, componentes, variables y textos en español, con la terminología del dominio de la ERS (empresa, contacto, oportunidad, etapa, embudo, salón, actividad).
- **Estilos:** CSS Modules y tokens en custom properties. Ningún valor de color, espaciado o tipografía se escribe a mano: sale de `shared/styles/tokens.css`.
- **Componentes:** un `.module.css` por componente, con el mismo nombre. Los estilos compartidos entre pantallas van en `shared/styles/pantalla.module.css`.
- **Iconos:** se agregan a `shared/components/Icono.tsx`, no se importan de librerías.
- **SEO:** toda página pública renderiza `<Seo />` con `titulo`, `descripcion` y `canonica`. Las pantallas de `/app` lo hacen con `noIndexar`.

Las confirmaciones de operaciones usan `shared/components/AvisoExito.tsx`: ícono,
fondo y borde semánticos para ambos temas, con `role="status"` para lectores de
pantalla. Se muestran junto al contenido y permanecen visibles, sin mover el foco
ni depender de un temporizador. Reutilizarlo en lugar de mensajes de éxito en texto
suelto o etiquetas de estado.

## Integración con la API

### Selectores con búsqueda

Empresas, contactos, catálogos, embudos, salones y servicios comparten
`shared/components/SelectorBuscable.tsx`: un único campo para buscar y elegir.
El desplegable aparece al enfocar el campo; las flechas recorren opciones,
Enter selecciona y Escape o salir del campo cancela la búsqueda conservando el
valor anterior. Los servicios admiten varias selecciones y etiquetas para quitarlas.

`useOpcionesSelector` consulta al abrir, espera 200 ms al escribir y acumula páginas
de 20 con «Ver más resultados» dentro del desplegable. No muestra números de página
ni controles de paginación en el formulario. Cada búsqueda/contexto tiene su caché
y cancelación; los valores elegidos se resuelven por ID, incluidos los históricos
inactivos. Los estados de carga, vacío, error y reintento pertenecen al selector.
Los filtros simples de estado, rol y etapas conservan sus controles nativos.

Los formularios de clientes no precargan listas de empresas/orígenes ni bloquean
el guardado por una lista opcional que no se abrió. El servidor sigue validando
las referencias elegidas. Las pruebas del componente cubren teclado, cancelación,
multiselección y foco; las pruebas comerciales conservan páginas posteriores,
respuestas tardías, referencias históricas y guardado por HTTP.

`src/shared/api/` contiene el cliente `fetch`, el contrato generado y TanStack Query;
`src/shared/sesion/` resuelve token, usuario, expiración y persistencia. Login y cambio
de clave ya usan estas capas.

Los módulos `api*.ts` derivan sus DTO del esquema generado desde
`backend/docs/openapi.yaml`. TanStack Query gestiona caché e invalidación; los formularios
comerciales usan React Hook Form y Zod, y los filtros/paginación se reflejan en la URL.

## Oferta comercial

Salones: `/app/salones`, `/app/salones/nuevo`, `/app/salones/:id` y
`/app/salones/:id/editar`. Servicios: las mismas rutas bajo `/app/servicios`.
Altas/ediciones protegidas por GuardaRol ADMIN y acciones visibles sólo para ese
rol. El menú Servicios adicionales es administrativo; lecturas por URL siguen
habilitadas para los tres roles. Salones conserva su acceso comercial.

Listados con búsqueda/estado y paginación en URL. Formularios de oferta con
validación y errores de campo del servidor, baja lógica por estado y caché
invalidada al guardar. Los selectores de salón/servicios buscan en servidor,
paginan de a 20 y conservan seleccionados por detalle, incluso inactivos previos.
El detalle de oportunidad muestra sus servicios y estado histórico del salón.
Una reducción que invalida una reserva confirmada muestra el conflicto de la API.

## Administración de usuarios

`ADMIN` dispone de `/app/usuarios`, `/app/usuarios/nuevo`, `/app/usuarios/:id` y
`/app/usuarios/:id/editar`: búsqueda por nombre/correo, filtros de rol y estado,
paginación en URL, alta con clave temporal, edición, baja lógica y restablecimiento.
Los tres roles mantienen la consulta comercial de oferta y catálogos. La configuración
de embudos y etapas está disponible para ADMIN en `/app/configuracion/embudos`.

`GuardaRol` rechaza URLs administrativas para `SELLER` y `SALES_MANAGER`; el menú
Usuarios aparece sólo para `ADMIN`. Sesión y cambio inicial de clave se validan antes.
El backend conserva la autorización efectiva, protección de último admin y de baja
propia, revocación y aislamiento. Si cambia el propio rol o se restablece la propia
contraseña, la interfaz cierra la sesión. Las pantallas privadas renderizan SEO noindex
también durante carga/error.

## Pruebas integradas locales

Requieren JDK 25, Docker Desktop y dependencias frontend. No usan `.env`, Supabase,
credenciales reales ni un endpoint de alta de organizaciones. El launcher de test
crea PostgreSQL efímero con Flyway y fixtures y habilita la política de cambio de clave.

Desde `backend/`:

```powershell
./mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/baseline-classpath.txt' '-Dmdep.includeScope=test'
npm install --prefix target/newman --no-save newman@6.2.3
```

Desde `frontend/`, con puertos 18080 y 14173 libres:

```powershell
npx playwright install chromium
$env:VITE_API_URL='http://localhost:18080/api/v1'
npm run build
npm run test:e2e
```

Playwright levanta y cierra la API/preview de prueba. Cubre administración→alta→login
temporal→cambio obligatorio, edición/baja/reset/revocación, roles, rutas administrativas
a 390 px en ambos temas y metadatos públicos. `entrega-integrada.spec.ts` recorre
login por interfaz → empresa/contacto → alta breve → tablero/cambio → actividad →
cierre → recarga y verifica la persistencia por HTTP sobre PostgreSQL real.
`rutas-entrega.spec.ts` revisa las rutas con los tres roles y ambos tamaños/temas.
`postman-entrega.spec.ts` importa y ejecuta la colección completa con Newman en el
segundo tenant; requiere la instalación aislada anterior dentro de `backend/target`.
Vitest se limita a `src/**/*.test.{ts,tsx}` y se ejecuta
con `npm test`; los E2E se ejecutan por separado.

La [línea base de carga](../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/evidencias/linea-base-carga.md)
documenta el comando independiente de medición y sus límites. Un build normal vuelve
a usar la configuración habitual; la variable del ejemplo sólo debe permanecer en la
terminal utilizada para pruebas.

## Estado de los flujos comerciales

Empresas/contactos incluyen origen y responsables por nombre. El origen se valida
contra la organización y su actividad al guardar; una referencia histórica inactiva
puede conservarse. El detalle identifica responsables inactivos sin ampliar los
usuarios asignables. Para SELLER, Editar y la URL del formulario requieren asignación
directa; los clientes relacionados sólo por oportunidad propia siguen siendo de consulta.
La baja por estado conserva oportunidades, actividades y autoría.

El E2E de clientes cubre origen, herencia, reasignación por supervisión, consulta y
edición por alcance para los tres roles y seis rutas a 390 px en ambos temas.

Alta contextual, cierres y registro de actividades implementados en el cambio
OpenSpec activo. El corte §10 incorpora carga por ruta y consultas acotadas;
la regresión de entrega sigue registrada por separado en §11.

## Carga y caché

Las pantallas usan `lazy` por ruta y `Suspense`; las guardas se aplican antes de
montar la pantalla. El panel consulta cinco oportunidades por fecha de alta y ocho
actividades, con carga, vacío, error y reintento independientes. Conserva accesos
a nueva oportunidad y embudo; no descarga el tablero ni calcula indicadores.
Los selectores siguen solicitando páginas de 20 bajo demanda.

Las claves distinguen embudo, filtros, página y tamaño; las consultas vigentes se
reutilizan durante 30 segundos (usuarios asignables: cinco minutos). Las lecturas
consumen `AbortSignal` y descartan búsquedas anteriores; IDs inválidos no consultan.
Los errores HTTP 4xx y cancelaciones no se reintentan automáticamente. Altas,
ediciones y acciones invalidan listas, detalles y relaciones afectadas. Ingresar
o cerrar sesión vacía la caché; una respuesta tardía de otra cuenta se descarta
y no puede cerrar la sesión vigente mediante un 401 antiguo.

`src/shared/api/carga.test.tsx` verifica cancelación, claves, invalidaciones y
cambio de cuenta. `e2e/carga.spec.ts` comprueba solicitudes reales del panel/alta,
división de pantallas, navegación repetida, IDs inválidos y recuperación de error.

## Actividades e historial comercial

Los tres detalles ofrecen `Registrar actividad`: tipo activo mediante búsqueda
paginada, fecha/hora del hecho en Buenos Aires, descripción y resultado. El contexto
queda fijado al cliente u oportunidad abiertos; el servidor deriva asociaciones y
autoría. Se permiten hechos de oportunidades cerradas. SELLER sólo ve alta general
en clientes asignados directamente, además del alta sobre sus oportunidades.

El historial distingue fecha real y fecha de registro, conserva autores inactivos y
permite recorrer páginas de 20 hechos. Guardar invalida actividades, clientes y
oportunidades; vuelve a la primera página y devuelve el foco a la acción de alta.
Cancelar no escribe; un error conserva campos y permite corregir/reintentar.
No existen acciones de edición ni borrado. Empresas/contactos muestran también sus
oportunidades abiertas y cerradas con páginas; las empresas paginan sus contactos.
El historial de etapas conserva notas, autores y datos de cierres sucesivos y permite
reintentar su lectura. `Actividades.test.tsx` y `e2e/actividades.spec.ts` cubren el corte.

## Catálogos comerciales

`ADMIN` dispone de `/app/configuracion` y de `origenes`, `motivos-perdida`,
`tipos-actividad` y `tipos-evento` bajo esa ruta. Cada catálogo tiene listado,
`nuevo`, `:id` y `:id/editar`, guardados por rol, con búsqueda/estado/página en URL,
nombre y actividad. Desactivar conserva las referencias; no existe borrado físico.

`SelectorCatalogo` busca y pagina en servidor de a 20; consulta la selección por ID
para conservar valores posteriores a la página inicial o históricos inactivos.
Los detalles también resuelven nombres por ID. Se adaptaron formularios y filtros
de clientes, oportunidades y tablero; no se descargan todas las páginas.
`Catalogos.test.tsx` cubre paginación/selección/respuestas tardías y recuperación de
conflicto; `e2e/catalogos.spec.ts` recorre los cuatro ABM contra la API real,
permisos, referencias históricas y rutas a 390 px/escritorio.

## Embudos y etapas

`/app/configuracion/embudos`, `/nuevo` y `/:id` permiten listado paginado, búsqueda,
creación vacía o desde plantilla, activación/baja, elección del predeterminado y ABM
con orden de etapas. ADMIN configura; los otros roles conservan lectura comercial.
Las reglas de uso, clases mínimas y pertenencia se validan en servidor.

El tablero persiste `pipelineId` en URL y presenta el predeterminado si no hay selección.
Los filtros de oportunidad permiten todos o un embudo, incluidos inactivos. Cambiar
embudo limpia etapa; el alta muestra predeterminado y etapas activas OPEN propias.
La edición conserva el embudo fijo y sus relaciones históricas. Los nombres de etapas
no determinan si están abiertas o cerradas. El tablero incluye columnas cerradas e
inactivas; sólo ofrece destinos activos OPEN a oportunidades abiertas.

## Alta contextual de oportunidades

`Nueva oportunidad` desde empresa o contacto abre la misma ruta de alta con
`empresaId` o `contactoId`. Se consulta el detalle autorizado por ID: un contacto
corporativo deriva empresa y propone su responsable; uno individual no exige empresa.
El alta breve muestra título, cliente, embudo/primera etapa abierta, salón, tipo de
evento, inicio/fin y asistentes. No inventa fechas. Los datos opcionales se expanden
y conservan lo ingresado; cancelar vuelve al cliente sin crear registros.

`SelectorCliente` busca/pagina empresas/contactos de a 20, conserva seleccionados
por ID y se comparte con filtros y formularios. Contactos se filtran por empresa;
cambiar empresa limpia contacto y las respuestas tardías no restauran la relación
anterior. La búsqueda incluye contactos individuales. El backend vuelve a comprobar
pertenencia, herencia DP-02, alcance y capacidad. Más de 50 advierte; superar la
capacidad real bloquea. Las fechas se presentan en la zona de negocio.

Los servicios conservan etiquetas, foco y referencias históricas; el formulario
muestra resumen accesible y errores junto al campo. `SeleccionClientes.test.tsx`
prueba página posterior y respuesta tardía; `e2e/alta-oportunidades.spec.ts` recorre
altas corporativa/individual desde IDs posteriores, cancelación, guardado y edición.

El detalle de oportunidad permite reasignar y ganar/perder/reabrir según rol/estado.
Los formularios de acción capturan la versión al abrirse; edición conserva la versión
con que se cargó. Un 409 mantiene lo escrito, bloquea el reenvío y ofrece Recargar
datos. El cierre muestra fecha/valor o motivo y el historial conserva cierres sucesivos,
incluso tras renombrar el motivo. Disponibilidad previa orientativa; confirmar revalida.
