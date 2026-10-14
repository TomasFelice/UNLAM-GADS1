import { elegirSelector } from "./ayudas/selectores.js";
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { expect, test } from "@playwright/test";
const API = "http://localhost:18080/api/v1";
test("ADMIN configura dos embudos y consulta oportunidades históricas; otros roles no configuran", async ({ browser, request }) => {
  test.setTimeout(120000);
  const datos = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const headers = { Authorization: `Bearer ${datos.accessToken}` };
  const contexto = await browser.newContext({ viewport: { width: 390, height: 844 } });
  await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), datos);
  const page = await contexto.newPage(); const errores: string[] = [];
  page.on("pageerror", e => errores.push(e.message)); page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  const anterior = await (await request.get(`${API}/pipelines/default`, { headers })).json();
  const ids: number[] = [];
  for (const [nombre, plantilla] of [["Proceso breve E2E", "brief-hiring"], ["Proceso corporativo E2E", "corporate-events"]]) {
    await page.goto("/app/configuracion/embudos/nuevo");
    await page.getByRole("textbox", { name: "Nombre", exact: true }).fill(nombre);
    await page.getByRole("combobox", { name: "Plantilla" }).selectOption(plantilla);
    await expect(page.getByText("Ganada / Reservado · Ganada")).toBeVisible();
    await page.getByRole("button", { name: "Crear embudo", exact: true }).click();
    await expect(page.getByRole("heading", { name: nombre, exact: true })).toBeVisible();
    const confirmacion = page.getByRole("status").filter({ hasText: "Embudo creado." });
    await expect(confirmacion).toBeVisible();
    await expect(confirmacion).toHaveAttribute("aria-atomic", "true");
    await expect(confirmacion.locator('svg[aria-hidden="true"]')).toBeVisible();
    for (const tema of ["claro", "oscuro"]) {
      await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await expect(confirmacion).toHaveCSS("background-color", tema === "claro" ? "rgb(230, 244, 237)" : "rgb(17, 44, 34)");
      await confirmacion.screenshot({ path: `test-results/aviso-exito-${tema}.png` });
    }
    await page.reload();
    await expect(page.getByRole("textbox", { name: "Nombre del embudo" })).toHaveValue(nombre);
    await expect(page.getByText("Embudo creado.", { exact: true })).toHaveCount(0);
    ids.push(Number(new URL(page.url()).pathname.split("/").at(-1)));
    await page.getByRole("combobox", { name: "Estado del embudo" }).selectOption("true");
    await page.getByRole("button", { name: "Guardar embudo" }).click();
    await expect(page.getByRole("status")).toHaveText("Embudo guardado.");
  }
  const segundo = ids[1];
  await page.getByRole("button", { name: "Usar como predeterminado" }).click();
  await expect(page.getByRole("status")).toHaveText("Embudo predeterminado actualizado.");
  await page.getByRole("button", { name: "Editar Consulta recibida", exact: true }).click();
  await page.getByRole("textbox", { name: "Nombre de la etapa" }).fill("Recepción renombrada");
  await page.getByRole("button", { name: "Guardar etapa", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText("Etapa guardada.");
  await page.getByRole("button", { name: "Bajar Recepción renombrada" }).click();
  await expect(page.getByRole("status")).toHaveText("Orden de etapas guardado.");
  await page.reload();
  const etapas = await (await request.get(`${API}/stages?pipelineId=${segundo}`, { headers })).json();
  expect(etapas[1].name).toBe("Recepción renombrada");
  const propias = await (await request.get(`${API}/opportunities?pipelineId=${anterior.id}&size=1`, { headers })).json();
  const original = await (await request.get(`${API}/opportunities/${propias.content[0].id}`, { headers })).json();
  const tipoPropio = await (await request.post(`${API}/event-types`, { headers, data: { name: "Evento propio embudos E2E", active: true } })).json();
  const nueva = await request.post(`${API}/opportunities`, { headers, data: { title: "Oportunidad segundo proceso", pipelineId: segundo, stageId: etapas[1].id, companyId: original.company?.id, contactId: original.contact?.id, salesRepId: original.salesRepId, venueId: original.venue.id, eventTypeId: tipoPropio.id, eventStart: "2031-02-01T12:00:00Z", eventEnd: "2031-02-01T16:00:00Z", attendeeCount: 20 } });
  expect(nueva.status()).toBe(201); const oportunidad = await nueva.json();
  await page.goto("/app/embudo");
  await expect(page).toHaveURL(new RegExp(`pipelineId=${segundo}`));
  await expect(page.getByRole("heading", { name: "Proceso corporativo E2E", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Oportunidad segundo proceso", exact: true })).toBeVisible();
  await elegirSelector(page, "Embudo", String(anterior.id));
  await expect(page).toHaveURL(new RegExp(`pipelineId=${anterior.id}`));
  await page.reload();
  await expect(page.getByRole("combobox", { name: "Embudo", exact: true })).toHaveAttribute("data-valor", String(anterior.id));
  await page.goto(`/app/configuracion/embudos/${segundo}`);
  // Retain a historical stage while keeping another OPEN stage for activation.
  await page.getByRole("button", { name: "Editar Recepción renombrada", exact: true }).click();
  await page.getByRole("combobox", { name: "Estado de la etapa" }).selectOption("false");
  await page.getByRole("button", { name: "Guardar etapa", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText("Etapa guardada.");
  // A different active pipeline must be the default before deactivation.
  expect((await request.post(`${API}/pipelines/${anterior.id}/default`, { headers })).status()).toBe(200);
  await page.reload();
  await page.getByRole("combobox", { name: "Estado del embudo" }).selectOption("false");
  await page.getByRole("button", { name: "Guardar embudo" }).click();
  await expect(page.getByRole("status")).toHaveText("Embudo guardado.");
  await page.goto(`/app/embudo?pipelineId=${segundo}`);
  await expect(page.getByRole("heading", { name: "Recepción renombrada · Inactiva", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Oportunidad segundo proceso", exact: true })).toBeVisible();
  await page.goto(`/app/oportunidades/${oportunidad.id}/editar`);
  await expect(page.getByText("Proceso corporativo E2E · Inactivo", { exact: true })).toBeVisible();
  await page.getByRole("textbox", { name: "Observaciones" }).fill("Historia conservada desde un embudo inactivo");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Historia conservada desde un embudo inactivo", { exact: true })).toBeVisible();
  await page.goto("/app/oportunidades/nueva");
  await expect(page.getByRole("combobox", { name: "Embudo", exact: true })).toHaveAttribute("data-valor", String(anterior.id));
  await elegirSelector(page, "Embudo", String(ids[0]));
  await expect(page.getByRole("combobox", { name: "Etapa inicial" })).toHaveValue(/\d+/);
  await expect(page.getByRole("combobox", { name: "Etapa inicial" }).locator("option")).toHaveCount(3);
  const rutas = ["/app/configuracion/embudos", "/app/configuracion/embudos/nuevo", `/app/configuracion/embudos/${segundo}`, `/app/embudo?pipelineId=${segundo}`, `/app/oportunidades?pipelineId=${segundo}`, `/app/oportunidades/${oportunidad.id}`, `/app/oportunidades/${oportunidad.id}/editar`, "/app/oportunidades/nueva"];
  for (const ancho of [390, 1440]) {
    await page.setViewportSize({ width: ancho, height: 900 });
    for (const ruta of rutas) {
      await page.goto(ruta); await expect(page.locator("h1")).toBeVisible();
      await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      for (const tema of ["claro", "oscuro"]) { await page.evaluate(t => document.documentElement.dataset.tema = t, tema); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), ruta).toBe(true); }
      await page.keyboard.press("Tab"); expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
    }
    await page.screenshot({ path: `test-results/embudos-${ancho}.png`, fullPage: true });
  }
  expect(errores).toEqual([]); await contexto.close();
  for (const rol of ["manager", "seller"]) {
    const sesion = await (await request.post(`${API}/auth/login`, { data: { email: `${rol}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } })).json();
    const contextoRol = await browser.newContext();
    await contextoRol.addInitScript(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), sesion);
    const pagina = await contextoRol.newPage();
    for (const ruta of rutas.slice(0,3)) { await pagina.goto(ruta); await expect(pagina.getByRole("heading", { name: "Acceso denegado" })).toBeVisible(); }
    await pagina.goto(`/app/embudo?pipelineId=${segundo}`); await expect(pagina.getByRole("heading", { name: "Proceso corporativo E2E", exact: true })).toBeVisible();
    await contextoRol.close();
  }
});

test("Postman configura un embudo independiente, ordena y restituye el predeterminado", async ({ request }) => {
  const sesion = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const variables: Record<string, string> = { baseUrl: API, $timestamp: String(Date.now()) };
  const coleccion = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM.postman_collection.json", "utf8"));
  const carpeta = coleccion.item.find((c: { name: string }) => c.name === "Embudos y etapas");
  const sustituir = (texto: string) => texto.replace(/\{\{([\w$]+)\}\}/g, (_, clave: string) => { expect(variables[clave], clave).toBeDefined(); return variables[clave]; });
  let cantidad = 0;
  for (const item of carpeta.item) {
    const ruta = sustituir(item.request.url.raw);
    const respuesta = await request.fetch(ruta, { method: item.request.method, headers: { Authorization: `Bearer ${sesion.accessToken}` }, ...(item.request.body ? { data: JSON.parse(sustituir(item.request.body.raw)) } : {}) });
    expect(respuesta.status(), item.name).toBe(item.name === "Copiar plantilla corporativa" ? 201 : 200);
    const datos = await respuesta.json();
    for (const evento of item.event ?? []) if (evento.listen === "test") runInNewContext(evento.script.exec.join("\n"), { pm: { response: { json: () => datos }, collectionVariables: { set: (clave: string, valor: unknown) => { variables[clave] = String(valor); } } } }, { timeout: 1000 });
    cantidad++;
  }
  expect(cantidad).toBe(14);
  const runtime = await (await request.get("http://localhost:18080/v3/api-docs.yaml")).text();
  const exportado = readFileSync("../backend/docs/openapi.yaml", "utf8");
  for (const ruta of ["/api/v1/pipelines:", "/api/v1/pipelines/{id}:", "/api/v1/pipelines/{id}/default:", "/api/v1/pipelines/{id}/stages/reorder:", "/api/v1/pipeline-templates:", "/api/v1/stages/{id}:"]) { expect(runtime).toContain(ruta); expect(exportado).toContain(ruta); }
});
