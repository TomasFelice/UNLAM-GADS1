import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { HttpResponse, delay, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { PaginaUsuarios } from "./PaginaUsuarios";
import { PaginaUsuarioDetalle } from "./PaginaUsuarioDetalle";
import { Contexto } from "../../shared/sesion/contextoSesion";

const consultas: URL[] = [];
const paginaVacia = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
const servidor = setupServer(http.get("http://localhost:8080/api/v1/users", async ({ request }) => {
  consultas.push(new URL(request.url));
  await delay(30);
  return HttpResponse.json(paginaVacia);
}));
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => { servidor.resetHandlers(); consultas.length = 0; });
afterAll(() => servidor.close());

function Ubicacion() { return <output aria-label="URL actual">{useLocation().search}</output>; }
function renderizar(ruta = "/app/usuarios") {
  const cache = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<MemoryRouter initialEntries={[ruta]}><QueryClientProvider client={cache}>
    <Contexto.Provider value={{ sesion: null, ingresar: async () => { throw new Error("No usado"); }, cerrar: () => {} }}>
      <Ubicacion /><Routes>
        <Route path="/app/usuarios" element={<PaginaUsuarios />} />
        <Route path="/app/usuarios/:id" element={<PaginaUsuarioDetalle />} />
      </Routes>
    </Contexto.Provider>
  </QueryClientProvider></MemoryRouter>);
}

describe("administración de usuarios", () => {
  it("conserva filtros y página de la URL, y reinicia página al cambiar rol", async () => {
    const persona = userEvent.setup();
    renderizar("/app/usuarios?q=Ana&role=SELLER&active=false&page=2");
    expect(screen.getByText("Cargando usuarios…")).toBeInTheDocument();
    expect(await screen.findByText("No hay usuarios para mostrar")).toBeInTheDocument();
    await new Promise(resolver => setTimeout(resolver, 400));
    expect(consultas.at(-1)?.searchParams.get("page")).toBe("2");
    expect(consultas.at(-1)?.searchParams.get("q")).toBe("Ana");
    expect(consultas.at(-1)?.searchParams.get("active")).toBe("false");
    await persona.selectOptions(screen.getByLabelText("Rol"), "SALES_MANAGER");
    await waitFor(() => expect(consultas.at(-1)?.searchParams.get("role")).toBe("SALES_MANAGER"));
    expect(consultas.at(-1)?.searchParams.get("page")).toBe("0");
    expect(screen.getByLabelText("URL actual")).toHaveTextContent("active=false");
  });

  it("permite paginar y navegar al detalle de un usuario inactivo", async () => {
    servidor.use(http.get("http://localhost:8080/api/v1/users", ({ request }) => {
      const pagina = Number(new URL(request.url).searchParams.get("page"));
      return HttpResponse.json({ content: [{ id: 27, firstName: "Ana", lastName: "Equipo", email: "ana@fixtures.invalid", role: "SELLER", active: false }],
        page: pagina, size: 20, totalElements: 21, totalPages: 2 });
    }));
    servidor.use(http.get("http://localhost:8080/api/v1/users/27", () => HttpResponse.json({ id: 27, firstName: "Ana", lastName: "Equipo", role: "SELLER", active: false })));
    const persona = userEvent.setup(); renderizar();
    await persona.click(await screen.findByRole("button", { name: "Página siguiente" }));
    await waitFor(() => expect(screen.getByLabelText("URL actual")).toHaveTextContent("page=1"));
    await persona.click(screen.getByRole("link", { name: "Ana Equipo" }));
    expect(await screen.findByRole("heading", { name: "Ana Equipo" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Editar usuario" })).toHaveAttribute("href", "/app/usuarios/27/editar");
    expect(screen.getByText("Inactivo")).toBeInTheDocument();
  });

  it("recupera el listado tras un error mediante reintento", async () => {
    servidor.use(http.get("http://localhost:8080/api/v1/users", () => HttpResponse.json({ detail: "Falla controlada" }, { status: 500 })));
    const persona = userEvent.setup(); renderizar();
    expect(await screen.findByText("Falla controlada")).toBeInTheDocument();
    servidor.use(http.get("http://localhost:8080/api/v1/users", () => HttpResponse.json(paginaVacia)));
    await persona.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("No hay usuarios para mostrar")).toBeInTheDocument();
  });
});
