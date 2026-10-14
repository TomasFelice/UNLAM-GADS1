import { elegirSelector } from "./ayudas/selectores.js";
import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";

const API = "http://localhost:18080/api/v1";
test("alta contextual e históricos completos con tres roles, páginas y bajas", async ({ page, request }) => {
  test.setTimeout(180000);
  const acceso = async (nombre: string) => (await request.post(`${API}/auth/login`, { data: { email: `${nombre}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } })).json();
  const admin = await acceso("admin"); const headers = { Authorization: `Bearer ${admin.accessToken}` };
  const vendedor = await acceso("seller");
  const post = async (ruta: string, data: object) => { const r = await request.post(`${API}/${ruta}`, { headers, data }); expect(r.status(), ruta + await r.text()).toBe(201); return r.json(); };
  const empresa = await post("companies", { legalName: "Historia contextual", businessName: "Historia contextual", status: "POTENCIAL", salesRepId: vendedor.user.id });
  const contacto = await post("contacts", { firstName: "Ana", lastName: "Historia contextual", status: "POTENCIAL", companyId: empresa.id, salesRepId: vendedor.user.id });
  const tipo = await post("activity-types", { name: "Interacción historial E2E", active: true });
  const evento = await post("event-types", { name: "Evento historial E2E", active: true });
  const embudo = await (await request.get(`${API}/pipelines/default`, { headers })).json();
  const etapas = await (await request.get(`${API}/stages?pipelineId=${embudo.id}`, { headers })).json();
  const salon = (await (await request.get(`${API}/venues?status=DISPONIBLE`, { headers })).json()).content[0];
  const oportunidad = await post("opportunities", { title: "Oportunidad historial E2E", companyId: empresa.id, contactId: contacto.id, salesRepId: vendedor.user.id, pipelineId: embudo.id, stageId: etapas.find((e: any) => e.active && e.kind === "OPEN").id, venueId: salon.id, eventTypeId: evento.id, eventStart: "2037-04-01T12:00:00Z", eventEnd: "2037-04-01T16:00:00Z", attendeeCount: 5 });
  const motivo = await post("loss-reasons", { name: "Sin presupuesto historial E2E", active: true });
  expect((await request.post(`${API}/opportunities/${oportunidad.id}/lose`, { headers, data: { stageId: etapas.find((e: any) => e.active && e.kind === "LOST").id, lossReasonId: motivo.id, version: 0 } })).status()).toBe(200);
  const rutas = [`/app/empresas/${empresa.id}`, `/app/contactos/${contacto.id}`, `/app/oportunidades/${oportunidad.id}`];
  const errores: string[] = []; page.on("pageerror", e => errores.push(e.message));
  page.on("console", m => { if (m.type() === "error") errores.push(m.text()); });
  await page.goto("/ingresar");
  for (const rol of ["admin", "manager", "seller"]) {
    const datos = await acceso(rol);
    await page.evaluate(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), datos);
    for (const [indice, ruta] of rutas.entries()) {
      await page.goto(ruta); const historial = page.getByRole("region", { name: "Historial comercial", exact: true });
      await historial.getByRole("button", { name: "Registrar actividad", exact: true }).click();
      const formulario = page.getByRole("form", { name: "Registrar actividad" });
      await formulario.getByRole("combobox", { name: "Tipo de actividad" }).fill("Interacción historial E2E");
      await elegirSelector(page, "Tipo de actividad", String(tipo.id));
      await formulario.getByLabel("Fecha y hora del hecho").fill("2026-01-01T09:00");
      await formulario.getByRole("textbox", { name: "Descripción" }).fill(`Hecho ${rol} ${indice}`);
      await formulario.getByRole("textbox", { name: "Resultado" }).fill("Cliente informado");
      for (const width of [390, 1440]) for (const tema of ["claro", "oscuro"]) {
        await page.setViewportSize({ width, height: 900 });
        await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
        await formulario.getByRole("textbox", { name: "Descripción" }).focus(); await page.keyboard.press("Tab");
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${rol} ${ruta} ${tema}`).toBe(true);
        await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      }
      if (rol === "seller" && indice === 2) {
        await page.screenshot({ path: test.info().outputPath("actividad-escritorio-oscuro.png"), fullPage: true });
        await page.setViewportSize({ width: 390, height: 900 });
        await expect(page.getByRole("navigation", { name: "Navegación de la aplicación" })).not.toBeInViewport();
        await page.screenshot({ path: test.info().outputPath("actividad-movil-oscuro.png"), fullPage: true });
      }
      await formulario.getByRole("button", { name: "Guardar actividad" }).click();
      await expect(historial.getByRole("status")).toHaveText("Actividad registrada.");
      await expect(historial.getByText(`Hecho ${rol} ${indice}`, { exact: false })).toHaveCount(1);
      await expect(historial.getByRole("button", { name: "Registrar actividad" })).toBeFocused();
    }
  }
  // La misma interacción de oportunidad está en sus tres historias, con un único ID.
  for (const ruta of rutas) { await page.goto(ruta); await expect(page.getByText("Hecho seller 2", { exact: false })).toHaveCount(1); }
  const hecho = { typeId: tipo.id, occurredAt: "2026-02-01T12:00:00Z", description: "Hecho paginado", result: "Confirmado", contactId: contacto.id };
  for (let i = 0; i < 24; i++) await post("activities", { ...hecho, description: `Hecho paginado ${i}` });
  for (let i = 0; i < 25; i++) {
    await post("contacts", { firstName: `Persona paginada ${i}`, lastName: "Histórica", companyId: empresa.id, status: "INACTIVO", salesRepId: vendedor.user.id });
    await post("opportunities", { title: `Relación histórica ${i}`, companyId: empresa.id, contactId: contacto.id, salesRepId: vendedor.user.id, pipelineId: embudo.id, stageId: etapas.find((e: any) => e.active && e.kind === "OPEN").id, venueId: salon.id, eventTypeId: evento.id, eventStart: "2037-04-01T12:00:00Z", eventEnd: "2037-04-01T16:00:00Z", attendeeCount: 5 });
  }
  await request.put(`${API}/companies/${empresa.id}`, { headers, data: { legalName: empresa.legalName, businessName: empresa.businessName, status: "INACTIVO", salesRepId: vendedor.user.id } });
  await request.put(`${API}/contacts/${contacto.id}`, { headers, data: { firstName: contacto.firstName, lastName: contacto.lastName, companyId: empresa.id, status: "NO_CONTACTAR", salesRepId: vendedor.user.id } });
  await request.put(`${API}/activity-types/${tipo.id}`, { headers, data: { name: tipo.name, active: false } });
  await page.goto(rutas[1]); const historial = page.getByRole("region", { name: "Historial comercial", exact: true });
  await expect(historial.getByText("Mostrando 20 de 30 actividades")).toBeVisible();
  await historial.getByRole("button", { name: "Página siguiente" }).click();
  await expect(historial.getByText("Mostrando 10 de 30 actividades")).toBeVisible();
  await expect(historial.getByText("Hecho seller 2", { exact: false })).toHaveCount(1);
  await expect(historial.getByRole("button", { name: /Editar actividad|Eliminar actividad/ })).toHaveCount(0);
  for (const ruta of rutas.slice(0, 2)) {
    await page.goto(ruta); const relaciones = page.getByRole("region", { name: "Oportunidades del cliente" });
    await expect(relaciones.getByText("Mostrando 20 de 26 oportunidades")).toBeVisible();
    await relaciones.getByRole("button", { name: "Página siguiente" }).click();
    await expect(relaciones.getByText("Mostrando 6 de 26 oportunidades")).toBeVisible();
    await expect(relaciones.getByRole("link", { name: "Relación histórica 24", exact: true })).toBeVisible();
    if (ruta === rutas[0]) {
      const personas = page.getByRole("region", { name: "Contactos de la empresa" });
      await expect(personas.getByText("Mostrando 20 de 26 contactos")).toBeVisible();
      await personas.getByRole("button", { name: "Página siguiente" }).click();
      await expect(personas.getByText("Mostrando 6 de 26 contactos")).toBeVisible();
    }
  }
  // Permiso de lectura indirecta: empresa visible por oportunidad propia, sin alta general.
  const compartida = await post("companies", { legalName: "Consulta indirecta historial", businessName: "Consulta indirecta historial", status: "POTENCIAL", salesRepId: admin.user.id });
  await post("opportunities", { title: "Acceso indirecto historial", companyId: compartida.id, salesRepId: vendedor.user.id, pipelineId: embudo.id, stageId: etapas.find((e: any) => e.active && e.kind === "OPEN").id, venueId: salon.id, eventTypeId: evento.id, eventStart: "2038-04-01T12:00:00Z", eventEnd: "2038-04-01T16:00:00Z", attendeeCount: 5 });
  await page.goto(`/app/empresas/${compartida.id}`);
  await expect(page.getByRole("heading", { name: "Consulta indirecta historial" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Registrar actividad" })).toHaveCount(0);
  expect(errores).toEqual([]);
});

test("colección Postman registra un hecho y consulta las tres asociaciones sin cambiar IDs a mano", async ({ request }) => {
  const acceso = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const coleccion = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM.postman_collection.json", "utf8"));
  const carpeta = coleccion.item.find((c: any) => c.name === "Registro de actividades e historial");
  const variables: Record<string, string> = { baseUrl: API };
  const sustituir = (texto: string) => texto.replace(/\{\{(\w+)\}\}/g, (_, clave: string) => { expect(variables[clave], clave).toBeDefined(); return variables[clave]; });
  for (const item of carpeta.item) {
    const r = await request.fetch(sustituir(item.request.url.raw), { method: item.request.method, headers: { Authorization: `Bearer ${acceso.accessToken}` }, ...(item.request.body ? { data: JSON.parse(sustituir(item.request.body.raw)) } : {}) });
    expect(r.status(), item.name).toBe(item.request.method === "POST" ? 201 : 200);
    const datos = await r.json();
    for (const evento of item.event ?? []) runInNewContext(evento.script.exec.join("\n"), { pm: { response: { json: () => datos }, collectionVariables: { set: (k: string, v: unknown) => { variables[k] = String(v); } } } }, { timeout: 1000 });
    if (item.request.method === "POST") {
      expect(datos.companyId).toBe(Number(variables.activityCompanyId));
      expect(datos.contactId).toBe(Number(variables.activityContactId));
      expect(datos.author.id).toBe(acceso.user.id);
    }
  }
  expect(carpeta.item).toHaveLength(8);
});
