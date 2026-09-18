# ADR-005 — Frontend en Vercel

- Estado: Aceptada
- Fecha: 12/09/2026
- Decisores: equipo zTech CRM

## Contexto

La arquitectura inicial proponía Cloudflare Pages, pero la maqueta ya se valida en
previews de Vercel y el repositorio cuenta con configuración de SPA y cabeceras para
esa plataforma. Mantener dos destinos agrega documentación y pruebas duplicadas sin
aportar valor al trabajo práctico.

## Decisión

Vercel será el hosting definitivo del frontend. El proyecto usa `frontend/` como Root
Directory, `VITE_API_URL` para la API desplegada en Render y `VITE_SITE_URL` para URL
canónica. Las ramas se revisan mediante previews y la promoción a producción se hace
manualmente después de la aceptación.

## Consecuencias

- `frontend/vercel.json` es la única configuración de hosting estático.
- Se elimina la configuración `_redirects` específica de Cloudflare Pages.
- CORS del backend admite sólo localhost, la preview aprobada y producción.
- La cuenta, conexión del repositorio y promoción de ambientes requieren intervención
  del usuario; ninguna credencial se versiona.
