import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, renderHook, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, expect, it } from "vitest";
import { HistorialComercial } from "./HistorialComercial";
import { HistorialEtapas } from "./HistorialEtapas";
import { useActividades, useRegistrarActividad } from "./apiActividades";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer();
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());
function entorno() {
  const cache = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return { cache, wrapper: ({ children }: { children: React.ReactNode }) => <QueryClientProvider client={cache}>{children}</QueryClientProvider> };
}
function tipos() {
  servidor.use(http.get(`${API}/activity-types`, () => HttpResponse.json({ content: [{ id: 1, name: "Llamada", active: true }], totalPages: 1 })),
    http.get(`${API}/activity-types/1`, () => HttpResponse.json({ id: 1, name: "Llamada", active: true })));
}

it("navega más de 20 hechos sin pedir todas las páginas y distingue ocurrencia y registro", async () => {
  const paginas: string[] = [];
  servidor.use(http.get(`${API}/contacts/7/activities`, ({ request }) => {
    const p = new URL(request.url).searchParams.get("page")!; paginas.push(p);
    return HttpResponse.json({ content: Array.from({ length: p === "0" ? 20 : 7 }, (_, i) => ({ id: Number(p) * 20 + i, description: `Hecho ${Number(p) * 20 + i}`, result: "Confirmado", occurredAt: "2026-01-01T12:00:00Z", createdAt: "2026-02-01T12:00:00Z", type: { name: "Llamada histórica", active: false }, author: { firstName: "Autor", lastName: "Inactivo" } })), totalElements: 27, totalPages: 2 });
  }));
  render(<HistorialComercial asociacion={{ tipo: "contacts", id: 7 }} puedeRegistrar={false} />, entorno());
  expect(await screen.findByText("Mostrando 20 de 27 actividades")).toBeInTheDocument();
  expect(paginas).toEqual(["0"]);
  await userEvent.click(screen.getByRole("button", { name: "Página siguiente" }));
  expect(await screen.findByText("Mostrando 7 de 27 actividades")).toBeInTheDocument();
  expect(screen.getByText("Hecho 26", { exact: false })).toBeInTheDocument();
  expect(screen.getAllByText(/Fecha de registro:/)).toHaveLength(7);
  expect(screen.getAllByText(/Autor Inactivo/)).toHaveLength(7);
  expect(screen.getByRole("button", { name: "Página siguiente" })).toBeDisabled();
  expect(screen.queryByRole("button", { name: "Registrar actividad" })).not.toBeInTheDocument();
});

it("valida fecha y campos, conserva valores ante error y registra sólo el contexto de oportunidad", async () => {
  tipos(); const envios: Record<string, unknown>[] = [];
  servidor.use(http.get(`${API}/opportunities/8/activities`, () => HttpResponse.json({ content: [], totalElements: 0, totalPages: 0 })),
    http.post(`${API}/activities`, async ({ request }) => {
      envios.push(await request.json() as Record<string, unknown>);
      return envios.length === 1 ? HttpResponse.json({ detail: "El tipo seleccionado se desactivó.", errors: [{ field: "typeId", message: "Elegí otro tipo." }] }, { status: 422 }) : HttpResponse.json({ id: 9 }, { status: 201 });
    }));
  render(<HistorialComercial asociacion={{ tipo: "opportunities", id: 8 }} puedeRegistrar />, entorno());
  const persona = userEvent.setup();
  await persona.click(screen.getByRole("button", { name: "Registrar actividad" }));
  await persona.click(screen.getByRole("button", { name: "Guardar actividad" }));
  expect(await screen.findByText("Revisá los campos indicados antes de guardar.")).toBeInTheDocument();
  await persona.click(screen.getByRole("combobox", { name: "Tipo de actividad" }));
  await persona.click(await screen.findByRole("option", { name: "Llamada" }));
  const fecha = screen.getByLabelText(/Fecha y hora del hecho/);
  // datetime-local no admite userEvent.type de forma portable en jsdom.
  const { fireEvent } = await import("@testing-library/react");
  fireEvent.change(fecha, { target: { value: "2099-01-01T09:00" } });
  await persona.type(screen.getByRole("textbox", { name: "Descripción" }), "Conversación realizada");
  await persona.type(screen.getByRole("textbox", { name: "Resultado" }), "Interés confirmado");
  await persona.click(screen.getByRole("button", { name: "Guardar actividad" }));
  expect(envios).toHaveLength(0);
  fireEvent.change(fecha, { target: { value: "2026-01-01T09:00" } });
  await persona.click(screen.getByRole("button", { name: "Guardar actividad" }));
  expect(await screen.findByText("El tipo seleccionado se desactivó.")).toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: "Descripción" })).toHaveValue("Conversación realizada");
  await persona.click(screen.getByRole("button", { name: "Guardar actividad" }));
  expect(await screen.findByText("Actividad registrada.")).toBeInTheDocument();
  expect(envios[1]).toEqual({ opportunityId: 8, typeId: 1, occurredAt: "2026-01-01T12:00:00.000Z", description: "Conversación realizada", result: "Interés confirmado" });
  expect(screen.queryByRole("button", { name: /Editar actividad|Eliminar actividad/ })).not.toBeInTheDocument();
});

it("invalida históricos y clientes afectados y no consulta una asociación inválida", async () => {
  servidor.use(http.post(`${API}/activities`, () => HttpResponse.json({ id: 9 }, { status: 201 })));
  const { cache, wrapper } = entorno();
  const claves = [["actividades", "global"], ["actividades", "companies", 7], ["actividades", "contacts", 8], ["actividades", "opportunities", 9], ["empresas"], ["contactos"], ["oportunidades"]];
  claves.forEach(clave => cache.setQueryData(clave, {}));
  const { result } = renderHook(() => useRegistrarActividad(), { wrapper });
  result.current.mutate({ typeId: 1, occurredAt: "2026-01-01T12:00:00Z", description: "Hecho", result: "Resultado", companyId: 7 });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
  claves.forEach(clave => expect(cache.getQueryState(clave)?.isInvalidated).toBe(true));
  const consulta = renderHook(() => useActividades({ tipo: "contacts", id: NaN }), { wrapper });
  expect(consulta.result.current.fetchStatus).toBe("idle");
});

it("reintenta el historial y reconstruye cambios, notas, autores y los dos cierres", async () => {
  let intento = 0;
  const tipos = ["CREATED", "STAGE_CHANGED", "WON", "REOPENED", "LOST"];
  servidor.use(http.get(`${API}/opportunities/7/stage-history`, () => ++intento === 1 ? HttpResponse.json({ detail: "Falla temporal" }, { status: 503 }) : HttpResponse.json(tipos.map((eventType, i) => ({ id: i + 1, eventType, fromStage: i ? { name: "Anterior" } : null, toStage: { name: `Etapa ${i}` }, changedAt: "2026-01-01T12:00:00Z", author: { firstName: "Ana", lastName: "Histórica" }, note: `Nota ${i}`, finalValue: eventType === "WON" ? 1234.5 : null, lossReasonName: eventType === "LOST" ? "Motivo original" : null, closedAt: ["WON", "LOST"].includes(eventType) ? "2026-01-01T12:00:00Z" : null })))));
  render(<HistorialEtapas oportunidadId={7} />, entorno());
  expect(await screen.findByText("Falla temporal")).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(await screen.findByText(/Ganada · Valor final:/)).toHaveTextContent("1.234,50");
  expect(screen.getByText(/Perdida · Motivo:/)).toHaveTextContent("Motivo original");
  expect(screen.getAllByText("Ana Histórica")).toHaveLength(5);
  expect(screen.getByText("Nota 3")).toBeInTheDocument();
  expect(screen.getByText("Alta en Etapa 0")).toBeInTheDocument();
});
