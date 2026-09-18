# Plan de desarrollo — Maqueta de frontend (Entrega 1, 24/09)

## Objetivo

Construir la **maqueta visual navegable** del frontend de zTech CRM: todas las pantallas del alcance de Entrega 1 más el sitio institucional público, con diseño, contenido y navegación definitivos, **sin backend, sin base de datos y sin lógica de negocio real**.

La maqueta congela el diseño de pantallas (parte pendiente de DP-11) y funciona como contrato visual para cuando se conecte la API.

## Fuera de alcance (deliberado)

| No se implementa | Por qué |
|---|---|
| Llamadas HTTP, TanStack Query, cliente de API | El backend todavía no existe |
| Autenticación, JWT, sesión, guardas de ruta | El login es una pantalla estética; "Ingresar" navega al panel |
| Persistencia de altas, ediciones y cambios de etapa | Los datos salen de un módulo de demostración en memoria |
| Validación con React Hook Form + Zod | Los formularios son visuales; se validan al integrar |
| Envío del formulario de contacto | No hay servicio de correo; el envío muestra estado visual |
| Roles y permisos efectivos | E1 no los exige; el selector de rol es decorativo |
| Pruebas automatizadas (Vitest, Playwright) | Se incorporan junto con la lógica, no con la maqueta |

## Stack de la maqueta

Respeta lo aprobado en `docs/arquitectura/01-arquitectura-general.md`.

| Área | Decisión | Nota |
|---|---|---|
| Base | React 19 + TypeScript + Vite | Del documento de arquitectura |
| Ruteo | React Router 7 (modo declarativo) | Del documento de arquitectura |
| Estilos | CSS Modules + tokens en CSS custom properties | Sin framework de utilidades; ver DF-01 |
| Iconografía | Set propio de SVG inline | Sin dependencia externa; ver DF-02 |
| SEO | Hook propio `useSeo` + assets estáticos | Sin `react-helmet`; ver DF-03 |
| Datos | Módulo `shared/datos/demo.ts` tipado | Reemplazable por la API sin tocar las vistas |
| Dependencias de runtime | `react`, `react-dom`, `react-router-dom` | Sin agregados |

## Mapa de rutas

### Sitio público (indexable, con SEO)

| Ruta | Pantalla | Objetivo SEO |
|---|---|---|
| `/` | Inicio (landing de producto) | Término principal: CRM para salones de eventos corporativos |
| `/nosotros` | Quiénes somos | Marca, equipo y enfoque del producto |
| `/contacto` | Contacto (formulario + preguntas frecuentes) | Consulta comercial, `FAQPage` en JSON-LD |
| `/terminos` | Términos del Servicio | Confianza y cumplimiento |
| `/privacidad` | Política de Privacidad | Confianza y Ley 25.326 |
| `*` | Error 404 | `noindex` |

### Aplicación (no indexable, `noindex`)

| Ruta | Pantalla | Caso de uso de E1 |
|---|---|---|
| `/ingresar` | Inicio de sesión | Iniciar sesión |
| `/app` | Panel principal | Pantalla principal |
| `/app/empresas` | Listado de empresas | Listar y buscar |
| `/app/empresas/nueva`, `/app/empresas/:id/editar` | Alta y edición de empresa | Crear y modificar |
| `/app/empresas/:id` | Detalle de empresa | Consultar detalle |
| `/app/contactos` | Listado de contactos | Listar y buscar |
| `/app/contactos/nuevo`, `/app/contactos/:id/editar` | Alta y edición de contacto | Crear y modificar |
| `/app/contactos/:id` | Detalle de contacto | Consultar detalle |
| `/app/salones` | Listado de salones | Producto o servicio precargado |
| `/app/oportunidades` | Listado de oportunidades | Listar y filtrar |
| `/app/oportunidades/nueva`, `/app/oportunidades/:id/editar` | Alta y edición | Crear y modificar |
| `/app/oportunidades/:id` | Detalle de oportunidad | Consultar detalle |
| `/app/embudo` | Tablero por etapa | Visualizar embudo y cambiar de etapa |

El recorrido de la demostración obligatoria queda navegable de punta a punta:
`/ingresar` → `/app/empresas/nueva` → `/app/contactos/nuevo` → `/app/oportunidades/nueva` → `/app/embudo` → `/app/oportunidades/:id`.

## Estrategia de SEO

Aplica **sólo al sitio público**; la aplicación se marca `noindex, nofollow`.

1. **HTML semántico**: un `<h1>` por página, jerarquía de encabezados correcta, `header`, `nav`, `main`, `section`, `article`, `footer`, `lang="es-AR"`.
2. **Metadatos por ruta**: `title` (50-60 caracteres), `meta description` (140-158), `canonical`, Open Graph y Twitter Card, gestionados por el hook `useSeo`.
3. **Datos estructurados JSON-LD**: `Organization` y `WebSite` en el layout público, `SoftwareApplication` en inicio, `FAQPage` en contacto, `BreadcrumbList` en las internas.
4. **Assets estáticos**: `robots.txt` con referencia al sitemap, `sitemap.xml` con las cinco URL públicas, `site.webmanifest`, favicons y `apple-touch-icon`.
5. **Imagen social**: 1200x630 generada desde el logo, referenciada en `og:image`.
6. **Accesibilidad como SEO**: contraste AA, foco visible, `alt` descriptivo, `aria-label` en controles sin texto, enlace de salto al contenido, objetivos táctiles de 44 px.
7. **Rendimiento**: sin framework de CSS, sin librería de iconos, fuentes con `preconnect` y `display=swap`, imágenes con `width`/`height` y `loading="lazy"` fuera del pliegue.
8. **URLs en español**, cortas y estables (`/nosotros`, `/contacto`, `/terminos`, `/privacidad`).

Límite conocido: al ser una SPA, los metadatos se aplican en tiempo de ejecución. Googlebot los procesa, pero otros rastreadores y las tarjetas sociales no ejecutan JavaScript. Se resuelve con prerenderizado antes de publicar (ver DF-03).

## Lenguaje visual

Derivado del logo (isotipo "Z" con auriculares y anteojos, degradado turquesa a violeta, wordmark índigo).

- **Paleta**: turquesa `#2E95A3`, azul `#4878A0`, violeta `#6C489C`, índigo `#301854`. Neutros con matiz violeta. El degradado se usa como acento puntual, nunca como fondo general.
- **Tipografía**: `Sora` para titulares (geometría afín al wordmark), `Inter` para interfaz y texto corrido, números tabulares en tablas y montos.
- **Forma**: radios contenidos (6 a 12 px), bordes de 1 px en lugar de sombras pesadas, densidad de datos real en las pantallas de la aplicación.
- **Tono**: producto B2B sobrio. Sin emojis, sin glassmorphism, sin tarjetas infladas.

## Secuencia de trabajo

| # | Paso |
|---|---|
| 1 | Lectura de consignas, ERS y documentos de arquitectura |
| 2 | Rama `feature/frontend` |
| 3 | Normalización de assets de marca y derivados (favicon, Open Graph) |
| 4 | Scaffold Vite + React 19 + TS + React Router 7 |
| 5 | Sistema de diseño: tokens, base, componentes compartidos |
| 6 | Infraestructura de SEO: `useSeo`, JSON-LD, `robots`, `sitemap`, manifest |
| 7 | Layout público: encabezado, pie, navegación |
| 8 | Páginas públicas: inicio, nosotros, contacto, términos, privacidad, 404 |
| 9 | Pantalla de inicio de sesión |
| 10 | Layout de aplicación: barra lateral, barra superior, migas |
| 11 | Datos de demostración tipados |
| 12 | Empresas: listado, detalle, formulario |
| 13 | Contactos: listado, detalle, formulario |
| 14 | Salones: listado |
| 15 | Oportunidades: listado, detalle, formulario |
| 16 | Embudo: tablero por etapa |
| 17 | Panel principal |
| 18 | Repaso responsive, accesibilidad y build de producción |
| 19 | Bitácora y registro de decisiones pendientes |

El avance se registra en `bitacora-frontend.md`; las decisiones abiertas, en `docs/decisiones/decisiones-frontend.md`.

## Criterio de terminado de la maqueta

- Las rutas del mapa navegan sin errores de consola.
- `npm run build` pasa sin errores de TypeScript.
- Cada página pública tiene `title`, `description`, `canonical` y datos estructurados propios.
- El sitio y la aplicación se usan a 360 px, 768 px y 1440 px.
- No hay texto de relleno ni pantallas vacías sin estado vacío diseñado.
- Toda decisión de diseño que condicione el backend queda registrada como `DF-xx`.

## Cómo se ejecuta

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # verificación de tipos y build de producción
npm run preview  # sirve el build
```
