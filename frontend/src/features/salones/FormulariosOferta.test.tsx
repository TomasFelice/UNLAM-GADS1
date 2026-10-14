import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { PaginaSalonFormulario } from "./PaginaSalonFormulario";
import { PaginaServicioFormulario } from "../servicios/PaginaServicioFormulario";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer();
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());

function renderizar(servicio = false) {
  const cache = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<QueryClientProvider client={cache}><MemoryRouter initialEntries={[servicio ? "/app/servicios/nuevo" : "/app/salones/nuevo"]}><Routes>
    <Route path="/app/salones/nuevo" element={<PaginaSalonFormulario />} /><Route path="/app/servicios/nuevo" element={<PaginaServicioFormulario />} />
  </Routes></MemoryRouter></QueryClientProvider>);
}

describe("errores de validación de oferta", () => {
  it("asocia el error del servidor al equipamiento y conserva los datos del salón", async () => {
    let envio: unknown;
    servidor.use(http.post(`${API}/venues`, async ({ request }) => { envio = await request.json(); return HttpResponse.json({ detail: "Revisá el equipamiento", errors: [{ field: "equipment[0]", message: "Corregí el elemento indicado." }] }, { status: 400 }); }));
    renderizar();
    const persona = userEvent.setup();
    await persona.type(screen.getByRole("textbox", { name: "Nombre" }), "Salón de prueba");
    await persona.type(screen.getByRole("spinbutton", { name: "Capacidad" }), "30");
    await persona.type(screen.getByLabelText("Equipamiento", { exact: true }), "Audio\nProyector");
    await persona.click(screen.getByRole("button", { name: "Crear salón" }));
    expect(await screen.findByText("Corregí el elemento indicado.")).toBeVisible();
    expect(envio).toMatchObject({ name: "Salón de prueba", capacity: 30, equipment: ["Audio", "Proyector"], status: "DISPONIBLE" });
    expect(screen.getByLabelText("Equipamiento", { exact: true })).toHaveValue("Audio\nProyector");
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Salón de prueba");
  });

  it("muestra el error de precio del servicio sin perder el estado elegido", async () => {
    let envio: unknown;
    servidor.use(http.post(`${API}/event-services`, async ({ request }) => { envio = await request.json(); return HttpResponse.json({ detail: "Revisá el precio", errors: [{ field: "price", message: "El importe excede el máximo permitido." }] }, { status: 400 }); }));
    renderizar(true);
    const persona = userEvent.setup();
    await persona.type(screen.getByRole("textbox", { name: "Nombre" }), "Servicio de prueba");
    await persona.type(screen.getByLabelText("Precio", { exact: true }), "200");
    await persona.selectOptions(screen.getByLabelText("Estado", { exact: true }), "false");
    await persona.click(screen.getByRole("button", { name: "Crear servicio" }));
    expect(await screen.findByText("El importe excede el máximo permitido.")).toBeVisible();
    expect(envio).toMatchObject({ name: "Servicio de prueba", price: 200, active: false });
    expect(screen.getByLabelText("Estado", { exact: true })).toHaveValue("false");
    expect(screen.getByLabelText("Precio", { exact: true })).toHaveAccessibleDescription("Opcional, en pesos. El importe excede el máximo permitido.");
  });
});
