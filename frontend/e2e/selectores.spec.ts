import { expect, test } from "@playwright/test";

test("selectores compactos: teclado, desplegable visible y temas a 390 px", async ({ page, request }) => {
  test.setTimeout(90000);
  const sesion = await (await request.post("http://localhost:18080/api/v1/auth/login", { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  await page.addInitScript(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), sesion);
  const errores: string[] = []; const consultas: string[] = [];
  page.on("pageerror", e => errores.push(e.message));
  page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  page.on("request", r => { if (/\/(companies|contacts|origins|event-types|event-services|venues)\?/.test(r.url())) consultas.push(r.url()); });
  for (const ancho of [390, 1440]) for (const tema of ["claro", "oscuro"]) {
    await page.setViewportSize({ width: ancho, height: 900 });
    await page.goto("/app/oportunidades/nueva");
    await page.getByRole("heading", { name: "Nueva oportunidad", exact: true }).waitFor();
    await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
    await page.waitForLoadState("networkidle");
    expect(consultas).toHaveLength(0);
    expect(await page.getByRole("searchbox").count()).toBe(0);
    await page.screenshot({ path: test.info().outputPath(`formulario-${ancho}-${tema}.png`), fullPage: true });
    if (!(await page.locator("details").evaluate(e => e.hasAttribute("open")))) await page.getByText("Más datos de la negociación", { exact: true }).click();
    for (const nombre of ["Empresa", "Contacto", "Embudo", "Salón", "Tipo de evento", "Origen", "Servicios"]) {
      const control = page.getByRole("combobox", { name: nombre, exact: true });
      await control.click();
      const lista = page.getByRole("listbox");
      await expect(lista).toBeVisible();
      await expect(lista.getByRole("option").first()).toBeVisible();
      const limites = await lista.boundingBox();
      expect(limites!.x).toBeGreaterThanOrEqual(0); expect(limites!.x + limites!.width).toBeLessThanOrEqual(ancho);
      expect(limites!.y).toBeGreaterThanOrEqual(0); expect(limites!.y + limites!.height).toBeLessThanOrEqual(900);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      if (nombre === "Salón" || nombre === "Servicios") await page.screenshot({ path: test.info().outputPath(`selector-${nombre}-${ancho}-${tema}.png`) });
      await page.keyboard.press("Escape"); await expect(lista).toHaveCount(0); await expect(control).toBeFocused();
    }
    consultas.length = 0;
  }
  expect(errores).toEqual([]);
});
