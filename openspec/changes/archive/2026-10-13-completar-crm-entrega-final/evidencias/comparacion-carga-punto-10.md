# Comparación de carga — §10, 10/10/2026

Capturas: [antes, 09/10](linea-base-carga.json) y [después, 10/10](carga-punto-10.json).
Cada valor es el promedio de tres corridas; `antes → después`. Hay 48 muestras por captura.

## Condiciones reproducidas

- Chromium 153.0.8010.12, JDK Temurin 25.0.4.1, PostgreSQL 16 efímero, Windows, viewport 1440×900, build de producción, red local sin throttling.
- Dataset idéntico: CommercialFixtures baseline.a: 100 oportunidades, 100 actividades, 30 empresas/contactos, 4 servicios; segundo tenant de fixture y seed V1 aislados.
- Mismo launcher `frontend/scripts/medir-carga.mjs`, fixtures e instrumentación por hilo/request. Login fuera de muestra; JVM nueva/base conservada al reiniciar. Caché caliente: vuelta SPA dentro de 30 s.
- Se corrigió la frontera previa a la vuelta caliente: ahora espera el heading de Nosotros y red quieta. Con rutas lazy, la espera genérica anterior podía contar cuatro chunks públicos (6.135 bytes) dentro de la siguiente muestra, sin consultas comerciales. Se conservan las 24 muestras reiniciadas de la [captura inicial](carga-punto-10-inicial.json), cuya espera del reinicio ya separaba ambas rutas, y se reemplazan las 24 despiertas por una [repetición corregida](carga-punto-10-despierto.json). El JSON final identifica ambas fuentes: 48 muestras válidas, de 72 ejecutadas en esta sesión. No se descuentan bytes manualmente.
- El script comparador comprueba igualdad de dataset, navegador y condiciones y exige tres corridas por cada combinación, sin errores HTTP.
- Las suites terminaron antes de esta captura. La línea base original advierte carga de otras comprobaciones durante parte de su ejecución: no usar diferencias pequeñas de tiempos como prueba de mejora.
- La referencia anterior corresponde al código inicial del apply; el resultado incluye las funcionalidades de §2–§9, además de la optimización de §10. El mayor número de selectores de alta tiene esa explicación funcional.

## Caché fría

Bytes de cuerpos de respuestas medidos por Playwright; cabeceras por separado en JSON. SQL incluye autenticación. Solicitudes totales incluyen documento, JS, CSS e imágenes; API es el subconjunto comercial. Los totales también incluyen fuentes externas de Google Fonts: se observó una respuesta tipográfica diferente en una corrida de detalle, por eso se informan aparte los bytes API y JS propios.

| Ruta | Servidor | Solicitudes totales | API | SQL | Bytes totales | Bytes API | Bytes JS | Carga ms | Arranque Spring ms |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Panel | despierto | 10 → 24 | 3 → 2 | 745 → 28 | 350.594 → 246.000,33 | 55.330 → 4.836 | 163.480 → 112.102 | 1.894,17 → 1.025,82 | — |
| Panel | reiniciado | 10 → 24 | 3 → 2 | 745 → 28 | 350.594 → 245.999,67 | 55.330 → 4.836 | 163.480 → 112.102 | 2.133,91 → 1.221,61 | 13.403 → 11.369,33 |
| Lista | despierto | 14 → 39 | 7 → 7 | 157 → 44 | 312.139 → 267.437 | 16.875 → 18.169 | 163.480 → 120.331 | 1.058,94 → 1.059,35 | — |
| Lista | reiniciado | 14 → 39 | 7 → 7 | 157 → 44 | 312.139 → 267.436 | 16.875 → 18.169 | 163.480 → 120.331 | 1.280,96 → 1.274,1 | 11.734,33 → 10.974 |
| Detalle | despierto | 13 → 40 | 6 → 6 | 62 → 63 | 302.601 → 295.410 | 7.337 → 7.702 | 163.480 → 157.092 | 896,94 → 1.111,89 | — |
| Detalle | reiniciado | 13 → 40 | 6 → 6 | 62 → 63 | 302.600,67 → 292.093,67 | 7.337 → 7.702 | 163.480 → 157.092 | 1.442,52 → 1.383,06 | 12.386,33 → 11.590,67 |
| Alta | despierto | 15 → 39 | 8 → 11 | 38 → 47 | 303.702 → 293.758 | 8.438 → 9.924 | 163.480 → 156.264 | 852,92 → 1.072,58 | — |
| Alta | reiniciado | 15 → 39 | 8 → 11 | 38 → 47 | 303.702 → 293.757 | 8.438 → 9.924 | 163.480 → 156.264 | 1.020,95 → 1.314,85 | 12.718,67 → 11.395 |

## Caché caliente

| Ruta | Servidor | Solicitudes totales | API | SQL | Bytes | Carga ms | Arranque Spring ms |
|---|---|---:|---:|---:|---:|---:|---:|
| Panel | despierto | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 78,92 → 40,72 | — |
| Panel | reiniciado | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 69,79 → 40,22 | 12.470,33 → 11.306,67 |
| Lista | despierto | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 81,72 → 75,98 | — |
| Lista | reiniciado | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 66,57 → 58,33 | 11.410 → 11.387,67 |
| Detalle | despierto | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 71,18 → 44,57 | — |
| Detalle | reiniciado | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 62,03 → 57,35 | 13.740,67 → 11.311 |
| Alta | despierto | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 58,41 → 56,44 | — |
| Alta | reiniciado | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 55,27 → 61,46 | 12.055 → 11.358 |

## Espera, servidor y descarga

Sumas de tiempos por request API, promediadas por corrida. Las solicitudes se solapan: estas sumas NO equivalen al tiempo de navegación ni son una descomposición aditiva. Espera incluye transporte/servidor; servidor se mide en el filtro de test; descarga mide recepción del cuerpo. Los JSON conservan el detalle de cada solicitud.

| Ruta | Servidor | Σ espera API ms | Σ servidor API ms | Σ descarga API ms |
|---|---|---:|---:|---:|
| Panel | despierto | 1.530,84 → 84,15 | 1.523,67 → 80,67 | 36,74 → 10,72 |
| Panel | reiniciado | 2.168,62 → 409,84 | 2.160,67 → 405,33 | 75,85 → 37,6 |
| Lista | despierto | 486,07 → 245,47 | 470,67 → 231,33 | 109,65 → 75,63 |
| Lista | reiniciado | 1.403,7 → 1.141,47 | 1.385 → 1.121,67 | 215,42 → 161,78 |
| Detalle | despierto | 211,79 → 168,24 | 201,67 → 159,67 | 67,73 → 41,57 |
| Detalle | reiniciado | 1.969,47 → 569,51 | 1.941,33 → 554,33 | 443,68 → 67,58 |
| Alta | despierto | 244,91 → 216,65 | 225,67 → 198,67 | 114,67 → 105,06 |
| Alta | reiniciado | 1.133,43 → 1.449,91 | 1.108,67 → 1.419 | 258,24 → 219,6 |

## Interpretación y límites

- Panel: se elimina el tablero de 100 oportunidades y la página de 20 recortada en cliente. Se solicitan cinco oportunidades y ocho actividades; SQL 745 → 28. El listado de oportunidades pasa de 157 → 44 SQL incluyendo sus selectores.
- `OpportunityLoadIT` separa ese listado de sus selectores: 9 SQL para 20 y 100 oportunidades ADMIN; tablero 11. SELLER 11/10 SQL para 20/100, manteniendo 50 oportunidades propias. El count se omite cuando la página no se llena.
- Las rutas cargan sus chunks activos. Puede aumentar la cantidad de archivos JS/CSS aunque baje su volumen; no se interpreta el conteo total de requests como mejora por sí solo.
- Alta y detalle no mejoran todos sus conteos respecto del inicio del apply: incorporan embudos y selectores paginados/por ID para conservar datos históricos y selecciones posteriores a página cero. Cada selector solicita una página de 20; no recorre todas automáticamente. No se oculta ese costo funcional ni se afirma una mejora universal de tiempos.
- La vuelta caliente conserva cero solicitudes/bytes/SQL, incluso tras reiniciar la API. Esto verifica caché local; no demuestra trabajo de servidor ante una consulta nueva.
- Carga va desde navegar hasta red quieta y dos frames. Incluye hasta 500 ms de `networkidle`; no representa render puro. Arranque Spring se mide aparte y no se suma. Backend local, con fuentes de CDN externas; no reproduce Render/Supabase remotos ni fija un SLA.

## Repetir

Desde `backend/`, preparar el classpath de test según README frontend. Desde `frontend/`:

```powershell
$env:VITE_API_URL='http://localhost:18080/api/v1'
npm run build
node scripts/medir-carga.mjs ../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/evidencias/carga-punto-10.json
node scripts/comparar-carga.mjs
```

API/preview y PostgreSQL se cierran al finalizar. Puertos 18080/14173 libres. Ejecutar sin otras suites en paralelo; restaurar el build habitual al terminar. El comparador admite rutas antes/después/salida como argumentos.
