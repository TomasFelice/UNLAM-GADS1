import { elegirSelector } from "./ayudas/selectores.js";
import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";

const API = "http://localhost:18080/api/v1";

test("requests mantenidos de Postman recorren lectura y administración de oferta", async ({ request }) => {
  const acceso = await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } });
  const datos = await acceso.json();
  const variables: Record<string, string> = { baseUrl: API };
  const coleccion = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM.postman_collection.json", "utf8"));
  let cantidad = 0;
  for (const carpeta of coleccion.item.filter((i: { name: string }) => ["Venues", "EventServices"].includes(i.name))) {
    for (const item of carpeta.item) {
      const ruta = item.request.url.raw.replace(/\{\{(\w+)\}\}/g, (_: string, clave: string) => variables[clave]);
      const respuesta = await request.fetch(ruta, { method: item.request.method, headers: { Authorization: `Bearer ${datos.accessToken}` },
        ...(item.request.body ? { data: JSON.parse(item.request.body.raw) } : {}) });
      expect(respuesta.status(), item.name).toBe(item.request.method === "POST" ? 201 : 200);
      const cuerpo = await respuesta.json();
      if (item.name === "Listar salones") variables.venueId = String(cuerpo.content[0].id);
      if (item.request.method === "POST") variables[carpeta.name === "Venues" ? "managedVenueId" : "managedServiceId"] = String(cuerpo.id);
      cantidad++;
    }
  }
  expect(cantidad).toBe(11);
});

test("oferta posterior a la primera página se busca y mantiene en formulario y filtros", async ({ page, request }) => {
  const acceso = await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } });
  expect(acceso.status()).toBe(200);
  const datos = await acceso.json();
  const headers = { Authorization: `Bearer ${datos.accessToken}` };
  let salonId = 0;
  for (let i = 0; i < 26; i++) {
    const nombre = i === 25 ? "ZZZ Salón posterior" : `AAA Salón paginado ${String(i).padStart(2, "0")}`;
    const salon = await request.post(`${API}/venues`, { headers, data: { name: nombre, capacity: 80, status: "DISPONIBLE" } });
    expect(salon.status()).toBe(201);
    if (i === 25) salonId = (await salon.json()).id;
    const servicio = await request.post(`${API}/event-services`, { headers, data: { name: i === 25 ? "ZZZ Servicio posterior" : `AAA Servicio paginado ${i}`, active: true } });
    expect(servicio.status()).toBe(201);
  }
  await page.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({
    token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false,
    usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role },
  })), datos);
  const errores: string[] = [];
  page.on("pageerror", e => errores.push(e.message));
  page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  const consultas: string[] = [];
  page.on("request", r => { if (/\/(venues|event-services)\?/.test(r.url())) consultas.push(r.url()); });
  await page.goto("/app/oportunidades/nueva");
  await expect(page.getByRole("option", { name: /ZZZ Salón posterior/ })).toHaveCount(0);
  await page.getByRole("combobox", { name: "Salón", exact: true }).click();
  await page.getByRole("button", { name: "Ver más resultados", exact: true }).click();
  await elegirSelector(page, "Salón", String(salonId));
  await page.getByRole("combobox", { name: "Salón", exact: true }).fill("AAA");
  await expect(page.getByRole("combobox", { name: "Salón", exact: true })).toHaveAttribute("data-valor", String(salonId));
  await page.keyboard.press("Escape");
  await expect(page.getByRole("combobox", { name: "Salón", exact: true })).toHaveValue(/ZZZ Salón posterior/);
  await page.getByRole("combobox", { name: "Servicios", exact: true }).fill("posterior");
  await page.getByRole("option", { name: "ZZZ Servicio posterior", exact: true }).click();
  await page.getByRole("combobox", { name: "Servicios", exact: true }).fill("AAA");
  await expect(page.getByRole("button", { name: "Quitar ZZZ Servicio posterior", exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await page.setViewportSize({ width: 390, height: 844 });
  for (const tema of ["claro", "oscuro"]) {
    await page.evaluate(tema => document.documentElement.dataset.tema = tema, tema);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: `test-results/oferta-formulario-${tema}-390.png`, fullPage: true });
  }
  for (const ruta of ["/app/oportunidades", "/app/embudo"]) {
    await page.goto(ruta);
    await page.getByRole("combobox", { name: "Salón", exact: true }).fill("posterior");
    await elegirSelector(page, "Salón", String(salonId));
    await expect(page).toHaveURL(new RegExp(`venueId=${salonId}`));
    await page.reload();
    await expect(page.getByRole("combobox", { name: "Salón", exact: true })).toHaveAttribute("data-valor", String(salonId));
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
  await page.goto("/app/salones");
  await page.getByRole("searchbox", { name: "Buscar salones" }).fill("posterior");
  await expect(page.getByRole("heading", { name: "ZZZ Salón posterior" })).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
  expect(consultas.length).toBeGreaterThan(0);
  expect(consultas.every(ruta => new URL(ruta).searchParams.get("size") === "20")).toBe(true);
  expect(errores).toEqual([]);
});

test("administrador gestiona oferta, conserva referencias históricas y roles comerciales sólo consultan", async ({ browser, request }) => {
  test.setTimeout(60000);
  const acceso = await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } });
  const datos = await acceso.json();
  const headers = { Authorization: `Bearer ${datos.accessToken}` };
  const contexto = await browser.newContext({ viewport: { width: 390, height: 844 } });
  await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), datos);
  const page = await contexto.newPage();
  const errores: string[] = [];
  page.on("pageerror", e => errores.push(e.message));
  page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  await page.goto("/app/salones/nuevo");
  await page.getByRole("textbox", { name: "Nombre", exact: true }).fill("Salón ABM histórico");
  await page.getByRole("spinbutton", { name: "Capacidad", exact: true }).fill("40");
  await page.getByLabel("Tarifa por jornada", { exact: true }).fill("15000.50");
  await page.getByLabel("Localidad", { exact: true }).fill("San Justo");
  await page.getByLabel("Equipamiento", { exact: true }).fill("Proyector\nAudio");
  await page.getByRole("button", { name: "Crear salón", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Salón ABM histórico", exact: true })).toBeVisible();
  const rutaSalon = new URL(page.url()).pathname;
  const salonId = Number(rutaSalon.split("/").at(-1));
  await page.goto("/app/servicios/nuevo");
  await page.getByRole("textbox", { name: "Nombre", exact: true }).fill("Servicio ABM histórico");
  await page.getByRole("textbox", { name: "Descripción", exact: true }).fill("Servicio complementario de prueba");
  await page.getByRole("button", { name: "Crear servicio", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Servicio ABM histórico", exact: true })).toBeVisible();
  const rutaServicio = new URL(page.url()).pathname;
  const servicioId = Number(rutaServicio.split("/").at(-1));
  const lista = await (await request.get(`${API}/opportunities?size=100`, { headers })).json();
  const oportunidadId = lista.content.find((o: { status: string }) => o.status === "ABIERTA").id;
  const oportunidad = await (await request.get(`${API}/opportunities/${oportunidadId}`, { headers })).json();
  const respuesta = await request.put(`${API}/opportunities/${oportunidadId}`, { headers, data: {
    version: oportunidad.version, title: oportunidad.title, companyId: oportunidad.company?.id, contactId: oportunidad.contact?.id, venueId: salonId, eventTypeId: oportunidad.eventTypeId,
    eventStart: oportunidad.eventStart, eventEnd: oportunidad.eventEnd, attendeeCount: 20, serviceIds: [servicioId],
  } });
  expect(respuesta.status()).toBe(200);
  await page.goto(`${rutaSalon}/editar`);
  await expect(page.getByRole("combobox", { name: "Estado", exact: true })).toHaveValue("DISPONIBLE");
  await page.getByRole("combobox", { name: "Estado", exact: true }).selectOption("MANTENIMIENTO");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("En mantenimiento", { exact: true })).toBeVisible();
  await page.goto(`${rutaServicio}/editar`);
  await page.getByRole("combobox", { name: "Estado", exact: true }).selectOption("false");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Inactivo", { exact: true })).toBeVisible();
  await page.goto(`/app/oportunidades/${oportunidadId}/editar`);
  await expect(page.getByRole("combobox", { name: "Salón", exact: true })).toHaveValue(/Salón ABM histórico.*Asociación anterior/);
  await expect(page.getByRole("button", { name: /Quitar Servicio ABM histórico.*Inactivo/ })).toBeVisible();
  await page.getByLabel("Observaciones", { exact: true }).fill("Actualizada sin perder oferta histórica");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Actualizada sin perder oferta histórica", { exact: true })).toBeVisible();
  await expect(page.getByText("Servicio ABM histórico · Inactivo", { exact: true })).toBeVisible();
  const guardada = await (await request.get(`${API}/opportunities/${oportunidadId}`, { headers })).json();
  expect(guardada.venue.id).toBe(salonId);
  expect(guardada.serviceIds).toEqual([servicioId]);
  for (const ruta of ["/app/salones", "/app/salones/nuevo", rutaSalon, `${rutaSalon}/editar`, "/app/servicios", "/app/servicios/nuevo", rutaServicio, `${rutaServicio}/editar`]) {
    await page.goto(ruta);
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
    for (const tema of ["claro", "oscuro"]) {
      await page.evaluate(tema => document.documentElement.dataset.tema = tema, tema);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    }
    await page.keyboard.press("Tab");
    expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
  }
  expect(errores).toEqual([]);
  await contexto.close();
  for (const nombre of ["manager", "seller"]) {
    const login = await (await request.post(`${API}/auth/login`, { data: { email: `${nombre}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } })).json();
    const comercial = await browser.newContext();
    await comercial.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), login);
    const pagina = await comercial.newPage();
    for (const ruta of [rutaSalon, rutaServicio]) {
      await pagina.goto(ruta);
      await expect(pagina.locator("h1")).toBeVisible();
      await expect(pagina.getByRole("link", { name: /^Editar (salón|servicio)$/ })).toHaveCount(0);
      await pagina.goto(`${ruta}/editar`);
      await expect(pagina.getByRole("heading", { name: "Acceso denegado" })).toBeVisible();
    }
    await comercial.close();
  }
});
