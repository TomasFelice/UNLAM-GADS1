# Línea base de carga — 09/10/2026

Capturada antes de cambiar pantallas o consultas de producción. No hay comparación
posterior ni una mejora demostrada todavía. Los datos de las **48 muestras** están
en [linea-base-carga.json](linea-base-carga.json), con cada solicitud, estado, bytes,
espera, descarga, tiempo en servidor y conteo de sentencias Hibernate.

Fuente del runtime medido: archivos de producción de
`957032a789e0be2d25b2d927989b8c7eec89c34c`, sin modificaciones al iniciar el apply.
El launcher y los fixtures de test se agregaron para instrumentar esta captura.

La [comparación posterior de §10](comparacion-carga-punto-10.md), del 10/10/2026,
conserva esta línea base y documenta resultados, límites y una corrección de frontera
de muestreo necesaria tras incorporar rutas lazy. Los resultados siguientes siguen
describiendo exclusivamente la captura inicial.

## Condiciones y dataset

Windows, JDK Temurin 25.0.4.1, PostgreSQL 16 efímero de Testcontainers y Chromium
153.0.8010.12, viewport 1440 × 900, red local sin throttling, build de producción.
El proceso controla únicamente su base efímera; no carga `.env` ni modifica Supabase.

Se conservó el seed V1 y se agregaron los dos tenants de `CommercialFixtures`.
El administrador del primero ve 100 oportunidades abiertas, 100 actividades,
30 empresas, 30 contactos, un salón, cuatro servicios y catálogos de prueba.
Dos vendedores comparten empresa/contacto y tienen oportunidades propias. Las fechas
son constantes; se crea el dataset una vez y se conserva al reiniciar la API.

- **Servidor despierto:** visita previa de la misma ruta en otro contexto, descartado
  antes de la muestra para conservar fría la caché del navegador medido.
- **Servidor frío/reiniciado:** proceso JVM nuevo antes de cada muestra, misma base
  PostgreSQL. Se registra el arranque Spring por separado; no se suma a la carga.
- **Caché fría:** contexto y página nuevos, sin caché HTTP ni TanStack Query previa.
- **Caché caliente:** navegación SPA a `/nosotros` y vuelta dentro de los 30 s de
  `staleTime`; conserva el bundle y las consultas. Para servidor frío se reinicia
  también la API antes de volver, manteniendo el navegador abierto.
- Login fuera de muestra. SQL contado por hilo/request con `StatementInspector`,
  incluyendo autenticación y consultas de la ruta; no se registran textos SQL.

## Resultados

Promedios de tres corridas por fila. Bytes corresponden a cuerpos de respuestas de
toda la navegación registrados por Playwright; el JSON también distingue JS y API.
El tiempo va desde navegar hasta red quieta y dos frames de render; incluye la
ventana de 500 ms de `networkidle` cuando corresponde, que puede resolverse
inmediatamente en una vuelta sin red. No representa tiempo de render exclusivo.

| Ruta | Servidor | Caché | Solicitudes API | SQL | Bytes | Carga ms | Arranque Spring ms |
|---|---|---|---:|---:|---:|---:|---:|
| Panel | Despierto | Fría | 3 | 745 | 350594 | 1894,17 | — |
| Panel | Despierto | Caliente | 0 | 0 | 0 | 78,92 | — |
| Panel | Reiniciado | Fría | 3 | 745 | 350594 | 2133,91 | 13403,00 |
| Panel | Reiniciado | Caliente | 0 | 0 | 0 | 69,79 | 12470,33 |
| Lista | Despierto | Fría | 7 | 157 | 312139 | 1058,94 | — |
| Lista | Despierto | Caliente | 0 | 0 | 0 | 81,72 | — |
| Lista | Reiniciado | Fría | 7 | 157 | 312139 | 1280,96 | 11734,33 |
| Lista | Reiniciado | Caliente | 0 | 0 | 0 | 66,57 | 11410,00 |
| Detalle | Despierto | Fría | 6 | 62 | 302601 | 896,94 | — |
| Detalle | Despierto | Caliente | 0 | 0 | 0 | 71,18 | — |
| Detalle | Reiniciado | Fría | 6 | 62 | 302601 | 1442,52 | 12386,33 |
| Detalle | Reiniciado | Caliente | 0 | 0 | 0 | 62,03 | 13740,67 |
| Alta | Despierto | Fría | 8 | 38 | 303702 | 852,92 | — |
| Alta | Despierto | Caliente | 0 | 0 | 0 | 58,41 | — |
| Alta | Reiniciado | Fría | 8 | 38 | 303702 | 1020,95 | 12718,67 |
| Alta | Reiniciado | Caliente | 0 | 0 | 0 | 55,27 | 12055,00 |

La ausencia de solicitudes en caliente verifica reutilización local dentro de
`staleTime`, aun con API reiniciada; no demuestra reducción de trabajo del servidor
ante nuevas consultas. Panel y lista son los candidatos principales para el corte §10.

## Repetir

Desde `backend/`, con JDK 25 y Docker disponibles:

```powershell
./mvnw.cmd test-compile dependency:build-classpath -Dmdep.outputFile=target/baseline-classpath.txt -Dmdep.includeScope=test
```

Desde `frontend/`, con dependencias instaladas:

```powershell
npx playwright install chromium
$env:VITE_API_URL='http://localhost:18080/api/v1'
npm run build
node scripts/medir-carga.mjs ../backend/target/linea-base-carga.json
```

Los puertos 18080 y 14173 deben estar libres. El script cierra navegador, preview,
JVMs y contenedor al terminar. El build de medición queda en `dist`, ignorado por Git;
un build normal vuelve a usar la configuración habitual.

## Límites

No reproduce suspensión ni latencia de Render/Supabase; mide un arranque real de
JVM/API con PostgreSQL ya disponible. Los tiempos locales pueden variar por carga
de la estación (se ejecutaron checks independientes durante parte de la captura).
Para §10.5 repetir dataset, navegador, instrumentación y condiciones declaradas,
comparar también conteos/bytes y explicitar variaciones de carga. No afirmar SLA
ni atribuir diferencias de hosting al frontend. La captura no incluye cuerpos de
respuesta, tokens ni cabeceras de autorización.
