import { expect, test } from "@playwright/test";

const API = "http://localhost:18080/api/v1";
test("carga por ruta, panel acotado, errores independientes y navegación vigente", async ({ page, request }) => {
  const acceso = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  await page.addInitScript(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), acceso);
  const consultas: string[] = []; const scripts: string[] = []; const errores: string[] = [];
  page.on("request", r => { if (r.url().includes("/api/v1/")) consultas.push(new URL(r.url()).pathname + new URL(r.url()).search); if (r.resourceType() === "script") scripts.push(r.url()); });
  page.on("pageerror", e => errores.push(e.message));
  await page.goto("/app");
  await expect(page.getByRole("region", { name: "Oportunidades recientes" }).getByRole("listitem")).toHaveCount(5);
  await expect(page.getByRole("region", { name: "Actividad reciente" }).getByRole("listitem")).toHaveCount(8);
  await page.waitForLoadState("networkidle");
  expect(consultas).toHaveLength(2);
  expect(consultas).toContain("/api/v1/opportunities?page=0&size=5&sort=createdAt,desc");
  expect(scripts.some(s => /PaginaOportunidadFormulario|PaginaUsuarios|PaginaEmbudo-/.test(s))).toBe(false);
  const volver = async (ruta: string) => { await page.evaluate(r => { history.pushState(null, "", r); dispatchEvent(new PopStateEvent("popstate")); }, ruta); };
  await volver("/nosotros"); await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  consultas.length = 0; await volver("/app");
  await expect(page.getByRole("region", { name: "Oportunidades recientes" }).getByRole("listitem")).toHaveCount(5);
  await page.waitForLoadState("networkidle"); expect(consultas).toHaveLength(0);
  for (const width of [390, 1440]) for (const tema of ["claro", "oscuro"]) {
    await page.setViewportSize({ width, height: 900 }); await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
  }
  consultas.length = 0; await page.goto("/app/oportunidades/nueva");
  await expect(page.getByRole("heading", { name: "Nueva oportunidad", exact: true })).toBeVisible();
  await page.waitForLoadState("networkidle");
  expect(consultas.some(r => /\/opportunities\?|\/board|\/activities/.test(r))).toBe(false);
  expect(consultas.every(r => !r.includes("page=") || r.includes("page=0") && r.includes("size=20"))).toBe(true);
  for (const ruta of ["oportunidades/no-valido", "oportunidades/0/editar", "salones/-1", "servicios/no-valido"]) {
    consultas.length = 0; await page.goto(`/app/${ruta}`); await page.waitForLoadState("networkidle"); expect(consultas, ruta).toHaveLength(0);
  }
  let intentos = 0;
  await page.route("**/api/v1/activities?*", route => { intentos++; return route.fulfill({ status: 403, contentType: "application/problem+json", body: JSON.stringify({ status: 403, detail: "Sin permiso de consulta.", errors: [] }) }); });
  await page.goto("/app");
  await expect(page.getByRole("region", { name: "Oportunidades recientes" }).getByRole("listitem")).toHaveCount(5);
  await expect(page.getByText("Sin permiso de consulta.")).toBeVisible();
  await page.waitForTimeout(3200); expect(intentos).toBe(1);
  await page.unroute("**/api/v1/activities?*"); await page.getByRole("button", { name: "Reintentar actividades" }).click();
  await expect(page.getByRole("region", { name: "Actividad reciente" }).getByRole("listitem")).toHaveCount(8);
  expect(errores).toEqual([]);
});
