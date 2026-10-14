import { elegirSelector } from "./ayudas/selectores.js";
import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";

const API = "http://localhost:18080/api/v1";
const catalogos = [
  { clave: "origenes", recurso: "origins", singular: "origen" },
  { clave: "motivos-perdida", recurso: "loss-reasons", singular: "motivo de pérdida" },
  { clave: "tipos-actividad", recurso: "activity-types", singular: "tipo de actividad" },
  { clave: "tipos-evento", recurso: "event-types", singular: "tipo de evento" },
];

test("ADMIN administra los cuatro catálogos y los otros roles tienen acceso denegado", async ({ browser, request }) => {
  test.setTimeout(120000);
  const errores: string[] = [];
  for (const rol of ["admin", "manager", "seller"]) {
    const acceso = await request.post(`${API}/auth/login`, { data: { email: `${rol}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } });
    expect(acceso.status()).toBe(200);
    const datos = await acceso.json();
    const contexto = await browser.newContext({ viewport: { width: 390, height: 844 } });
    await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), datos);
    const page = await contexto.newPage();
    page.on("pageerror", e => errores.push(e.message));
    page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
    await page.goto("/app/configuracion");
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
    if (rol !== "admin") {
      await expect(page.getByRole("heading", { name: "Acceso denegado" })).toBeVisible();
      await expect(page.getByRole("link", { name: "Configuración comercial", exact: true })).toHaveCount(0);
      for (const c of catalogos) for (const sufijo of ["", "/nuevo", "/1", "/1/editar"]) {
        await page.goto(`/app/configuracion/${c.clave}${sufijo}`);
        await expect(page.getByRole("heading", { name: "Acceso denegado" })).toBeVisible();
      }
      await contexto.close();
      continue;
    }
    const rutas = ["/app/configuracion"];
    for (const c of catalogos) {
      const ruta = `/app/configuracion/${c.clave}`;
      const nombre = `Catálogo E2E ${c.singular}`;
      await page.goto(`${ruta}/nuevo`);
      await page.getByRole("textbox", { name: "Nombre", exact: true }).fill(nombre);
      await page.getByRole("button", { name: `Crear ${c.singular}`, exact: true }).click();
      await expect(page.getByRole("heading", { name: nombre, exact: true })).toBeVisible();
      const detalle = new URL(page.url()).pathname;
      await page.goto(`${detalle}/editar`);
      await expect(page.getByRole("textbox", { name: "Nombre", exact: true })).toHaveValue(nombre);
      await page.getByRole("textbox", { name: "Nombre", exact: true }).fill(`${nombre} renombrado`);
      await page.getByRole("combobox", { name: "Estado", exact: true }).selectOption("false");
      await page.getByRole("button", { name: "Guardar cambios" }).click();
      await expect(page.getByText("Inactivo", { exact: true })).toBeVisible();
      await page.reload();
      await expect(page.getByRole("heading", { name: `${nombre} renombrado`, exact: true })).toBeVisible();
      await page.goto(ruta);
      await page.getByRole("searchbox", { name: `Buscar ${c.singular}` }).fill("renombrado");
      await page.getByRole("combobox", { name: "Estado" }).selectOption("false");
      await expect(page.getByRole("link", { name: `${nombre} renombrado`, exact: true })).toBeVisible();
      await expect(page).toHaveURL(/active=false/);
      await expect(page).toHaveURL(/q=renombrado/);
      await page.reload();
      await expect(page.getByRole("searchbox")).toHaveValue("renombrado");
      rutas.push(ruta, `${ruta}/nuevo`, detalle, `${detalle}/editar`);
    }
    for (const ancho of [390, 1440]) {
      await page.setViewportSize({ width: ancho, height: 900 });
      for (const ruta of rutas) {
        await page.goto(ruta);
        await expect(page.locator("h1")).toBeVisible();
        await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
        for (const tema of ["claro", "oscuro"]) {
          await page.evaluate(tema => document.documentElement.dataset.tema = tema, tema);
          expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), ruta).toBe(true);
        }
        await page.keyboard.press("Tab");
        expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
      }
      await page.screenshot({ path: `test-results/catalogos-${ancho}.png`, fullPage: true });
    }
    await contexto.close();
  }
  expect(errores).toEqual([]);
});

test("catálogos posteriores a página cero se seleccionan y conservan en clientes y oportunidades", async ({ page, request }) => {
  test.setTimeout(60000);
  const datos = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const headers = { Authorization: `Bearer ${datos.accessToken}` };
  const ids: Record<string, number> = {};
  for (const recurso of ["origins", "event-types"]) for (let i = 0; i < 26; i++) {
    const respuesta = await request.post(`${API}/${recurso}`, { headers, data: { name: i === 25 ? `ZZZ Posterior ${recurso}` : `AAA Paginado ${recurso} ${String(i).padStart(2, "0")}`, active: true } });
    expect(respuesta.status()).toBe(201);
    if (i === 25) ids[recurso] = (await respuesta.json()).id;
  }
  await page.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), datos);
  const consultas: string[] = [];
  page.on("request", r => { if (/\/(origins|event-types)\?/.test(r.url())) consultas.push(r.url()); });
  const errores: string[] = [];
  page.on("pageerror", e => errores.push(e.message));
  page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  await page.goto("/app/empresas/nueva");
  await page.getByRole("textbox", { name: "Razón social" }).fill("Empresa catálogo paginado");
  await page.getByRole("textbox", { name: "Nombre comercial" }).fill("Empresa catálogo paginado");
  await page.getByRole("combobox", { name: "Origen", exact: true }).fill("Posterior");
  await elegirSelector(page, "Origen", String(ids.origins));
  await page.getByRole("button", { name: "Crear empresa" }).click();
  await expect(page.getByText("ZZZ Posterior origins", { exact: true })).toBeVisible();
  const empresa = new URL(page.url()).pathname;
  await request.put(`${API}/origins/${ids.origins}`, { headers, data: { name: "ZZZ Posterior origins", active: false } });
  await page.goto(`${empresa}/editar`);
  await expect(page.getByRole("combobox", { name: "Origen", exact: true })).toHaveValue("ZZZ Posterior origins (inactivo)");
  await page.getByLabel("Observaciones", { exact: true }).fill("Historia intacta");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Historia intacta", { exact: true })).toBeVisible();
  const lista = await (await request.get(`${API}/opportunities`, { headers })).json();
  const oportunidadId = lista.content.find((o: { status: string }) => o.status === "ABIERTA").id;
  await page.goto(`/app/oportunidades/${oportunidadId}/editar`);
  await page.getByRole("combobox", { name: "Tipo de evento", exact: true }).fill("Posterior");
  await elegirSelector(page, "Tipo de evento", String(ids["event-types"]));
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("ZZZ Posterior event-types", { exact: true })).toBeVisible();
  await request.put(`${API}/event-types/${ids["event-types"]}`, { headers, data: { name: "ZZZ Posterior event-types", active: false } });
  await page.goto(`/app/oportunidades/${oportunidadId}/editar`);
  await expect(page.getByRole("combobox", { name: "Tipo de evento", exact: true })).toHaveValue("ZZZ Posterior event-types (inactivo)");
  await page.getByLabel("Observaciones", { exact: true }).fill("Tipo histórico conservado");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Tipo histórico conservado", { exact: true })).toBeVisible();
  for (const ruta of [empresa, `${empresa}/editar`, `/app/oportunidades/${oportunidadId}`, `/app/oportunidades/${oportunidadId}/editar`, "/app/contactos/nuevo", "/app/contactos", "/app/empresas", "/app/oportunidades", "/app/embudo"]) {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(ruta);
    await expect(page.locator("h1")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), ruta).toBe(true);
  }
  expect(consultas.length).toBeGreaterThan(0);
  expect(consultas.every(r => new URL(r).searchParams.get("size") === "20")).toBe(true);
  expect(errores).toEqual([]);
});

test("la colección Postman recorre los cuatro ABM y conserva el export runtime", async ({ request }) => {
  const datos = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const variables: Record<string, string> = { baseUrl: API };
  const coleccion = JSON.parse(readFileSync("../backend/docs/postman/zTech-CRM.postman_collection.json", "utf8"));
  let cantidad = 0;
  for (const carpeta of coleccion.item.filter((c: { name: string }) => c.name.startsWith("Catalog "))) for (const item of carpeta.item) {
    const ruta = item.request.url.raw.replace(/\{\{(\w+)\}\}/g, (_: string, clave: string) => variables[clave]);
    const respuesta = await request.fetch(ruta, { method: item.request.method, headers: { Authorization: `Bearer ${datos.accessToken}` }, ...(item.request.body ? { data: JSON.parse(item.request.body.raw) } : {}) });
    expect(respuesta.status(), item.name).toBe(item.request.method === "POST" ? 201 : 200);
    const cuerpo = await respuesta.json();
    if (item.request.method === "POST") variables.managedCatalogId = String(cuerpo.id);
    cantidad++;
  }
  expect(cantidad).toBe(16);
  const runtime = await (await request.get("http://localhost:18080/v3/api-docs.yaml")).text();
  const exportado = readFileSync("../backend/docs/openapi.yaml", "utf8");
  for (const c of catalogos) {
    expect(runtime).toContain(`/api/v1/${c.recurso}/{id}:`);
    expect(exportado).toContain(`/api/v1/${c.recurso}/{id}:`);
  }
});
