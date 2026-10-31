# Despliegue de producción — 19/10/2026

## Versión publicada

- Commit: `81454a81e455478e0119fecf5678de9ac0aec9e3`, rama
  `feature/faltantesEntregaFinal`. No se hicieron commits ni pushes durante el despliegue.
- Frontend: <https://ztech-crm.benai.com.ar>, Vercel `frontend`, despliegue
  `dpl_9WGinqqEHoHLdhLR75pjmAYFAbKW`, estado `READY`.
  Se promovió la compilación preparada con variables de producción, sin reconstruirla.
- API: <https://unlam-gads1.onrender.com>, servicio Render
  `srv-dam9fmijnfac73ea65ng`, despliegue `dep-dbb8an3bc2fs73des4f0`.
  Render confirmó `Deploy succeeded | Live`, duración 5m29s.
- PostgreSQL: Supabase `zlduzfjmdmafpenidpss`, conexión Supavisor session.

Render se desplegó manualmente por commit específico. Ese modo desactiva Auto-Deploy;
la rama configurada sigue siendo `main`. Un despliegue futuro debe seleccionar la
versión compatible de API y frontend; no usar el último commit de `main` por inferencia.

## Migraciones en Supabase

Flyway se ejecutó durante el arranque de la API con su conexión ya configurada en
Render. La V1 no difiere de la versión previamente desplegada. No se usó el historial
de migraciones de Supabase CLI ni se modificaron migraciones aplicadas.

El 19/10 a las 18:05:32 (Buenos Aires), Flyway validó cinco migraciones y encontró
`public` en V1. Aplicó en orden:

| Versión | Migración |
|---|---|
| V2 | `normalize_company_cuit` |
| V3 | `add_commercial_pipelines` |
| V4 | `snapshot_commercial_history` |
| V5 | `index_activity_history` |

A las 18:05:37 registró:

```text
Successfully applied 4 migrations to schema "public", now at version v5
```

La evidencia procede de los [logs del despliegue en Render](https://dashboard.render.com/web/srv-dam9fmijnfac73ea65ng/deploys/dep-dbb8an3bc2fs73des4f0).
El conector Supabase de esta sesión no tenía permisos sobre el proyecto; no se afirma
una consulta SQL directa posterior a `flyway_schema_history`.

## Validaciones de esta sesión

- Backend: `./mvnw.cmd clean verify`, `BUILD SUCCESS`, 5 unitarias y 109 IT correctos
  contra PostgreSQL efímero de Testcontainers.
- Frontend: build de producción y 47 pruebas Vitest correctos.
- Playwright local: tres recorridos correctos de `entrega-integrada.spec.ts` y
  `ciclo-oportunidades.spec.ts`, incluidos login, clientes, oportunidad, tablero,
  actividad, cierre y reapertura. No se ejecutó toda la suite E2E en esta sesión.
- API publicada: `/actuator/health` devuelve `200` y `status: UP`.
- `/v3/api-docs` publica 44 rutas y 69 operaciones; los pares método/ruta coinciden
  con `backend/docs/openapi.yaml`. Esto no certifica igualdad de todos los esquemas DTO.
- Preflight CORS del login: `200`, origen permitido `https://ztech-crm.benai.com.ar`.
  `GET /api/v1/pipelines` sin JWT devuelve `401`.
- El dominio de Vercel apunta al despliegue nuevo y sirve `index-BS7qB6s2.js`.
  El bundle consume la API de Render; el sitemap usa el dominio de producción.
- Consulta de logs runtime de Vercel del despliegue, últimos 15 minutos con
  `level:error`: cero registros. Es un frontend estático; este resultado no sustituye
  la revisión de los logs de la API en Render.
- Navegador en producción a 390 px: `/`, `/nosotros`, `/contacto`, `/terminos` y
  `/privacidad` con título, descripción y canonical propios; sin scroll horizontal
  ni errores de consola. `/app/configuracion/embudos` sin sesión redirige a
  `/ingresar`, que conserva `noindex, nofollow` y no presenta desbordamiento horizontal.
- `git diff --check` y revisión de los enlaces locales agregados.

## Límites

No se realizó un recorrido comercial autenticado ni se crearon datos de prueba en
producción. La demostración integral remota, las rutas con los tres roles y las
pruebas de aislamiento/concurrencia remotas siguen pendientes; sus resultados locales
no se presentan como evidencia remota. El cierre archivado de OpenSpec conserva
su estado histórico de 71/72 tareas.

Para revertir el frontend puede promoverse la versión anterior de Vercel
`dpl_6LCF2zXgeKVzYtKqsAsD7xtB5m8s`. Una reversión de API requiere comprobar
compatibilidad con el esquema V5; no debe revertirse el esquema ni borrarse el historial
Flyway como parte de un rollback de aplicación.
