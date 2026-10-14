import { elegirSelector } from "./ayudas/selectores.js";
import { expect, test, type Page } from "@playwright/test";
const API = "http://localhost:18080/api/v1";
async function evento(page: Page, titulo: string, salon: number, tipo: number, asistentes = 20) {
  await page.getByRole("textbox", { name: "Título", exact: true }).fill(titulo);
  await page.getByRole("combobox", { name: "Salón", exact: true }).fill("baseline.a");
  await elegirSelector(page, "Salón", String(salon));
  await page.getByRole("combobox", { name: "Tipo de evento", exact: true }).fill("Alta rápida E2E");
  await elegirSelector(page, "Tipo de evento", String(tipo));
  await page.getByLabel(/Inicio/).fill("2031-03-01T09:00");
  await page.getByLabel(/Fin/).fill("2031-03-01T13:00");
  await page.getByRole("spinbutton", { name: "Asistentes", exact: true }).fill(String(asistentes));
}
test("alta breve desde clientes posteriores a página cero conserva contexto, herencia y fechas", async ({ page, request }) => {
  test.setTimeout(120000);
  const sesion = await (await request.post(`${API}/auth/login`, { data: { email: "admin.baseline.a@fixtures.invalid", password: "FixtureTemporal123!" } })).json();
  const headers = { Authorization: `Bearer ${sesion.accessToken}` };
  await page.addInitScript(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), sesion);
  const usuarios = await (await request.get(`${API}/users/assignable`, { headers })).json();
  const vendedor = usuarios.find((u: { email: string }) => u.email === "seller.baseline.a@fixtures.invalid");
  let empresa = 0, contacto = 0;
  for (let i = 0; i < 26; i++) {
    const creada = await request.post(`${API}/companies`, { headers, data: { legalName: `Alta rápida empresa ${i}`, businessName: i === 25 ? "ZZZ Alta rápida empresa" : `AAA Alta rápida empresa ${String(i).padStart(2,"0")}`, status: "POTENCIAL", salesRepId: vendedor.id } });
    expect(creada.status()).toBe(201); if (i === 25) empresa = (await creada.json()).id;
  }
  for (let i = 0; i < 26; i++) {
    const creada = await request.post(`${API}/contacts`, { headers, data: { firstName: "Contacto contextual", lastName: i === 25 ? "ZZZ Alta rápida" : `AAA Alta rápida ${String(i).padStart(2,"0")}`, companyId: empresa, status: "POTENCIAL" } });
    expect(creada.status()).toBe(201); if (i === 25) contacto = (await creada.json()).id;
  }
  const individual = await (await request.post(`${API}/contacts`, { headers, data: { firstName: "Individual", lastName: "ZZZ Alta rápida", status: "POTENCIAL", salesRepId: vendedor.id } })).json();
  const salon = (await (await request.get(`${API}/venues?q=baseline.a&status=DISPONIBLE`, { headers })).json()).content[0];
  const tipo = await (await request.post(`${API}/event-types`, { headers, data: { name: "Alta rápida E2E", active: true } })).json();
  const errores: string[] = [], consultas: string[] = [];
  page.on("pageerror", e => errores.push(e.message)); page.on("console", e => { if (e.type() === "error") errores.push(e.text()); });
  page.on("request", r => { if (/\/(companies|contacts)\?/.test(r.url())) consultas.push(r.url()); });
  const oportunidades: number[] = [];
  for (const cliente of [{ id: contacto, empresa, titulo: "Alta corporativa E2E" }, { id: individual.id, empresa: undefined, titulo: "Alta individual E2E" }]) {
    await page.goto(`/app/contactos/${cliente.id}`);
    await page.getByRole("link", { name: "Nueva oportunidad", exact: true }).click();
    await expect(page.getByRole("combobox", { name: "Contacto", exact: true })).toHaveAttribute("data-valor", String(cliente.id));
    await expect(page.getByRole("combobox", { name: "Empresa", exact: true })).toHaveAttribute("data-valor", cliente.empresa ? String(cliente.empresa) : "");
    await expect(page.getByText(/Responsable propuesto: seller Fixture/)).toBeVisible();
    await expect(page.locator("details")).not.toHaveAttribute("open", "");
    await expect(page.getByLabel(/Inicio/)).toHaveValue("");
    await expect(page.getByLabel(/Fin/)).toHaveValue("");
    await evento(page, cliente.titulo, salon.id, tipo.id, cliente.empresa ? 60 : 20);
    if (cliente.empresa) {
      await expect(page.getByText(/Más de 50 asistentes/)).toBeVisible();
      await page.getByRole("spinbutton", { name: "Asistentes", exact: true }).fill("81");
      await page.getByRole("button", { name: "Crear oportunidad", exact: true }).click();
      await expect(page.getByText(/La capacidad máxima/).first()).toBeVisible();
      await expect(page.getByRole("textbox", { name: "Título", exact: true })).toHaveValue(cliente.titulo);
      await page.getByRole("spinbutton", { name: "Asistentes", exact: true }).fill("60");
    }
    await page.getByRole("button", { name: "Crear oportunidad", exact: true }).click();
    await expect(page.getByRole("heading", { name: cliente.titulo, exact: true })).toBeVisible();
    const id = Number(new URL(page.url()).pathname.split("/").at(-1)); oportunidades.push(id);
    await page.reload(); await expect(page.getByRole("heading", { name: cliente.titulo, exact: true })).toBeVisible();
    const guardada = await (await request.get(`${API}/opportunities/${id}`, { headers })).json();
    expect(guardada.company?.id).toBe(cliente.empresa); expect(guardada.contact.id).toBe(cliente.id); expect(guardada.salesRepId).toBe(vendedor.id);
    expect(guardada.eventStart).toBe("2031-03-01T12:00:00Z"); expect(guardada.eventEnd).toBe("2031-03-01T16:00:00Z");
    await page.goto(`/app/oportunidades/${id}/editar`); await expect(page.getByLabel(/Inicio/)).toHaveValue("2031-03-01T09:00");
    await page.getByRole("textbox", { name: "Observaciones", exact: true }).fill("Seguimiento posterior al alta breve");
    await page.getByRole("button", { name: "Guardar cambios", exact: true }).click();
    await expect(page.getByText("Seguimiento posterior al alta breve", { exact: true })).toBeVisible();
  }
  // Company entry point offers the same complete, cancellable form.
  await page.goto(`/app/empresas/${empresa}`); await page.getByRole("link", { name: "Nueva oportunidad", exact: true }).click();
  await expect(page.getByRole("combobox", { name: "Empresa", exact: true })).toHaveAttribute("data-valor", String(empresa));
  await page.getByRole("textbox", { name: "Título", exact: true }).fill("Cancelada sin escritura");
  await page.getByText("Más datos de la negociación", { exact: true }).click();
  await page.getByRole("textbox", { name: "Observaciones", exact: true }).fill("Opcional conservado");
  await page.getByText("Más datos de la negociación", { exact: true }).click();
  await page.getByText("Más datos de la negociación", { exact: true }).click();
  await expect(page.getByRole("textbox", { name: "Observaciones", exact: true })).toHaveValue("Opcional conservado");
  await page.getByRole("link", { name: "Cancelar", exact: true }).click(); await expect(page).toHaveURL(new RegExp(`/empresas/${empresa}$`));
  expect((await (await request.get(`${API}/opportunities?q=Cancelada%20sin%20escritura`, { headers })).json()).totalElements).toBe(0);
  await page.goto("/app/oportunidades/nueva");
  await page.getByRole("combobox", { name: "Empresa", exact: true }).fill("ZZZ Alta rápida");
  await elegirSelector(page, "Empresa", String(empresa));
  await page.getByRole("combobox", { name: "Contacto", exact: true }).fill("ZZZ Alta rápida");
  const masContactos = page.getByRole("button", { name: "Ver más resultados" });
  await expect(masContactos).toBeEnabled(); await masContactos.click();
  await elegirSelector(page, "Contacto", String(contacto));
  await page.getByRole("combobox", { name: "Contacto", exact: true }).click();
  await expect(page.getByRole("listbox").locator(`[data-valor="${individual.id}"]`)).toHaveCount(0);
  await page.keyboard.press("Escape");
  await elegirSelector(page, "Empresa", "");
  await expect(page.getByRole("combobox", { name: "Contacto", exact: true })).toHaveAttribute("data-valor", "");
  await page.getByRole("combobox", { name: "Contacto", exact: true }).fill("Individual");
  await elegirSelector(page, "Contacto", String(individual.id));
  await expect(page.getByRole("combobox", { name: "Empresa", exact: true })).toHaveAttribute("data-valor", "");
  expect(consultas.some(u => new URL(u).searchParams.get("companyId") === String(empresa))).toBe(true);
  expect(consultas.every(u => new URL(u).searchParams.get("size") === "20")).toBe(true);
  const rutas = [`/app/oportunidades/nueva?contactoId=${contacto}`, `/app/oportunidades/nueva?contactoId=${individual.id}`, `/app/oportunidades/nueva?empresaId=${empresa}`, `/app/oportunidades/${oportunidades[0]}/editar`, "/app/oportunidades", "/app/embudo", "/app/contactos", `/app/contactos/${contacto}`];
  for (const ancho of [390, 1440]) {
    await page.setViewportSize({ width: ancho, height: 900 });
    for (const ruta of rutas) {
      await page.goto(ruta); await expect(page.locator("h1")).toBeVisible(); await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      for (const tema of ["claro", "oscuro"]) { await page.evaluate(t => document.documentElement.dataset.tema = t, tema); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), ruta).toBe(true); }
      if (ruta.endsWith("/editar")) { await page.getByRole("combobox", { name: "Servicios", exact: true }).click(); await expect(page.getByRole("listbox").getByRole("option").first()).toBeVisible(); await page.keyboard.press("ArrowDown"); await page.keyboard.press("Enter"); await expect(page.getByRole("list", { name: "Servicios seleccionados" })).toBeVisible(); await page.keyboard.press("Escape"); }
      await page.keyboard.press("Tab"); expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY")).toBe(true);
    }
    await page.goto(`/app/oportunidades/nueva?contactoId=${contacto}`); await expect(page.getByRole("heading", { name: "Nueva oportunidad", exact: true })).toBeVisible();
    await page.screenshot({ path: `test-results/alta-breve-${ancho}.png`, fullPage: true });
  }
  expect(errores).toEqual([]);
});
