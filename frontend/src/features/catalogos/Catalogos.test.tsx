import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HttpResponse, delay, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { SelectorCatalogo } from "./SelectorCatalogo";
import { PaginaCatalogoFormulario } from "./PaginaCatalogoFormulario";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer();
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());
function entorno(contenido: React.ReactNode) {
  return <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{contenido}</QueryClientProvider>;
}
function Elegir() {
  const [valor, cambiar] = useState("99");
  return <SelectorCatalogo catalogo="origenes" valor={valor} alCambiar={cambiar} soloActivos />;
}

describe("catálogos comerciales", () => {
  it("pagina bajo demanda, conserva una selección inactiva por ID y descarta respuestas tardías", async () => {
    const consultas: string[] = [];
    servidor.use(
      http.get(`${API}/origins/:id`, ({ params }) => HttpResponse.json({ id: Number(params.id), name: params.id === "99" ? "Histórico" : "Página posterior", active: params.id !== "99" })),
      http.get(`${API}/origins`, async ({ request }) => {
        const url = new URL(request.url); consultas.push(url.search);
        const pagina = url.searchParams.get("page");
        const q = url.searchParams.get("q");
        if (q === "lenta") await delay(150);
        return HttpResponse.json({ content: [{ id: pagina === "1" ? 25 : q === "lenta" ? 2 : 1, name: pagina === "1" ? "Página posterior" : q === "lenta" ? "Respuesta vieja" : "Opción vigente", active: true }], page: Number(pagina), size: 20, totalElements: 25, totalPages: 2 });
      }),
    );
    render(entorno(<Elegir />));
    const persona = userEvent.setup();
    await waitFor(() => expect(screen.getByRole("combobox")).toHaveValue("Histórico (inactivo)"));
    expect(consultas).toHaveLength(0);
    await persona.click(screen.getByRole("combobox"));
    await persona.click(await screen.findByRole("button", { name: "Ver más resultados" }));
    await screen.findByRole("option", { name: "Página posterior" });
    await waitFor(() => expect(screen.getByRole("combobox")).toHaveFocus());
    await persona.keyboard("{Enter}");
    expect(screen.getByRole("combobox")).toHaveAttribute("data-valor", "25");
    const buscar = screen.getByRole("combobox");
    await persona.click(buscar);
    await persona.type(buscar, "lenta");
    await waitFor(() => expect(consultas.some(q => q.includes("q=lenta"))).toBe(true));
    await persona.clear(buscar);
    await persona.type(buscar, "vigente");
    expect(await screen.findByRole("option", { name: "Opción vigente" })).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("option", { name: "Respuesta vieja" })).not.toBeInTheDocument());
    expect(consultas.some(q => q.includes("page=1"))).toBe(true);
    expect(consultas.every(q => q.includes("size=20") && q.includes("active=true"))).toBe(true);
  });

  it("conserva nombre y estado al rechazar duplicado y guarda la corrección", async () => {
    const envios: unknown[] = [];
    servidor.use(http.post(`${API}/origins`, async ({ request }) => {
      const datos = await request.json(); envios.push(datos);
      return envios.length === 1 ? HttpResponse.json({ detail: "Ya existe un origen con ese nombre.", errors: [] }, { status: 409 }) : HttpResponse.json({ id: 7, ...(datos as object) }, { status: 201 });
    }));
    render(entorno(<MemoryRouter initialEntries={["/nuevo"]}><Routes><Route path="nuevo" element={<PaginaCatalogoFormulario catalogo="origenes" />} /><Route path="/app/configuracion/origenes/7" element={<p>Guardado</p>} /></Routes></MemoryRouter>));
    const persona = userEvent.setup();
    await persona.type(screen.getByRole("textbox", { name: "Nombre" }), "Referido");
    await persona.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "false");
    await persona.click(screen.getByRole("button", { name: "Crear origen" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Ya existe");
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Referido");
    expect(screen.getByRole("combobox", { name: "Estado" })).toHaveValue("false");
    await persona.type(screen.getByRole("textbox", { name: "Nombre" }), " nuevo");
    await persona.click(screen.getByRole("button", { name: "Crear origen" }));
    expect(await screen.findByText("Guardado")).toBeInTheDocument();
    expect(envios[1]).toEqual({ name: "Referido nuevo", active: false });
  });
});
