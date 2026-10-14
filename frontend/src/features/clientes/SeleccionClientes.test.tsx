import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HttpResponse, delay, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { Contexto } from "../../shared/sesion/contextoSesion";
import { PaginaOportunidadFormulario } from "../oportunidades/PaginaOportunidadFormulario";
import { SelectorCliente } from "./SelectorCliente";
const API = "http://localhost:8080/api/v1";
const pagina = (content: unknown[], totalPages = 1) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages });
const servidor = setupServer();
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" })); afterEach(() => servidor.resetHandlers()); afterAll(() => servidor.close());
function entorno(contenido: React.ReactNode) { return <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{contenido}</QueryClientProvider>; }
function ElegirEmpresa() { const [valor, cambiar] = useState("99"); return <SelectorCliente tipo="empresa" valor={valor} alCambiar={cambiar} />; }
describe("selección completa de clientes", () => {
  it("consulta páginas bajo demanda y conserva la empresa elegida fuera de la página visible", async () => {
    const consultas: string[] = [];
    servidor.use(http.get(`${API}/companies/:id`, ({ params }) => HttpResponse.json({ id: Number(params.id), businessName: "Empresa histórica" })),
      http.get(`${API}/companies`, ({ request }) => { const url = new URL(request.url); consultas.push(url.search); return HttpResponse.json(pagina([{ id: url.searchParams.get("page") === "1" ? 25 : 1, businessName: url.searchParams.get("page") === "1" ? "Empresa posterior" : "Primera empresa" }], 2)); }));
    render(entorno(<ElegirEmpresa />)); const persona = userEvent.setup();
    await waitFor(() => expect(screen.getByRole("combobox")).toHaveValue("Empresa histórica"));
    expect(consultas).toHaveLength(0);
    await persona.click(screen.getByRole("combobox"));
    await persona.click(await screen.findByRole("button", { name: "Ver más resultados" }));
    await persona.click(await screen.findByRole("option", { name: "Empresa posterior" }));
    expect(screen.getByRole("combobox")).toHaveAttribute("data-valor", "25"); expect(consultas).toHaveLength(2);
    expect(consultas[1]).toContain("page=1"); expect(consultas.every(q => q.includes("size=20"))).toBe(true);
  });
  it("deriva contexto corporativo por ID y un cambio de empresa descarta la respuesta de contacto pendiente", async () => {
    let lentaTerminada = false; const filtros: string[] = [];
    servidor.use(
      http.get(`${API}/contacts/:id`, async ({ params }) => { if (params.id === "2") { await delay(300); lentaTerminada = true; } return HttpResponse.json({ id: Number(params.id), firstName: params.id === "99" ? "Contacto contextual" : "Respuesta tardía", lastName: "Prueba", company: { id: params.id === "99" ? 8 : 1, businessName: "Empresa vinculada" }, salesRepId: 7, salesRepName: "Responsable heredado" }); }),
      http.get(`${API}/companies/:id`, ({ params }) => HttpResponse.json({ id: Number(params.id), businessName: params.id === "8" ? "Empresa vinculada" : "Otra empresa", salesRepName: "Otro responsable" })),
      http.get(`${API}/companies`, () => HttpResponse.json(pagina([{ id: 1, businessName: "Otra empresa" }, { id: 8, businessName: "Empresa vinculada" }]))),
      http.get(`${API}/contacts`, ({ request }) => { const empresa = new URL(request.url).searchParams.get("companyId") ?? ""; filtros.push(empresa); return HttpResponse.json(pagina(empresa === "1" ? [{ id: 2, firstName: "Respuesta tardía", lastName: "Prueba" }] : [])); }),
      http.get(`${API}/pipelines/default`, () => HttpResponse.json({ id: 3, name: "Predeterminado", active: true, defaultPipeline: true })),
      http.get(`${API}/pipelines/:id`, () => HttpResponse.json({ id: 3, name: "Predeterminado", active: true, defaultPipeline: true })),
      http.get(`${API}/pipelines`, () => HttpResponse.json(pagina([{ id: 3, name: "Predeterminado", active: true }]))),
      http.get(`${API}/stages`, () => HttpResponse.json([{ id: 11, pipelineId: 3, name: "Consulta", kind: "OPEN", active: true }])),
      http.get(`${API}/users/assignable`, () => HttpResponse.json([])),
      ...["origins", "event-types", "event-services", "venues"].map(recurso => http.get(`${API}/${recurso}`, () => HttpResponse.json(pagina([])))),
    );
    render(entorno(<Contexto.Provider value={{ sesion: { token: "prueba", venceEn: Date.now() + 3600000, debeCambiarClave: false, usuario: { id: 1, nombre: "Admin", apellido: "Prueba", email: "admin@fixtures.invalid", rol: "ADMIN" } }, ingresar: async () => { throw new Error("No usado"); }, cerrar: () => {} }}><MemoryRouter initialEntries={["/app/oportunidades/nueva?contactoId=99"]}><Routes><Route path="/app/oportunidades/nueva" element={<PaginaOportunidadFormulario />} /></Routes></MemoryRouter></Contexto.Provider>));
    const persona = userEvent.setup();
    await waitFor(() => expect(screen.getByRole("combobox", { name: "Contacto" })).toHaveValue("Contacto contextual Prueba"));
    await waitFor(() => expect(screen.getByRole("combobox", { name: "Empresa" })).toHaveValue("Empresa vinculada"));
    expect(await screen.findByText(/Responsable propuesto: Responsable heredado/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Inicio/)).toHaveValue("");
    await waitFor(() => expect(screen.getByRole("combobox", { name: "Etapa inicial" })).toHaveValue("11"));
    await persona.click(screen.getByRole("combobox", { name: "Empresa" }));
    await persona.click(await screen.findByRole("option", { name: "Otra empresa" }));
    expect(screen.getByRole("combobox", { name: "Contacto" })).toHaveValue("");
    await persona.click(screen.getByRole("combobox", { name: "Contacto" }));
    await persona.click(await screen.findByRole("option", { name: "Respuesta tardía Prueba" }));
    await persona.click(screen.getByRole("combobox", { name: "Empresa" }));
    await persona.click(await screen.findByRole("option", { name: "Empresa vinculada" }));
    await persona.click(screen.getByRole("combobox", { name: "Contacto" }));
    await waitFor(() => expect(lentaTerminada).toBe(true));
    await waitFor(() => expect(screen.getByRole("combobox", { name: "Empresa" })).toHaveValue("Empresa vinculada"));
    expect(screen.getByRole("combobox", { name: "Contacto" })).toHaveValue("");
    expect(filtros).toContain("1"); expect(filtros).toContain("8");
    expect(screen.queryByRole("option", { name: "Respuesta tardía Prueba" })).not.toBeInTheDocument();
  });
});
