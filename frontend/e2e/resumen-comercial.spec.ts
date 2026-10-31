import { expect, test } from "@playwright/test";
import { elegirSelector } from "./ayudas/selectores.js";

const API = "http://localhost:18080/api/v1";

test("resumen automático sin metadatos ni fuentes, retorno sin duplicados y actividad con tres roles", async ({ page, request }) => {
  test.setTimeout(180000);
  const acceso = async (rol: string) => (await request.post(`${API}/auth/login`, { data: { email: `${rol}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } })).json();
  const admin = await acceso("admin"); const vendedor = await acceso("seller");
  const headers = { Authorization: `Bearer ${admin.accessToken}` };
  const post = async (ruta: string, data: object) => { const respuesta = await request.post(`${API}/${ruta}`, { headers, data }); expect(respuesta.status(), await respuesta.text()).toBe(201); return respuesta.json(); };
  const empresa = await post("companies", { legalName: "Empresa ficticia IA E2E", businessName: "Empresa ficticia IA E2E", status: "POTENCIAL", salesRepId: vendedor.user.id });
  const tipo = await post("activity-types", { name: "Interacción IA E2E", active: true });
  const evento = await post("event-types", { name: "Capacitación IA E2E", active: true });
  const embudo = await (await request.get(`${API}/pipelines/default`, { headers })).json();
  const etapas = await (await request.get(`${API}/stages?pipelineId=${embudo.id}`, { headers })).json();
  const salon = (await (await request.get(`${API}/venues?status=DISPONIBLE`, { headers })).json()).content[0];
  const oportunidad = await post("opportunities", { title: "IA E2E automática", companyId: empresa.id, salesRepId: vendedor.user.id,
    pipelineId: embudo.id, stageId: etapas.find((e: { active: boolean; kind: string }) => e.active && e.kind === "OPEN").id,
    venueId: salon.id, eventTypeId: evento.id, eventStart: "2045-02-01T12:00:00Z", eventEnd: "2045-02-01T16:00:00Z", attendeeCount: 5 });
  const errores: string[] = [];
  page.on("pageerror", e => errores.push(e.message)); page.on("console", m => { if (m.type() === "error") errores.push(m.text()); });
  await page.goto("/ingresar");
  for (const rol of ["admin", "manager", "seller"]) {
    await request.post("http://localhost:18080/__tests/summary?mode=success");
    const datos = await acceso(rol);
    await page.evaluate(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000,
      debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), datos);
    await page.goto(`/app/oportunidades/${oportunidad.id}`);
    const resumen = page.getByRole("region", { name: "Resumen comercial", exact: true });
    await expect(resumen.getByText("Síntesis ficticia de los hechos registrados")).toBeVisible();
    const llamadas = async () => (await (await request.get("http://localhost:18080/__tests/summary")).json()).calls;
    const iniciales = rol === "admin" ? 1 : 0;
    expect(await llamadas()).toBe(iniciales);
    await page.reload();
    await expect(resumen.getByText("Síntesis ficticia de los hechos registrados")).toBeVisible();
    expect(await llamadas()).toBe(iniciales);
    await expect(resumen.getByText("Generado con IA. Revisá la información y sus fuentes.")).toBeVisible();
    await expect(resumen.locator("details, summary")).toHaveCount(0);
    await expect(resumen.getByText(/Contexto al|Generado el|actividades y|Cambios:|Actividades:/)).toHaveCount(0);
    for (const width of [390, 1440]) for (const tema of ["claro", "oscuro"]) {
      await page.setViewportSize({ width, height: 900 }); await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      if (rol === "seller" && tema === "claro") await resumen.screenshot({ path: `test-results/resumen-${width}.png` });
    }
    await page.getByRole("link", { name: "Oportunidades", exact: true }).first().click(); await expect(page).toHaveURL(/\/app\/oportunidades$/);
    await page.goBack(); await expect(resumen.getByText("Síntesis ficticia de los hechos registrados")).toBeVisible(); expect(await llamadas()).toBe(iniciales);
    const historial = page.getByRole("region", { name: "Historial comercial", exact: true });
    await historial.getByRole("button", { name: "Registrar actividad", exact: true }).click();
    const formulario = page.getByRole("form", { name: "Registrar actividad" });
    await formulario.getByRole("combobox", { name: "Tipo de actividad" }).fill("Interacción IA E2E"); await elegirSelector(page, "Tipo de actividad", String(tipo.id));
    await formulario.getByLabel("Fecha y hora del hecho").fill("2026-01-01T09:00");
    await formulario.getByRole("textbox", { name: "Descripción" }).fill(`Acuerdo ficticio ${rol}`);
    await formulario.getByRole("textbox", { name: "Resultado" }).fill("Pantalla confirmada, sin precio acordado");
    await formulario.getByRole("button", { name: /Registrar|Guardar/ }).click();
    await expect(resumen.getByText("Síntesis ficticia de los hechos registrados")).toBeVisible();
    await expect.poll(llamadas).toBe(iniciales + 1);
    await expect(resumen.locator("details, summary")).toHaveCount(0);
  }
  await page.goto("/privacidad");
  await expect(page).toHaveTitle(/privacidad/i);
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", /\S/);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/privacidad$/);
  await expect(page.getByText(/El último resumen se guarda con sus fuentes y fechas/)).toBeVisible();
  await page.setViewportSize({ width: 390, height: 900 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errores).toEqual([]);
});

test("errores aislados, reintento manual, cuota, timeout y función apagada", async ({ page, request }) => {
  test.setTimeout(120000);
  const datos = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const headers = { Authorization: `Bearer ${datos.accessToken}` };
  const lista = await (await request.get(`${API}/opportunities?size=1`, { headers })).json(); const id = lista.content[0].id;
  await page.goto("/ingresar");
  await page.evaluate(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000,
    debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), datos);
  const resumen = page.getByRole("region", { name: "Resumen comercial", exact: true });
  for (const [modo, mensaje] of [["unavailable", "El servicio de resumen no está disponible"], ["quota", "La cuota del proveedor"],
    ["timeout", "El resumen tardó demasiado"], ["disabled", "El resumen con IA no está habilitado"]]) {
    const actividad = await request.post(`${API}/activities`, { headers, data: { opportunityId: id, typeId: (await (await request.get(`${API}/activity-types?size=1`, { headers })).json()).content[0].id, occurredAt: "2026-01-01T12:00:00Z", description: `Contexto ${modo}`, result: "Hecho ficticio" } });
    expect(actividad.status()).toBe(201);
    await request.post(`http://localhost:18080/__tests/summary?mode=${modo}`); await page.goto(`/app/oportunidades/${id}`);
    await expect(resumen.getByText(new RegExp(mensaje))).toBeVisible({ timeout: 25000 });
    await expect(page).toHaveURL(new RegExp(`/app/oportunidades/${id}$`));
    if (modo === "disabled") await expect(resumen.getByRole("button", { name: "Reintentar" })).toHaveCount(0);
    else {
      await request.post("http://localhost:18080/__tests/summary?mode=success");
      await resumen.getByRole("button", { name: "Reintentar" }).dblclick();
      await expect(resumen.getByText("Síntesis ficticia de los hechos registrados")).toBeVisible();
      expect((await (await request.get("http://localhost:18080/__tests/summary")).json()).calls).toBe(1);
    }
  }
  await request.post("http://localhost:18080/__tests/summary?mode=success");
});
