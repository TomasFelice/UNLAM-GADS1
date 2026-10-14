import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

const evidencias = fileURLToPath(new URL("../../openspec/changes/completar-crm-entrega-final/evidencias/", import.meta.url));
const antes = JSON.parse(await readFile(resolve(process.argv[2] ?? resolve(evidencias, "linea-base-carga.json")), "utf8"));
const despues = JSON.parse(await readFile(resolve(process.argv[3] ?? resolve(evidencias, "carga-punto-10.json")), "utf8"));
const salida = resolve(process.argv[4] ?? resolve(evidencias, "comparacion-carga-punto-10.md"));
for (const clave of ["navegador", "dataset", "condiciones"]) {
  if (antes[clave] !== despues[clave]) throw new Error(`Condición distinta: ${clave}. Revisar antes de comparar.`);
}
for (const datos of [antes, despues]) {
  if (datos.resultados.length !== 48) throw new Error("Se requieren 48 muestras por captura.");
}
const promedio = (filas, valor) => filas.reduce((suma, fila) => suma + valor(fila), 0) / filas.length;
const numero = valor => valor.toLocaleString("es-AR", { maximumFractionDigits: 2 });
const par = (a, d) => `${numero(a)} → ${numero(d)}`;
const filas = (datos, ruta, servidor, cache) => {
  const resultado = datos.resultados.filter(f => f.ruta === ruta && f.servidor === servidor && f.cache === cache);
  if (resultado.length !== 3 || new Set(resultado.map(f => f.corrida)).size !== 3) throw new Error(`Faltan corridas: ${ruta}/${servidor}/${cache}`);
  if (resultado.some(f => f.red.some(r => r.estado >= 400))) throw new Error("La captura contiene errores HTTP.");
  return resultado;
};
const bytes = (filas, tipo) => promedio(filas, f => f.red.filter(tipo).reduce((suma, r) => suma + r.bytes, 0));
const texto = [
  "# Comparación de carga — §10, 10/10/2026", "",
  "Capturas: [antes, 09/10](linea-base-carga.json) y [después, 10/10](carga-punto-10.json).",
  "Cada valor es el promedio de tres corridas; `antes → después`. Hay 48 muestras por captura.", "",
  "## Condiciones reproducidas", "",
  `- Chromium ${despues.navegador}, JDK Temurin 25.0.4.1, PostgreSQL 16 efímero, Windows, viewport 1440×900, build de producción, red local sin throttling.`,
  `- Dataset idéntico: ${despues.dataset}.`,
  "- Mismo launcher `frontend/scripts/medir-carga.mjs`, fixtures e instrumentación por hilo/request. Login fuera de muestra; JVM nueva/base conservada al reiniciar. Caché caliente: vuelta SPA dentro de 30 s.",
  "- Se corrigió la frontera previa a la vuelta caliente: ahora espera el heading de Nosotros y red quieta. Con rutas lazy, la espera genérica anterior podía contar cuatro chunks públicos (6.135 bytes) dentro de la siguiente muestra, sin consultas comerciales. Se conservan las 24 muestras reiniciadas de la [captura inicial](carga-punto-10-inicial.json), cuya espera del reinicio ya separaba ambas rutas, y se reemplazan las 24 despiertas por una [repetición corregida](carga-punto-10-despierto.json). El JSON final identifica ambas fuentes: 48 muestras válidas, de 72 ejecutadas en esta sesión. No se descuentan bytes manualmente.",
  "- El script comparador comprueba igualdad de dataset, navegador y condiciones y exige tres corridas por cada combinación, sin errores HTTP.",
  "- Las suites terminaron antes de esta captura. La línea base original advierte carga de otras comprobaciones durante parte de su ejecución: no usar diferencias pequeñas de tiempos como prueba de mejora.",
  "- La referencia anterior corresponde al código inicial del apply; el resultado incluye las funcionalidades de §2–§9, además de la optimización de §10. El mayor número de selectores de alta tiene esa explicación funcional.", "",
  "## Caché fría", "",
  "Bytes de cuerpos de respuestas medidos por Playwright; cabeceras por separado en JSON. SQL incluye autenticación. Solicitudes totales incluyen documento, JS, CSS e imágenes; API es el subconjunto comercial. Los totales también incluyen fuentes externas de Google Fonts: se observó una respuesta tipográfica diferente en una corrida de detalle, por eso se informan aparte los bytes API y JS propios.", "",
  "| Ruta | Servidor | Solicitudes totales | API | SQL | Bytes totales | Bytes API | Bytes JS | Carga ms | Arranque Spring ms |",
  "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|",
];
const nombres = { panel: "Panel", lista: "Lista", detalle: "Detalle", alta: "Alta" };
for (const ruta of Object.keys(nombres)) for (const servidor of ["despierto", "reiniciado"]) {
  const a = filas(antes, ruta, servidor, "fria"), d = filas(despues, ruta, servidor, "fria");
  const comparar = propiedad => par(promedio(a, f => f[propiedad]), promedio(d, f => f[propiedad]));
  texto.push(`| ${nombres[ruta]} | ${servidor} | ${comparar("solicitudes")} | ${comparar("solicitudesApi")} | ${comparar("sql")} | ${comparar("bytes")} | ${par(bytes(a, r => r.esApi), bytes(d, r => r.esApi))} | ${comparar("jsBytes")} | ${comparar("hastaRedQuietaYRenderMs")} | ${servidor === "despierto" ? "—" : comparar("arranqueMs")} |`);
}
texto.push("", "## Caché caliente", "", "| Ruta | Servidor | Solicitudes totales | API | SQL | Bytes | Carga ms | Arranque Spring ms |", "|---|---|---:|---:|---:|---:|---:|---:|");
for (const ruta of Object.keys(nombres)) for (const servidor of ["despierto", "reiniciado"]) {
  const a = filas(antes, ruta, servidor, "caliente"), d = filas(despues, ruta, servidor, "caliente");
  const comparar = propiedad => par(promedio(a, f => f[propiedad]), promedio(d, f => f[propiedad]));
  texto.push(`| ${nombres[ruta]} | ${servidor} | ${comparar("solicitudes")} | ${comparar("solicitudesApi")} | ${comparar("sql")} | ${comparar("bytes")} | ${comparar("hastaRedQuietaYRenderMs")} | ${servidor === "despierto" ? "—" : comparar("arranqueMs")} |`);
}
texto.push("", "## Espera, servidor y descarga", "",
  "Sumas de tiempos por request API, promediadas por corrida. Las solicitudes se solapan: estas sumas NO equivalen al tiempo de navegación ni son una descomposición aditiva. Espera incluye transporte/servidor; servidor se mide en el filtro de test; descarga mide recepción del cuerpo. Los JSON conservan el detalle de cada solicitud.", "",
  "| Ruta | Servidor | Σ espera API ms | Σ servidor API ms | Σ descarga API ms |", "|---|---|---:|---:|---:|");
for (const ruta of Object.keys(nombres)) for (const servidor of ["despierto", "reiniciado"]) {
  const a = filas(antes, ruta, servidor, "fria"), d = filas(despues, ruta, servidor, "fria");
  const comparar = propiedad => par(promedio(a, f => f.red.filter(r => r.esApi).reduce((s, r) => s + r[propiedad], 0)), promedio(d, f => f.red.filter(r => r.esApi).reduce((s, r) => s + r[propiedad], 0)));
  texto.push(`| ${nombres[ruta]} | ${servidor} | ${comparar("esperaMs")} | ${comparar("servidorMs")} | ${comparar("descargaMs")} |`);
}
texto.push("", "## Interpretación y límites", "",
  "- Panel: se elimina el tablero de 100 oportunidades y la página de 20 recortada en cliente. Se solicitan cinco oportunidades y ocho actividades; SQL 745 → 28. El listado de oportunidades pasa de 157 → 44 SQL incluyendo sus selectores.",
  "- `OpportunityLoadIT` separa ese listado de sus selectores: 9 SQL para 20 y 100 oportunidades ADMIN; tablero 11. SELLER 11/10 SQL para 20/100, manteniendo 50 oportunidades propias. El count se omite cuando la página no se llena.",
  "- Las rutas cargan sus chunks activos. Puede aumentar la cantidad de archivos JS/CSS aunque baje su volumen; no se interpreta el conteo total de requests como mejora por sí solo.",
  "- Alta y detalle no mejoran todos sus conteos respecto del inicio del apply: incorporan embudos y selectores paginados/por ID para conservar datos históricos y selecciones posteriores a página cero. Cada selector solicita una página de 20; no recorre todas automáticamente. No se oculta ese costo funcional ni se afirma una mejora universal de tiempos.",
  "- La vuelta caliente conserva cero solicitudes/bytes/SQL, incluso tras reiniciar la API. Esto verifica caché local; no demuestra trabajo de servidor ante una consulta nueva.",
  "- Carga va desde navegar hasta red quieta y dos frames. Incluye hasta 500 ms de `networkidle`; no representa render puro. Arranque Spring se mide aparte y no se suma. Backend local, con fuentes de CDN externas; no reproduce Render/Supabase remotos ni fija un SLA.", "",
  "## Repetir", "", "Desde `backend/`, preparar el classpath de test según README frontend. Desde `frontend/`:", "",
  "```powershell", "$env:VITE_API_URL='http://localhost:18080/api/v1'", "npm run build",
  "node scripts/medir-carga.mjs ../openspec/changes/completar-crm-entrega-final/evidencias/carga-punto-10.json",
  "node scripts/comparar-carga.mjs", "```", "",
  "API/preview y PostgreSQL se cierran al finalizar. Puertos 18080/14173 libres. Ejecutar sin otras suites en paralelo; restaurar el build habitual al terminar. El comparador admite rutas antes/después/salida como argumentos.", "",
);
await writeFile(salida, texto.join("\n"));
console.log(salida);
