import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { Contexto } from "../../shared/sesion/contextoSesion";
import { PaginaEmpresaFormulario } from "./PaginaEmpresaFormulario";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer(
  http.get(`${API}/origins/:id`, ({ params }) => HttpResponse.json({ id: Number(params.id), name: params.id === "8" ? "Histórico" : "Referido", active: params.id !== "8" })),
  http.get(`${API}/users/assignable`, () => HttpResponse.json([{ id: 1, firstName: "Ana", lastName: "Equipo" }])),
  http.get(`${API}/origins`, () => HttpResponse.json({ content: [{ id: 7, name: "Referido", active: true }], page: 0, size: 20, totalElements: 1, totalPages: 1 })),
);
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());

function renderizar(edicion = false) {
  const cache = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<MemoryRouter initialEntries={[edicion ? "/app/empresas/4/editar" : "/app/empresas/nueva"]}>
    <QueryClientProvider client={cache}><Contexto.Provider value={{ sesion: { token: "prueba", venceEn: Date.now()+3600000, debeCambiarClave: false, usuario: { id: 1, nombre: "Admin", apellido: "Prueba", email: "admin@fixtures.invalid", rol: "ADMIN" } }, ingresar: async () => { throw new Error("No usado"); }, cerrar: () => {} }}><Routes>
      <Route path="/app/empresas/nueva" element={<PaginaEmpresaFormulario />} />
      <Route path="/app/empresas/:id/editar" element={<PaginaEmpresaFormulario />} />
      <Route path="/app/empresas/:id" element={<p>Empresa guardada</p>} />
    </Routes></Contexto.Provider></QueryClientProvider>
  </MemoryRouter>);
}

describe("datos comerciales de empresa", () => {
  it("envía el origen elegido y permite corregir un error de campo del servidor", async () => {
    const envios: unknown[] = [];
    servidor.use(http.post(`${API}/companies`, async ({ request }) => {
      envios.push(await request.json());
      return HttpResponse.json({ detail: "Revisá el origen", errors: [{ field: "originId", message: "Elegí otro origen" }] }, { status: 400 });
    }));
    renderizar();
    const persona = userEvent.setup();
    await persona.type(screen.getByRole("textbox", { name: "Razón social" }), "Empresa de prueba");
    await persona.type(screen.getByRole("textbox", { name: "Nombre comercial" }), "Prueba");
    await persona.click(screen.getByLabelText("Origen"));
    await persona.click(await screen.findByRole("option", { name: "Referido" }));
    expect(screen.queryByRole("option", { name: "Histórico" })).not.toBeInTheDocument();
    await persona.click(screen.getByRole("button", { name: "Crear empresa" }));
    expect(await screen.findByText("Elegí otro origen")).toBeInTheDocument();
    expect(envios[0]).toMatchObject({ originId: 7, businessName: "Prueba" });
    expect(screen.getByLabelText("Origen")).toHaveValue("Referido");
    expect(screen.getByRole("textbox", { name: "Nombre comercial" })).toHaveValue("Prueba");
  });

  it("conserva el origen histórico y muestra el responsable ausente de asignables", async () => {
    servidor.use(http.get(`${API}/companies/4`, () => HttpResponse.json({ id: 4, legalName: "Empresa", businessName: "Empresa",
      status: "POTENCIAL", originId: 8, salesRepId: 3, salesRepName: "Luis Histórico" })));
    renderizar(true);
    await waitFor(() => expect(screen.getByLabelText("Origen")).toHaveValue("Histórico (inactivo)"));
    expect(screen.getByLabelText("Responsable comercial")).toHaveValue("3");
    expect(screen.getByRole("option", { name: "Luis Histórico" })).toBeInTheDocument();
  });

  it("permite reintentar la búsqueda de orígenes sin perder datos", async () => {
    servidor.use(http.get(`${API}/origins`, () => HttpResponse.json({ detail: "Falla controlada" }, { status: 500 })));
    renderizar();
    const persona = userEvent.setup();
    await persona.type(screen.getByRole("textbox", { name: "Nombre comercial" }), "Sin perder");
    await persona.click(screen.getByLabelText("Origen"));
    expect(await screen.findByText(/No pudimos consultar las opciones/)).toBeInTheDocument();
    servidor.use(http.get(`${API}/origins`, () => HttpResponse.json({ content: [{ id: 7, name: "Referido", active: true }], page: 0, size: 20, totalElements: 1, totalPages: 1 })));
    await persona.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByRole("option", { name: "Referido" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear empresa" })).toBeEnabled();
    expect(screen.getByRole("textbox", { name: "Nombre comercial" })).toHaveValue("Sin perder");
  });
});
