import { expect, test } from "@playwright/test";

const API = "http://localhost:18080/api/v1";

test("administrador crea, edita y restablece; vendedor cambia la clave e ingresa", async ({ browser, page, request }) => {
  const errores: string[] = [];
  page.on("pageerror", error => errores.push(error.message));
  page.on("console", mensaje => { if (mensaje.type() === "error") errores.push(mensaje.text()); });
  await expect.poll(async () => (await request.post(`${API}/auth/login`, {
    data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" },
  })).status(), { timeout: 15000 }).toBe(200);
  await page.goto("/ingresar");
  await page.getByLabel("Correo electrónico").fill("admin.baseline.a@fixtures.invalid");
  await page.getByLabel("Contraseña", { exact: false }).fill("FixtureTemporal123!");
  await page.getByRole("button", { name: "Iniciar sesión" }).click();
  await page.getByRole("link", { name: "Usuarios", exact: true }).click();
  await page.getByRole("link", { name: "Nuevo usuario" }).click();
  await page.getByRole("textbox", { name: "Nombre", exact: true }).fill("Sara");
  await page.getByLabel("Apellido").fill("Verificación");
  await page.getByLabel("Correo electrónico").fill("sara.e2e@fixtures.invalid");
  await page.getByLabel("Contraseña temporal").fill("TemporalE2e123!");
  await page.getByRole("button", { name: "Crear usuario" }).click();
  await expect(page.getByRole("heading", { name: "Sara Verificación" })).toBeVisible();
  const rutaDetalle = new URL(page.url()).pathname;

  const contextoVendedor = await browser.newContext();
  const vendedor = await contextoVendedor.newPage();
  vendedor.on("pageerror", error => errores.push(error.message));
  vendedor.on("console", mensaje => { if (mensaje.type() === "error") errores.push(mensaje.text()); });
  await vendedor.goto("http://localhost:14173/ingresar");
  await vendedor.getByLabel("Correo electrónico").fill("sara.e2e@fixtures.invalid");
  await vendedor.getByLabel("Contraseña", { exact: false }).fill("TemporalE2e123!");
  await vendedor.getByRole("button", { name: "Iniciar sesión" }).click();
  await expect(vendedor).toHaveURL(/\/app\/cambiar-clave$/);
  await vendedor.getByLabel("Contraseña temporal").fill("TemporalE2e123!");
  await vendedor.getByLabel("Nueva contraseña", { exact: false }).first().fill("PersonalE2e123!");
  await vendedor.getByLabel("Repetir nueva contraseña").fill("PersonalE2e123!");
  await vendedor.getByRole("button", { name: "Guardar contraseña" }).click();
  await expect(vendedor).toHaveURL(/\/ingresar$/);
  await expect(vendedor.getByRole("status")).toContainText("Contraseña actualizada");
  for (const tema of ["claro", "oscuro"]) {
    await vendedor.setViewportSize({ width: 390, height: 844 });
    await vendedor.evaluate(t => document.documentElement.dataset.tema = t, tema);
    await expect(vendedor.getByRole("status").locator('svg[aria-hidden="true"]')).toBeVisible();
    expect(await vendedor.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await vendedor.getByRole("status").screenshot({ path: test.info().outputPath(`clave-actualizada-${tema}.png`) });
  }
  await vendedor.getByLabel("Correo electrónico").fill("sara.e2e@fixtures.invalid");
  await vendedor.getByLabel("Contraseña", { exact: false }).fill("PersonalE2e123!");
  await vendedor.getByRole("button", { name: "Iniciar sesión" }).click();
  await expect(vendedor.getByRole("heading", { name: "Panel comercial" })).toBeVisible();
  await expect(vendedor.getByRole("link", { name: "Usuarios", exact: true })).toHaveCount(0);
  await vendedor.goto("http://localhost:14173/app/usuarios");
  await expect(vendedor.getByRole("heading", { name: "Acceso denegado" })).toBeVisible();

  await page.getByRole("link", { name: "Editar usuario" }).click();
  await page.getByRole("textbox", { name: "Nombre", exact: true }).fill("Sara actualizada");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByRole("heading", { name: "Sara actualizada Verificación" })).toBeVisible();
  await page.getByRole("button", { name: "Restablecer contraseña" }).click();
  await page.getByLabel("Nueva contraseña temporal").fill("RestablecidaE2e123!");
  await page.getByRole("button", { name: "Confirmar restablecimiento" }).click();
  await expect(page.getByRole("status")).toContainText("Contraseña restablecida");
  for (const tema of ["claro", "oscuro"]) {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
    await expect(page.getByRole("status").locator('svg[aria-hidden="true"]')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.getByRole("status").screenshot({ path: test.info().outputPath(`clave-restablecida-${tema}.png`) });
  }
  await expect(page.getByText("Debe cambiar la contraseña temporal", { exact: true })).toBeVisible();
  const sesion = await vendedor.evaluate(() => JSON.parse(sessionStorage.getItem("ztech.crm.sesion")!));
  expect((await request.get(`${API}/users/assignable`, { headers: { Authorization: `Bearer ${sesion.token}` } })).status()).toBe(401);

  await page.goto(`${rutaDetalle}/editar`);
  await page.getByLabel("Usuario activo").uncheck();
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await expect(page.getByText("Inactivo", { exact: true })).toBeVisible();
  expect((await request.post(`${API}/auth/login`, { data: { email: "sara.e2e@fixtures.invalid", password: "RestablecidaE2e123!" } })).status()).toBe(401);
  await contextoVendedor.close();
  expect(errores).toEqual([]);
});

test("rutas administrativas adaptadas a 390 px, ambos temas y sin errores de consola", async ({ page, request }) => {
  const errores: string[] = [];
  page.on("pageerror", error => errores.push(error.message));
  page.on("console", mensaje => { if (mensaje.type() === "error") errores.push(mensaje.text()); });
  const respuesta = await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } });
  expect(respuesta.status()).toBe(200);
  const datos = await respuesta.json();
  await page.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken,
    venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: "admin", apellido: "Fixture", email: datos.user.email, rol: "ADMIN" } })), datos);
  await page.setViewportSize({ width: 390, height: 844 });
  for (const tema of ["claro", "oscuro"]) {
    for (const ruta of ["/app/usuarios", "/app/usuarios/nuevo", `/app/usuarios/${datos.user.id}`, `/app/usuarios/${datos.user.id}/editar`]) {
      await page.goto(ruta);
      await page.evaluate(tema => { document.documentElement.dataset.tema = tema; }, tema);
      await expect(page.locator("h1")).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      await page.screenshot({ path: test.info().outputPath(`${tema}-${ruta.replaceAll("/", "-")}.png`), fullPage: true });
      await page.keyboard.press("Tab");
      expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
    }
  }
  expect(errores).toEqual([]);
});

test("roles comerciales no obtienen administración por menú ni URL", async ({ browser, request }) => {
  for (const nombre of ["seller", "manager"]) {
    const respuesta = await request.post(`${API}/auth/login`, { data: { email: `${nombre}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } });
    expect(respuesta.status()).toBe(200);
    const datos = await respuesta.json();
    const contexto = await browser.newContext();
    await contexto.addInitScript(datos => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: datos.accessToken,
      venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: datos.user.id, nombre: datos.user.firstName,
        apellido: datos.user.lastName, email: datos.user.email, rol: datos.user.role } })), datos);
    const pagina = await contexto.newPage();
    await pagina.goto("http://localhost:14173/app/usuarios");
    await expect(pagina.getByRole("heading", { name: "Acceso denegado" })).toBeVisible();
    await expect(pagina.getByRole("link", { name: "Usuarios", exact: true })).toHaveCount(0);
    expect((await request.get(`${API}/users`, { headers: { Authorization: `Bearer ${datos.accessToken}` } })).status()).toBe(403);
    await contexto.close();
  }
});

test("las páginas públicas conservan metadatos por ruta", async ({ page }) => {
  for (const ruta of ["/", "/nosotros", "/contacto", "/terminos", "/privacidad"]) {
    await page.goto(ruta);
    await expect(page.locator("h1")).toBeVisible();
    expect((await page.title()).length).toBeGreaterThan(10);
    const descripcion = await page.locator('meta[name="description"]').getAttribute("content");
    expect(descripcion?.length).toBeGreaterThan(30);
    const canonica = await page.locator('link[rel="canonical"]').getAttribute("href");
    expect(new URL(canonica!).pathname).toBe(ruta);
    await expect(page.locator('meta[name="robots"]')).not.toHaveAttribute("content", /noindex/);
  }
});
