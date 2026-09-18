# Bitácora — Maqueta de frontend

Registro breve de lo que se fue haciendo en la rama `feature/frontend`. Una línea por paso.

## 2026-09-12

**Preparación**

- Leídas las cuatro consignas en PDF, la ERS y los tres documentos de arquitectura; fijado el alcance de Entrega 1.
- Creada la rama `feature/frontend` a partir de `main`.
- Escrito `plan-frontend-maqueta.md` con alcance, no-alcance, mapa de rutas, estrategia de SEO y secuencia de trabajo.

**Marca**

- Los cinco archivos del logo se movieron de `docs/imagenes/` a `docs/marca/` con nombres descriptivos: `ztech-crm-logo-color-transparente.png`, `-color-fondo-claro.jpeg`, `-monocromo-oscuro.jpeg`, `-monocromo-claro-fondo-negro.jpeg`, `-escala-de-grises.jpeg`.
- Recortado el isotipo del PNG transparente a `ztech-crm-isotipo-transparente.png` y copiado al frontend como asset de la interfaz.
- Extraída la paleta del logo: turquesa `#2E95A3`, azul `#4878A0`, violeta `#6C489C`, índigo `#301854`.
- Generados favicon `.ico`, `apple-touch-icon` e iconos de 192 y 512 px (monograma "Z" sobre degradado de marca) y la imagen Open Graph de 1200x630.

**Base del proyecto**

- Scaffold `frontend/` con Vite 8, React 19 y TypeScript 6; agregado React Router 7. Sin más dependencias de runtime.
- Eliminados los archivos de ejemplo del scaffold y creada la estructura `app/`, `features/`, `shared/` del documento de arquitectura.

**Sistema de diseño**

- `shared/styles/tokens.css`: color, tipografía, espaciado, forma, sombras y medidas como custom properties.
- `shared/styles/base.css`: reset acotado, foco visible, `prefers-reduced-motion`, enlace de salto y utilidad para lectores de pantalla.
- Tipografías: `Sora` para titulares e `Inter` para interfaz, vía Google Fonts con `preconnect` y `display=swap`.
- Componentes compartidos: `Logo`, `Icono` (set propio de 40 SVG inline), `Boton`, `Etiqueta`, `Avatar`, `Tarjeta`, `Campos`, `Tabla`, `ListaDatos`, `EstadoVacio`, `CabeceraPagina`, `BarraFiltros`.

**SEO**

- `shared/seo/Seo.tsx`: hook propio que gestiona `title`, `description`, `canonical`, `robots`, Open Graph, Twitter Card y JSON-LD por ruta.
- `shared/seo/datosEstructurados.ts`: `Organization`, `WebSite`, `SoftwareApplication`, `BreadcrumbList` y `FAQPage`.
- Assets: `site.webmanifest`, favicons y `_redirects` para el fallback de SPA en Cloudflare Pages. `robots.txt` y `sitemap.xml` arrancaron estáticos y pasaron a generarse en el build (ver preparación del despliegue).

**Contenido**

- Textos legales y copy institucional redactados con agentes dedicados (ver `docs/decisiones/decisiones-frontend.md`, nota sobre el proceso).
- Términos del Servicio y Política de Privacidad quedan como Markdown en `features/institucional/documentos/` y se leen con un parser propio de 60 líneas; se editan sin tocar componentes.
- Datos de demostración: 5 usuarios, 12 empresas, 16 contactos, 6 salones, 7 etapas, 18 oportunidades, 20 actividades y 60 registros de historial, todos ficticios y coherentes entre sí.

**Sitio público**

- Layout público con encabezado adherido, menú móvil, pie de cuatro columnas y JSON-LD de organización.
- Inicio: portada con miniatura real del embudo, franja de datos del producto, problema, capacidades, cómo funciona y cierre.
- Nosotros, Contacto (formulario + preguntas frecuentes), Términos, Privacidad y error 404.

**Aplicación**

- Layout con barra lateral, barra superior con buscador y usuario, y aviso permanente de maqueta.
- Ingreso, Panel, Empresas (listado, detalle, formulario), Contactos (los tres), Salones, Oportunidades (los tres) y Embudo.
- Detalle de oportunidad con selector visual de etapa, historial de etapas, actividades y ocupación del salón.

**Verificación**

- `npm run build` pasa sin errores de TypeScript.
- Las 19 rutas se recorrieron con Playwright a 1440 px y 390 px: sin errores de consola y sin desborde horizontal de página.
- Auditados `title` (50-53 caracteres en páginas públicas), `description` (142-150), `canonical`, `robots` y JSON-LD por ruta.

**Correcciones durante la verificación**

- `fecha()` y sus variantes devolvían error ante una fecha nula; ahora muestran un guión. Rompía el embudo y el listado de oportunidades.
- El hook de SEO agregaba una segunda `meta description` en lugar de reutilizar la de `index.html`; ahora adopta la existente.
- Los textos de actividad generados traían identificadores internos (`e12`, `c4`); reemplazados por los nombres reales.
- Una oportunidad tenía `fechaEvento` nula y cero asistentes; corregida en el set de datos.
- El índice de los documentos legales recortaba los marcadores de dos cifras; ajustado el sangrado.
- Contadores en singular: "1 cambio registrado" en lugar de "1 cambios registrados".
- La columna de actividad del panel era mucho más alta que la otra; acotada con scroll propio.

**Preparación del despliegue**

- Agregado `frontend/vercel.json`: reescrituras de SPA, caché inmutable para `/assets/*` y cabeceras de seguridad. Sin las reescrituras, entrar directo a `/nosotros` devuelve 404.
- El dominio dejó de estar escrito a mano en tres archivos: ahora sale de `VITE_SITE_URL`, con respaldo automático en `VERCEL_PROJECT_PRODUCTION_URL` y en localhost.
- `robots.txt` y `sitemap.xml` dejaron de versionarse; los genera un plugin de `vite.config.ts` durante el build, a partir de esa misma URL.
- Registrada la desviación de hosting respecto de la arquitectura aprobada (`DF-09`) y actualizado `DF-04`.
- Rama `feature/frontend` publicada en GitHub para que Vercel la tome como previsualización. `main` queda intacto: hay seis personas trabajando sobre esa rama y un merge ahora sería disruptivo.
- Documentado en `frontend/README.md` el efecto colateral: como Vercel tiene `main` de producción y `main` no contiene `frontend/`, cada push a `main` dispara un build que falla. Se corta apuntando producción a la rama o, mejor para un monorepo, con un Ignored Build Step.

## Pendiente para la próxima sesión

- Revisión de accesibilidad con lector de pantalla real (hasta ahora sólo se validó la estructura).
- Decisiones `DF-01` a `DF-03` y `DF-05` a `DF-09` en `docs/decisiones/decisiones-frontend.md`.
- `DF-03` (prerenderizado) sigue siendo lo que más pesa: hasta resolverlo, un enlace compartido por WhatsApp o LinkedIn muestra el título genérico, no el de la página.
