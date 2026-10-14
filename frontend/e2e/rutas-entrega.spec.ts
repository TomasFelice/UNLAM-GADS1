import { expect, test } from "@playwright/test";

const API = "http://localhost:18080/api/v1";

test("regresión de todas las rutas con tres roles, dos tamaños, temas, teclado y SEO", async ({ page, request }) => {
  test.setTimeout(600000);
  const acceso = async (nombre: string) => {
    const respuesta = await request.post(`${API}/auth/login`, { data: { email: `${nombre}.baseline.a@fixtures.invalid`, password: "FixtureTemporal123!" } });
    expect(respuesta.status()).toBe(200);
    return respuesta.json();
  };
  const admin = await acceso("admin"), vendedor = await acceso("seller");
  const headers = { Authorization: `Bearer ${admin.accessToken}` };
  const post = async (ruta: string, data: object) => {
    const r = await request.post(`${API}/${ruta}`, { headers, data });
    expect(r.status(), await r.text()).toBe(201);
    return r.json();
  };
  const empresa = await post("companies", { legalName: "Rutas entrega SA", businessName: "Rutas entrega", status: "POTENCIAL", salesRepId: vendedor.user.id });
  const contacto = await post("contacts", { firstName: "Ana", lastName: "Rutas entrega", companyId: empresa.id, status: "POTENCIAL", salesRepId: vendedor.user.id });
  const embudo = await (await request.get(`${API}/pipelines/default`, { headers })).json();
  const etapas = await (await request.get(`${API}/stages?pipelineId=${embudo.id}`, { headers })).json();
  const salon = (await (await request.get(`${API}/venues?status=DISPONIBLE`, { headers })).json()).content[0];
  const servicio = (await (await request.get(`${API}/event-services`, { headers })).json()).content[0];
  const tipo = (await (await request.get(`${API}/event-types?active=true`, { headers })).json()).content[0];
  const oportunidad = await post("opportunities", { title: "Rutas entrega evento", companyId: empresa.id, contactId: contacto.id, salesRepId: vendedor.user.id, pipelineId: embudo.id, stageId: etapas.find((e: any) => e.active && e.kind === "OPEN").id, venueId: salon.id, eventTypeId: tipo.id, eventStart: "2041-01-01T12:00:00Z", eventEnd: "2041-01-01T16:00:00Z", attendeeCount: 5 });
  const comerciales = ["/app", "/app/empresas", "/app/empresas/nueva", `/app/empresas/${empresa.id}`, `/app/empresas/${empresa.id}/editar`, "/app/contactos", "/app/contactos/nuevo", `/app/contactos/${contacto.id}`, `/app/contactos/${contacto.id}/editar`, "/app/oportunidades", "/app/oportunidades/nueva", `/app/oportunidades/nueva?empresaId=${empresa.id}`, `/app/oportunidades/nueva?contactoId=${contacto.id}`, `/app/oportunidades/${oportunidad.id}`, `/app/oportunidades/${oportunidad.id}/editar`, `/app/embudo?pipelineId=${embudo.id}`, "/app/salones", `/app/salones/${salon.id}`, "/app/servicios", `/app/servicios/${servicio.id}`];
  const administrativas = ["/app/usuarios", "/app/usuarios/nuevo", `/app/usuarios/${admin.user.id}`, `/app/usuarios/${admin.user.id}/editar`, "/app/salones/nuevo", `/app/salones/${salon.id}/editar`, "/app/servicios/nuevo", `/app/servicios/${servicio.id}/editar`, "/app/configuracion", "/app/configuracion/embudos", "/app/configuracion/embudos/nuevo", `/app/configuracion/embudos/${embudo.id}`];
  for (const [ruta, recurso] of [["origenes", "origins"], ["motivos-perdida", "loss-reasons"], ["tipos-actividad", "activity-types"], ["tipos-evento", "event-types"]]) {
    const registro = (await (await request.get(`${API}/${recurso}`, { headers })).json()).content[0];
    administrativas.push(...["", "/nuevo", `/${registro.id}`, `/${registro.id}/editar`].map(sufijo => `/app/configuracion/${ruta}${sufijo}`));
  }
  const errores: string[] = [], evidencia: object[] = [];
  page.on("pageerror", e => errores.push(e.message));
  page.on("console", m => { if (m.type() === "error") errores.push(m.text()); });
  await page.goto("/ingresar");
  for (const rol of ["admin", "manager", "seller"]) {
    const sesion = await acceso(rol);
    await page.evaluate(d => sessionStorage.setItem("ztech.crm.sesion", JSON.stringify({ token: d.accessToken, venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: d.user.id, nombre: d.user.firstName, apellido: d.user.lastName, email: d.user.email, rol: d.user.role } })), sesion);
    for (const ruta of [...comerciales, ...administrativas]) {
      await page.goto(ruta);
      const denegada = rol !== "admin" && administrativas.includes(ruta);
      const titulo = denegada ? page.getByRole("heading", { name: "Acceso denegado", exact: true }) : page.locator("h1");
      await expect(titulo).toBeVisible();
      await expect(page.getByText(/^(Cargando|Preparando).*…$/)).toHaveCount(0);
      await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
      for (const ancho of [390, 1440]) for (const tema of ["claro", "oscuro"]) {
        await page.setViewportSize({ width: ancho, height: 900 });
        await page.evaluate(t => document.documentElement.dataset.tema = t, tema);
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${rol} ${ruta} ${ancho} ${tema}`).toBe(true);
        await titulo.click();
        await page.keyboard.press("Tab");
        expect(await page.evaluate(() => document.activeElement?.tagName !== "BODY"), `${ruta}: foco de teclado`).toBe(true);
        evidencia.push({ rol, ruta, ancho, tema, scrollHorizontal: false, teclado: true, noindex: true });
      }
      if (ruta === `/app/oportunidades/${oportunidad.id}`) {
        await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
        await page.screenshot({ path: test.info().outputPath(`${rol}-escritorio-oscuro.png`), fullPage: true, animations: "disabled" });
        await page.setViewportSize({ width: 390, height: 900 });
        const navegacion = page.getByRole("navigation", { name: "Navegación de la aplicación" });
        await expect(navegacion).not.toBeInViewport();
        await page.getByRole("button", { name: "Abrir la navegación", exact: true }).focus();
        await page.keyboard.press("Enter");
        await expect(navegacion).toBeInViewport();
        await page.getByRole("button", { name: "Cerrar la navegación", exact: true }).focus();
        await page.keyboard.press("Enter");
        await expect(navegacion).not.toBeInViewport();
        await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
        await page.screenshot({ path: test.info().outputPath(`${rol}-movil-oscuro.png`), fullPage: true, animations: "disabled" });
      }
    }
  }
  await test.info().attach("rutas-verificadas", { body: JSON.stringify(evidencia, null, 2), contentType: "application/json" });
  expect(evidencia).toHaveLength(576);
  expect(errores).toEqual([]);
});
