import { chromium } from "@playwright/test";
import { spawn } from "node:child_process";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { createInterface } from "node:readline";
import { resolve, delimiter } from "node:path";
import { fileURLToPath } from "node:url";
import { once } from "node:events";

const raiz = fileURLToPath(new URL("../../", import.meta.url));
const frontend = resolve(raiz, "frontend");
const backend = resolve(raiz, "backend");
const salida = resolve(process.argv[2] ?? resolve(backend, "target/linea-base-carga.json"));
const servidores = process.argv[3] ? [process.argv[3]] : ["despierto", "reiniciado"];
if (servidores.some(valor => !["despierto", "reiniciado"].includes(valor))) throw new Error("Condición de servidor inválida.");
const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, "bin/java.exe") : "java";
const dependencias = (await readFile(resolve(backend, "target/baseline-classpath.txt"), "utf8")).trim();
const api = spawn(java, ["-cp", [resolve(backend, "target/test-classes"), resolve(backend, "target/classes"), dependencias].join(delimiter),
  "com.ztech.crm.support.LoadBaselineApplication"], { cwd: backend, windowsHide: true });
const lineas = createInterface({ input: api.stdout });
let resolverLista;
let rechazarLista;
let ultimoEstado;
let diagnostico = "";
api.stderr.on("data", bloque => { diagnostico = (diagnostico + bloque.toString()).slice(-4000); });
lineas.on("line", linea => {
  if (linea.startsWith("BASELINE_READY ")) {
    ultimoEstado = JSON.parse(linea.slice("BASELINE_READY ".length));
    resolverLista?.(ultimoEstado);
    resolverLista = undefined;
  } else diagnostico = (diagnostico + linea + "\n").slice(-4000);
});
api.on("exit", codigo => rechazarLista?.(new Error(`API terminó (${codigo}): ${diagnostico}`)));
function esperarApi() {
  return new Promise((resolver, rechazar) => {
    const limite = setTimeout(() => rechazar(new Error(`API no lista: ${diagnostico}`)), 60000);
    resolverLista = estado => { clearTimeout(limite); resolver(estado); };
    rechazarLista = error => { clearTimeout(limite); rechazar(error); };
  });
}
async function reiniciarApi() {
  const lista = esperarApi();
  api.stdin.write("restart\n");
  return lista;
}

const vista = spawn(process.execPath, [resolve(frontend, "node_modules/vite/bin/vite.js"), "preview", "--host", "127.0.0.1", "--port", "14173", "--strictPort"],
  { cwd: frontend, windowsHide: true, stdio: "ignore" });
let navegador;
const resultados = [];
const tareas = new Set();
const redondear = numero => Math.round(numero * 100) / 100;
try {
  let estado = await esperarApi();
  // El proceso de preview puede iniciar mientras se prepara PostgreSQL.
  const respuestaVista = await fetch("http://localhost:14173");
  if (!respuestaVista.ok) throw new Error("Preview no disponible");
  navegador = await chromium.launch();
  const rutas = [
    { nombre: "panel", ruta: "/app", titulo: "Panel comercial" },
    { nombre: "lista", ruta: "/app/oportunidades", titulo: "Oportunidades" },
    { nombre: "detalle", ruta: `/app/oportunidades/${estado.opportunityId}`, titulo: "baseline.a propia" },
    { nombre: "alta", ruta: "/app/oportunidades/nueva", titulo: "Nueva oportunidad" },
  ];

  async function sesion(contexto) {
    const respuesta = await contexto.request.post("http://localhost:18080/api/v1/auth/login", {
      data: { email: estado.email, password: "FixtureTemporal123!" },
    });
    if (!respuesta.ok()) throw new Error(`Login: ${respuesta.status()}`);
    const datos = await respuesta.json();
    await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({
      token: datos.accessToken, venceEn: Date.now() + datos.expiresInSeconds * 1000,
      debeCambiarClave: datos.mustChangePassword, usuario: { id: datos.user.id, email: datos.user.email,
        nombre: datos.user.firstName, apellido: datos.user.lastName, rol: datos.user.role },
    })), datos);
  }

  async function navegarSpa(pagina, ruta) {
    await pagina.evaluate(ruta => {
      history.pushState(null, "", ruta);
      dispatchEvent(new PopStateEvent("popstate"));
    }, ruta);
  }

  async function medir(pagina, ruta, servidor, cache, corrida, arranqueMs) {
    const solicitudes = [];
    const errores = [];
    const capturar = solicitud => {
      const tarea = (async () => {
        const respuesta = await solicitud.response();
        if (!respuesta) return;
        const tamanos = await solicitud.sizes();
        const headers = await respuesta.allHeaders();
        const tiempos = solicitud.timing();
        const esApi = solicitud.url().includes("/api/v1/");
        if (respuesta.status() >= 400) errores.push(`${respuesta.status()} ${new URL(solicitud.url()).pathname}`);
        solicitudes.push({ ruta: new URL(solicitud.url()).pathname + new URL(solicitud.url()).search,
          tipo: solicitud.resourceType(), estado: respuesta.status(), esApi,
          bytes: tamanos.responseBodySize, cabecerasBytes: tamanos.responseHeadersSize,
          esperaMs: redondear(Math.max(0, tiempos.responseStart - tiempos.requestStart)),
          descargaMs: redondear(Math.max(0, tiempos.responseEnd - tiempos.responseStart)),
          sql: Number(headers["x-baseline-sql-count"] ?? 0), servidorMs: Number(headers["x-baseline-server-ms"] ?? 0) });
      })();
      tareas.add(tarea);
      tarea.finally(() => tareas.delete(tarea));
    };
    const errorConsola = error => errores.push(error.message);
    pagina.on("requestfinished", capturar);
    pagina.on("pageerror", errorConsola);
    const inicio = performance.now();
    if (cache === "fria") await pagina.goto(`http://localhost:14173${ruta.ruta}`);
    else await navegarSpa(pagina, ruta.ruta);
    await pagina.getByRole("heading", { name: ruta.titulo, exact: true, level: 1 }).waitFor();
    await pagina.waitForLoadState("networkidle");
    await Promise.all([...tareas]);
    // Dos frames después de red quieta; el margen fijo se declara separado, no se resta.
    await pagina.evaluate(() => new Promise(resolver => requestAnimationFrame(() => requestAnimationFrame(resolver))));
    const totalMs = performance.now() - inicio;
    pagina.off("requestfinished", capturar);
    pagina.off("pageerror", errorConsola);
    if (errores.length) throw new Error(errores.join("\n"));
    const registro = { ruta: ruta.nombre, servidor, cache, corrida, arranqueMs,
      hastaRedQuietaYRenderMs: redondear(totalMs), margenRedQuietaMs: 500,
      solicitudes: solicitudes.length, solicitudesApi: solicitudes.filter(s => s.esApi).length,
      bytes: solicitudes.reduce((s, r) => s + r.bytes, 0),
      jsBytes: solicitudes.filter(s => s.tipo === "script").reduce((s, r) => s + r.bytes, 0),
      sql: solicitudes.reduce((s, r) => s + r.sql, 0), red: solicitudes };
    resultados.push(registro);
    console.log(`${ruta.nombre} servidor=${servidor} caché=${cache} corrida=${corrida}: ${registro.solicitudesApi} API, ${registro.sql} SQL, ${registro.hastaRedQuietaYRenderMs} ms`);
  }

  for (const servidor of servidores) {
    for (const ruta of rutas) {
      for (let corrida = 1; corrida <= 3; corrida++) {
        if (servidor === "reiniciado") estado = await reiniciarApi();
        // Despierto: calentamiento del endpoint que se medirá, sin incluirlo en la muestra.
        if (servidor === "despierto") {
          const contextoCaliente = await navegador.newContext();
          await sesion(contextoCaliente);
          const calentamiento = await contextoCaliente.newPage();
          await calentamiento.goto(`http://localhost:14173${ruta.ruta}`);
          await calentamiento.waitForLoadState("networkidle");
          await contextoCaliente.close();
        }
        const contexto = await navegador.newContext({ viewport: { width: 1440, height: 900 } });
        await sesion(contexto);
        const pagina = await contexto.newPage();
        await medir(pagina, ruta, servidor, "fria", corrida, servidor === "reiniciado" ? estado.startupMs : 0);
        await navegarSpa(pagina, "/nosotros");
        await pagina.waitForURL("**/nosotros");
        // Suspense puede conservar el heading anterior mientras llegan los chunks públicos.
        // Terminar esa navegación antes de abrir la ventana de medición siguiente.
        await pagina.getByRole("heading", { name: "Un equipo de estudiantes, un producto real", exact: true, level: 1 }).waitFor();
        await pagina.waitForLoadState("networkidle");
        if (servidor === "reiniciado") estado = await reiniciarApi();
        await medir(pagina, ruta, servidor, "caliente", corrida, servidor === "reiniciado" ? estado.startupMs : 0);
        await contexto.close();
      }
    }
  }
  await mkdir(resolve(salida, ".."), { recursive: true });
  await writeFile(salida, JSON.stringify({ fecha: new Date().toISOString(), navegador: navegador.version(),
    dataset: "CommercialFixtures baseline.a: 100 oportunidades, 100 actividades, 30 empresas/contactos, 4 servicios; segundo tenant de fixture y seed V1 aislados",
    condiciones: "localhost, build producción, 1440x900, sin throttling; API reiniciada en JVM nueva/PG estable; login fuera de muestra; caché fría nueva página/contexto, caliente vuelta SPA dentro de staleTime; SQL Hibernate por hilo/request",
    resultados }, null, 2) + "\n");
  console.log(`Resultado: ${salida}`);
} finally {
  await navegador?.close();
  vista.kill();
  api.stdin.end("stop\n");
  if (api.exitCode === null) await Promise.race([once(api, "exit"), new Promise(resolver => setTimeout(() => { api.kill(); resolver(); }, 10000))]);
  lineas.close();
}
