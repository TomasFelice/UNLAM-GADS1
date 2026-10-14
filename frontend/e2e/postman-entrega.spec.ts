import { expect, test } from "@playwright/test";
import { createRequire } from "node:module";
import { readFileSync } from "node:fs";

test("Newman importa environment y colección completa, captura IDs y valida el contrato", async ({ request }) => {
  test.setTimeout(120000);
  const cargar = createRequire(import.meta.url);
  // Instalación de validación aislada, sin agregar dependencias de producción.
  const newman = cargar("../../backend/target/newman/node_modules/newman");
  const coleccion = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM.postman_collection.json", "utf8"));
  const entorno = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM-Local.postman_environment.json", "utf8"));
  const variables = { baseUrl: "http://localhost:18080/api/v1", email: "admin.baseline.b@fixtures.invalid", password: "FixtureTemporal123!" };
  for (const [key, value] of Object.entries(variables)) {
    const entrada = entorno.values.find((v: any) => v.key === key);
    expect(entrada, key).toBeDefined(); entrada.value = value;
  }
  const resultado: any = await new Promise((resolve, reject) => newman.run({ collection: coleccion, environment: entorno, reporters: [], timeoutRequest: 15000 }, (error: Error | null, resumen: any) => error ? reject(error) : resolve(resumen)));
  const fallas = resultado.run.failures.map((f: any) => ({ origen: f.source?.name, mensaje: f.error.message }));
  const respuestasFallidas = resultado.run.executions.filter((e: any) => e.response?.code >= 400).map((e: any) => ({ nombre: e.item.name, estado: e.response.code, error: e.response.json() }));
  await test.info().attach("newman", { body: JSON.stringify({ solicitudes: resultado.run.stats.requests, aserciones: resultado.run.stats.assertions, fallas, respuestasFallidas }, null, 2), contentType: "application/json" });
  expect(fallas).toEqual([]);
  expect(resultado.run.stats.requests.total).toBeGreaterThan(90);
  const contrato = await (await request.get("http://localhost:18080/v3/api-docs")).json();
  const operaciones = new Set(coleccion.item.flatMap((carpeta: any) => carpeta.item.map((item: any) => `${item.request.method.toLowerCase()} /api/v1${item.request.url.raw.replace("{{baseUrl}}", "").split("?")[0].replace(/\{\{\w+\}\}/g, "{id}")}`)));
  for (const [ruta, metodos] of Object.entries(contrato.paths)) for (const metodo of Object.keys(metodos as object)) {
    if (["get", "post", "put", "delete", "patch"].includes(metodo)) expect(operaciones.has(`${metodo} ${ruta}`), `${metodo} ${ruta}: falta en Postman`).toBe(true);
  }
  const runtime = await (await request.get("http://localhost:18080/v3/api-docs.yaml")).text();
  const normalizar = (texto: string) => texto.replace(/http:\/\/localhost:\d+/g, "http://localhost:PUERTO").replace(/\r\n/g, "\n");
  expect(normalizar(runtime)).toBe(normalizar(readFileSync("../backend/docs/openapi.yaml", "utf8")));
});
