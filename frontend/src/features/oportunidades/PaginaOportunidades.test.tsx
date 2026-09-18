import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HttpResponse, delay, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { PaginaOportunidades } from "./PaginaOportunidades";

let consultaRecibida = "";
const paginaVacia = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
const servidor = setupServer(
  http.get("http://localhost:8080/api/v1/opportunities", async ({ request }) => {
    consultaRecibida = request.url;
    await delay(30);
    return HttpResponse.json(paginaVacia);
  }),
  http.get("http://localhost:8080/api/v1/stages", () => HttpResponse.json([])),
  http.get("http://localhost:8080/api/v1/origins", () => HttpResponse.json([])),
  http.get("http://localhost:8080/api/v1/users/assignable", () => HttpResponse.json([])),
  http.get("http://localhost:8080/api/v1/venues", () => HttpResponse.json([])),
  http.get("http://localhost:8080/api/v1/companies", () => HttpResponse.json(paginaVacia)),
  http.get("http://localhost:8080/api/v1/contacts", () => HttpResponse.json(paginaVacia)),
);

beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => { servidor.resetHandlers(); consultaRecibida = ""; });
afterAll(() => servidor.close());

function renderizar(ruta = "/app/oportunidades") {
  const cache = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<MemoryRouter initialEntries={[ruta]}><QueryClientProvider client={cache}><Routes><Route path="/app/oportunidades" element={<PaginaOportunidades />} /></Routes></QueryClientProvider></MemoryRouter>);
}

describe("listado de oportunidades", () => {
  it("muestra carga y vacío, y envía filtros URL con fechas argentinas", async () => {
    renderizar("/app/oportunidades?stageId=3&eventFrom=2026-10-01&eventTo=2026-10-31");
    expect(screen.getByText("Cargando oportunidades…")).toBeInTheDocument();
    expect(await screen.findByText("No hay oportunidades para mostrar")).toBeInTheDocument();
    const url = new URL(consultaRecibida);
    expect(url.searchParams.get("stageId")).toBe("3");
    expect(url.searchParams.get("eventFrom")).toBe("2026-10-01T00:00:00-03:00");
    expect(url.searchParams.get("eventTo")).toBe("2026-10-31T23:59:59-03:00");
  });

  it("presenta el error de la API y permite reintentar", async () => {
    servidor.use(http.get("http://localhost:8080/api/v1/opportunities", () => HttpResponse.json({ status: 500, detail: "Falla controlada" }, { status: 500 })));
    renderizar();
    expect(await screen.findByText("No pudimos cargar las oportunidades")).toBeInTheDocument();
    expect(screen.getByText("Falla controlada")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reintentar" })).toBeEnabled();
  });
});
