import { expect, test, type APIRequestContext, type Browser, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";

const API = "http://localhost:18080/api/v1";

async function abrirSesion(browser: Browser, request: APIRequestContext, nombre: string) {
  const respuesta = await request.post(`${API}/auth/login`, {
    data: { email: `${nombre}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" },
  });
  expect(respuesta.status()).toBe(200);
  const datos = await respuesta.json();
  const contexto = await browser.newContext();
  await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({
    token: datos.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false,
    usuario: { id: datos.user.id, nombre: datos.user.firstName, apellido: datos.user.lastName,
      email: datos.user.email, rol: datos.user.role },
  })), datos);
  return { contexto, pagina: await contexto.newPage(), cabeceras: { Authorization: `Bearer ${datos.accessToken}` } };
}

async function comprobarPantalla(pagina: Page) {
  await expect(pagina.locator("h1")).toBeVisible();
  await expect(pagina.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
  expect(await pagina.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), pagina.url()).toBe(true);
  await pagina.keyboard.press("Tab");
  expect(await pagina.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
}

test("origen de empresa, herencia del contacto y reasignación comercial persisten tras recargar", async ({ browser, request }) => {
  const admin = await abrirSesion(browser, request, "admin");
  const pagina = admin.pagina;
  const errores: string[] = [];
  pagina.on("pageerror", error => errores.push(error.message));
  pagina.on("console", mensaje => { if (mensaje.type() === "error") errores.push(mensaje.text()); });
  await pagina.goto("http://localhost:14173/app/empresas/nueva");
  await pagina.getByLabel("Razón social").fill("AAA Empresa E2E SA");
  await pagina.getByLabel("Nombre comercial").fill("AAA Empresa E2E");
  await pagina.getByRole("combobox", { name: "Origen", exact: true }).fill("Referido");
  await pagina.getByRole("option", { name: "Referido", exact: true }).click();
  await pagina.getByLabel("Responsable comercial", { exact: true }).selectOption({ label: "other Fixture" });
  await expect(pagina.getByText(/backend/)).toHaveCount(0);
  await pagina.getByRole("button", { name: "Crear empresa" }).click();
  await expect(pagina.getByRole("heading", { name: "AAA Empresa E2E", exact: true })).toBeVisible();
  const rutaEmpresa = new URL(pagina.url()).pathname;
  await pagina.reload();
  await expect(pagina.getByText("Referido", { exact: true })).toBeVisible();
  await expect(pagina.getByText("other Fixture", { exact: true })).toBeVisible();
  await pagina.getByRole("link", { name: "Nuevo contacto", exact: true }).click();
  await pagina.getByRole("textbox", { name: "Nombre", exact: true }).fill("Ana E2E");
  await pagina.getByRole("textbox", { name: "Apellido", exact: true }).fill("Herencia");
  await expect(pagina.getByRole("combobox", { name: "Empresa", exact: true })).toHaveAttribute("data-valor", rutaEmpresa.split("/").at(-1)!);
  await pagina.getByRole("button", { name: "Crear contacto" }).click();
  await expect(pagina.getByRole("heading", { name: "Ana E2E Herencia" })).toBeVisible();
  const rutaContacto = new URL(pagina.url()).pathname;
  await pagina.reload();
  await expect(pagina.getByText("other Fixture", { exact: true })).toBeVisible();

  const manager = await abrirSesion(browser, request, "manager");
  await manager.pagina.goto(`http://localhost:14173${rutaEmpresa}/editar`);
  await manager.pagina.getByLabel("Responsable comercial", { exact: true }).selectOption({ label: "seller Fixture" });
  await manager.pagina.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(manager.pagina.getByRole("main").getByText("seller Fixture", { exact: true })).toBeVisible();
  await manager.pagina.goto(`http://localhost:14173${rutaContacto}/editar`);
  await manager.pagina.getByLabel("Responsable", { exact: true }).selectOption({ label: "seller Fixture" });
  await manager.pagina.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(manager.pagina.getByRole("main").getByText("seller Fixture", { exact: true })).toBeVisible();
  const vendedor = await abrirSesion(browser, request, "seller");
  for (const ruta of [rutaEmpresa, rutaContacto]) {
    await vendedor.pagina.goto(`http://localhost:14173${ruta}`);
    await expect(vendedor.pagina.getByRole("main").getByText("seller Fixture", { exact: true })).toBeVisible();
  }

  await pagina.setViewportSize({ width: 390, height: 844 });
  for (const tema of ["claro", "oscuro"]) {
    for (const ruta of ["/app/empresas/nueva", rutaEmpresa, `${rutaEmpresa}/editar`, "/app/contactos/nuevo", rutaContacto, `${rutaContacto}/editar`]) {
      await pagina.goto(`http://localhost:14173${ruta}`);
      await pagina.evaluate(tema => { document.documentElement.dataset.tema = tema; }, tema);
      await comprobarPantalla(pagina);
      await pagina.screenshot({ path: test.info().outputPath(`${tema}-${ruta.replaceAll("/", "-")}.png`), fullPage: true });
    }
  }
  expect(errores).toEqual([]);
  await vendedor.contexto.close();
  await manager.contexto.close();
  await admin.contexto.close();
});

test("la edición de clientes respeta asignación directa y consulta por oportunidad en los tres roles", async ({ browser, request }) => {
  const admin = await abrirSesion(browser, request, "admin");
  const empresas = await (await request.get(`${API}/companies?q=Compartida`, { headers: admin.cabeceras })).json();
  const empresa = empresas.content[0];
  const contactos = await (await request.get(`${API}/contacts?companyId=${empresa.id}`, { headers: admin.cabeceras })).json();
  const contacto = contactos.content[0];
  const vendedor = await abrirSesion(browser, request, "seller");
  const creada = await request.post(`${API}/companies`, { headers: vendedor.cabeceras,
    data: { legalName: "Directa E2E", businessName: "Directa E2E", status: "POTENCIAL" } });
  expect(creada.status()).toBe(201);
  const directa = await creada.json();
  const creado = await request.post(`${API}/contacts`, { headers: vendedor.cabeceras,
    data: { firstName: "Directo", lastName: "E2E", companyId: directa.id, status: "POTENCIAL" } });
  expect(creado.status()).toBe(201);
  const directo = await creado.json();
  const manager = await abrirSesion(browser, request, "manager");
  for (const sesion of [admin, manager, vendedor]) {
    const soloConsulta = sesion === vendedor;
    for (const ruta of [`/app/empresas/${empresa.id}`, `/app/contactos/${contacto.id}`]) {
      await sesion.pagina.goto(`http://localhost:14173${ruta}`);
      await expect(sesion.pagina.getByText("other Fixture", { exact: true })).toBeVisible();
      await expect(sesion.pagina.getByRole("link", { name: "Editar", exact: true })).toHaveCount(soloConsulta ? 0 : 1);
      await sesion.pagina.goto(`http://localhost:14173${ruta}/editar`);
      if (soloConsulta) {
        await expect(sesion.pagina.getByRole("heading", { name: "Este cliente es de consulta" })).toBeVisible();
        await expect(sesion.pagina.getByRole("button", { name: "Guardar cambios" })).toHaveCount(0);
      } else {
        await expect(sesion.pagina.getByRole("button", { name: "Guardar cambios" })).toBeVisible();
      }
    }
    for (const ruta of [`/app/empresas/${directa.id}`, `/app/contactos/${directo.id}`]) {
      await sesion.pagina.goto(`http://localhost:14173${ruta}`);
      await expect(sesion.pagina.getByRole("link", { name: "Editar", exact: true })).toBeVisible();
      await sesion.pagina.getByRole("link", { name: "Editar", exact: true }).click();
      await expect(sesion.pagina.getByRole("button", { name: "Guardar cambios" })).toBeVisible();
    }
  }
  for (const sesion of [admin, manager, vendedor]) await sesion.contexto.close();
});

test("los ejemplos de clientes de Postman son compatibles con el contrato actual", async ({ request }) => {
  const coleccion = JSON.parse(readFileSync(new URL("../../backend/docs/postman/zTech-CRM.postman_collection.json", import.meta.url), "utf8"));
  const login = await request.post(`${API}/auth/login`, {
    data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" },
  });
  expect(login.status()).toBe(200);
  const cabeceras = { Authorization: `Bearer ${(await login.json()).accessToken}` };
  const { content: origenes } = await (await request.get(`${API}/origins`, { headers: cabeceras })).json();
  const variables = new Map<string, string>([["baseUrl", API], ["originId", String(origenes.find((o: { active: boolean }) => o.active)?.id ?? "null")]]);
  const sustituir = (texto: string) => texto.replace(/{{([^}]+)}}/g, (_, clave: string) => {
    const valor = variables.get(clave);
    if (valor === undefined) throw new Error(`Variable sin preparar: ${clave}`);
    return valor;
  });
  for (const nombre of ["Companies", "Contacts"]) {
    const carpeta = coleccion.item.find((item: { name: string }) => item.name === nombre);
    for (const item of carpeta.item) {
      const respuesta = await request.fetch(sustituir(item.request.url.raw), {
        method: item.request.method, headers: cabeceras,
        data: item.request.body ? JSON.parse(sustituir(item.request.body.raw)) : undefined,
      });
      expect(respuesta.status(), item.name).toBe(item.request.method === "POST" ? 201 : 200);
      const datos = await respuesta.json();
      if (item.request.method === "POST") variables.set(nombre === "Companies" ? "companyId" : "contactId", String(datos.id));
      if (item.name.startsWith("Detalle de")) {
        expect(datos.salesRepName).toBe("admin Fixture");
        expect(datos.originId).toBe(Number(variables.get("originId")));
      }
    }
  }
});
