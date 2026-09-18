# Frontend — zTech CRM

Frontend React del CRM de salones de eventos corporativos. La fundación de integración
consume el backend para autenticación, clientes, catálogos, oportunidades, embudo,
actividades e historial. No mantiene datos comerciales privados en memoria.

Alcance, no-alcance y mapa de rutas: [`docs/planificacion/plan-frontend-maqueta.md`](../docs/planificacion/plan-frontend-maqueta.md).
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
```

## Variables de entorno

| Variable | Para qué | Si falta |
|---|---|---|
| `VITE_SITE_URL` | URL absoluta del sitio, sin barra final. Alimenta el `canonical`, Open Graph, el `sitemap.xml` y el `robots.txt`. | Se usa `VERCEL_PROJECT_PRODUCTION_URL` si el build corre en Vercel; si no, `http://localhost:5173`. |
| `VITE_API_URL` | Base versionada de la API, por ejemplo `https://api.example.com/api/v1`. | En desarrollo usa `http://localhost:8080/api/v1`; en Vercel es obligatoria. |

`robots.txt` y `sitemap.xml` **no están versionados**: los genera `vite.config.ts` durante el build, para que el dominio viva en un solo lugar. Al agregar una ruta pública hay que sumarla a `RUTAS_PUBLICAS` en ese archivo.

Copiá `.env.example` a `.env.local` si querés fijar la URL en desarrollo.

## Despliegue

Publicado en **Vercel** con integración de GitHub. La configuración vive en `vercel.json`: reescrituras de SPA (sin ellas, entrar directo a `/nosotros` da 404), caché inmutable para `/assets/*` y cabeceras de seguridad.

En Vercel, **Root Directory tiene que ser `frontend`**, porque el repositorio es un monorepo.

### Mientras el frontend viva en una rama

Hasta que `frontend/` llegue a `main`, la maqueta se publica como **previsualización de rama**. Cada push a `feature/frontend` genera un despliegue, y su URL es estable: siempre apunta al último despliegue de esa rama.

```text
https://<proyecto>-git-feature-frontend-<scope>.vercel.app
```

La URL exacta está en la pestaña **Deployments** del proyecto. No hace falta tocar la rama de producción para usarla.

### Builds de main que fallan

Vercel usa `main` como rama de producción y `main` todavía no tiene `frontend/`, así que **cada push a `main` dispara un build que falla** con `The specified Root Directory "frontend" does not exist`. No rompe nada, pero le llega un mail de error a todo el equipo.

Dos formas de cortarlo, cualquiera sirve:

- **Settings → Environments → Production → Branch Tracking**: apuntar producción a `feature/frontend`. En cuentas más viejas el ajuste está en **Settings → Git → Production Branch**.
- **Settings → Git → Ignored Build Step**: poner el comando

  ```bash
  git diff --quiet HEAD^ HEAD -- .
  ```

  Vercel salta el build cuando ese comando devuelve 0, es decir cuando el commit no tocó el Root Directory. Es lo habitual en un monorepo y conviene dejarlo puesto aunque después se resuelva lo otro.

Cuando `frontend/` llegue a `main`, la rama de producción vuelve a ser `main` y esto deja de aplicar.

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
│   ├── panel/           Pantalla principal del CRM
│   ├── empresas/        Listado, detalle, alta y edición
│   ├── contactos/       Listado, detalle, alta y edición
│   ├── salones/         Catálogo precargado
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

## Integración con la API

`src/shared/api/` contiene el cliente `fetch`, el contrato generado y TanStack Query;
`src/shared/sesion/` resuelve token, usuario, expiración y persistencia. Login y cambio
de clave ya usan estas capas.

Los módulos `api*.ts` derivan sus DTO del esquema generado desde
`backend/docs/openapi.yaml`. TanStack Query gestiona caché e invalidación; los formularios
comerciales usan React Hook Form y Zod, y los filtros/paginación se reflejan en la URL.

## Próximo corte de integración

1. Agregar navegación y pantallas administrativas por rol.
2. Incorporar el alta de actividades y los cierres cuando existan sus endpoints.
