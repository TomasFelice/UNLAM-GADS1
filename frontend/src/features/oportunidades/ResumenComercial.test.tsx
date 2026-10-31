import { QueryClientProvider } from "@tanstack/react-query";
import { act, render, renderHook, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { StrictMode, type ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, beforeEach, expect, it, vi } from "vitest";
import { cacheApi } from "../../shared/api/cache";
import { actualizarContextoComercial } from "../../shared/api/revisionesComerciales";
import { borrarSesion, guardarSesion } from "../../shared/sesion/almacenSesion";
import { useResumenComercial, type ResumenComercialRespuesta } from "./apiResumenComercial";
import { ResumenComercial } from "./ResumenComercial";
import { useOportunidad, useGuardarOportunidad, useCambiarEtapa, useAccionOportunidad, type SolicitudEditarOportunidad } from "./apiOportunidades";
import { useRegistrarActividad, type SolicitudActividad } from "../actividades/apiActividades";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer();
const sesion = { token: "sesion-ficticia", venceEn: Date.now() + 86400000, debeCambiarClave: false,
  usuario: { id: 1, nombre: "Ana", apellido: "Prueba", email: "ana@fixtures.invalid", rol: "SELLER" as const } };
const resultado: ResumenComercialRespuesta = { opportunityId: 7, opportunityVersion: 0,
  generatedAt: "2026-10-31T12:00:00Z", contextAsOf: "2026-10-31T12:00:00Z",
  coverage: { totalActivities: 70, includedActivities: 10, totalTransitions: 2, includedTransitions: 2, partial: true, textTruncated: true },
  sections: { clientNeeds: [{ text: "Necesidad ficticia <script>instrucción</script>", sourceIds: ["A1"] }], currentSituation: [], agreementsAndObjections: [], unconfirmedInformation: [] },
  sources: [{ sourceId: "A1", type: "ACTIVITY", originalId: 88, title: "Actividad fuera de página", occurredAt: "2026-01-01T12:00:00Z",
    recordedAt: "2026-01-02T12:00:00Z", fragments: { description: "Fragmento exacto <img src=x>", result: "Acuerdo ficticio" }, textTruncated: true }],
};
const envolver = ({ children }: { children: ReactNode }) => <StrictMode><QueryClientProvider client={cacheApi}>{children}</QueryClientProvider></StrictMode>;
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
beforeEach(() => guardarSesion(sesion, false));
afterEach(() => { servidor.resetHandlers(); cacheApi.clear(); borrarSesion(); vi.useRealTimers(); });
afterAll(() => servidor.close());

it("revalida acceso aun con detalle en caché y no muestra el resumen si el permiso se pierde al volver", async () => {
  let permiso = true; let resumenes = 0; let lecturas = 0;
  servidor.use(http.get(`${API}/opportunities/:id`, ({ params }) => {
    lecturas++; return permiso ? HttpResponse.json({ id: Number(params.id), version: 0 }) : HttpResponse.json({ detail: "No encontrada" }, { status: 404 });
  }), http.post(`${API}/opportunities/:id/commercial-summary`, ({ params }) => { resumenes++; return HttpResponse.json({ ...resultado, opportunityId: Number(params.id) }); }));
  const vista = renderHook(({ id }) => {
    const detalle = useOportunidad(id);
    return useResumenComercial(id, detalle.data?.version ?? 0, detalle.isSuccess && detalle.isFetchedAfterMount && !detalle.isFetching);
  }, { wrapper: envolver, initialProps: { id: 7 } });
  await waitFor(() => expect(vista.result.current.data?.opportunityId).toBe(7));
  vista.rerender({ id: 8 }); await waitFor(() => expect(vista.result.current.data?.opportunityId).toBe(8));
  permiso = false; vista.rerender({ id: 7 });
  await waitFor(() => expect(lecturas).toBeGreaterThanOrEqual(3));
  expect(vista.result.current.contextoPendiente).toBe(true); expect(resumenes).toBe(2);
});

it("genera sólo después de autorización y comparte solicitud bajo StrictMode y remontajes", async () => {
  let llamadas = 0; let liberar!: () => void;
  const pendiente = new Promise<void>(r => { liberar = r; });
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, async () => { llamadas++; await pendiente; return HttpResponse.json(resultado); }));
  const vista = renderHook(({ autorizado }) => useResumenComercial(7, 0, autorizado), { wrapper: envolver, initialProps: { autorizado: false } });
  expect(llamadas).toBe(0); vista.rerender({ autorizado: true });
  await waitFor(() => expect(llamadas).toBe(1)); vista.unmount();
  const vuelta = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  liberar(); await waitFor(() => expect(vuelta.result.current.data).toEqual(resultado)); expect(llamadas).toBe(1);
});

it("reutiliza cinco minutos al volver, vence sólo al entrar y no genera por foco, reconexión o tiempo", async () => {
  vi.useFakeTimers({ toFake: ["Date"] }); let llamadas = 0;
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => { llamadas++; return HttpResponse.json(resultado); }));
  const vista = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(vista.result.current.isSuccess).toBe(true)); vista.unmount();
  const vuelta = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  expect(vuelta.result.current.data).toEqual(resultado); expect(llamadas).toBe(1);
  act(() => { vi.setSystemTime(Date.now() + 300001); window.dispatchEvent(new Event("focus")); window.dispatchEvent(new Event("online")); });
  expect(llamadas).toBe(1); vuelta.unmount();
  const vencida = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(llamadas).toBe(2)); await waitFor(() => expect(vencida.result.current.isFetching).toBe(false));
});

it("retiene errores al remontar y sólo reintenta manualmente, sin duplicar el doble clic", async () => {
  let llamadas = 0;
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => { llamadas++; return llamadas === 1 ? HttpResponse.json({ code: "AI_UNAVAILABLE", detail: "Proveedor no disponible" }, { status: 503 }) : HttpResponse.json(resultado); }));
  const vista = render(<ResumenComercial oportunidadId={7} version={0} autorizado />, { wrapper: envolver });
  await screen.findByText("Proveedor no disponible"); vista.unmount();
  render(<ResumenComercial oportunidadId={7} version={0} autorizado />, { wrapper: envolver });
  expect(llamadas).toBe(1); await userEvent.dblClick(screen.getByRole("button", { name: "Reintentar" }));
  await screen.findByText(/Necesidad ficticia/); expect(llamadas).toBe(2);
});

it("muestra fuentes completas fuera de página, secciones vacías y texto escapado con controles de teclado", async () => {
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => HttpResponse.json(resultado)));
  const vista = render(<ResumenComercial oportunidadId={7} version={0} autorizado />, { wrapper: envolver });
  await screen.findByText(/Necesidad ficticia/); expect(screen.getAllByText("No consta en la información analizada.")).toHaveLength(3);
  const fuente = screen.getByText("Fuente A1: Actividad fuera de página"); fuente.focus(); await userEvent.click(fuente);
  expect(fuente.closest("details")).toHaveAttribute("open"); expect(fuente).toHaveFocus();
  expect(screen.getByText("Fragmento exacto <img src=x>")).toBeVisible(); expect(vista.container.querySelector("script, img")).toBeNull();
  expect(screen.getByText(/Cobertura parcial/)).toBeVisible();
});

it("serializa una actualización tras una mutación durante la generación y descarta el resultado viejo", async () => {
  let llamadas = 0; let liberar!: () => void;
  const pendiente = new Promise<void>(r => { liberar = r; });
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, async () => { llamadas++; if (llamadas === 1) await pendiente; return HttpResponse.json({ ...resultado, generatedAt: String(llamadas) }); }));
  const vista = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(llamadas).toBe(1));
  await act(async () => { await actualizarContextoComercial(cacheApi, 7, async () => undefined); });
  expect(llamadas).toBe(1); liberar();
  await waitFor(() => expect(vista.result.current.data?.generatedAt).toBe("2")); expect(llamadas).toBe(2);
});

it.each(["actividad", "edición", "assign", "etapa", "win", "lose", "reopen"] as const)("la mutación %s actualiza sólo el detalle activo", async tipo => {
  let llamadas = 0; servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => { llamadas++; return HttpResponse.json(resultado); }));
  servidor.use(http.put(`${API}/opportunities/7`, () => HttpResponse.json({ id: 7 })),
    http.post(`${API}/activities`, () => HttpResponse.json({ id: 99, opportunityId: 7 })),
    http.post(`${API}/opportunities/7/:accion`, () => HttpResponse.json({ id: 7 })));
  // Registrar primero la ruta específica para no interceptar el resumen como acción genérica.
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => { llamadas++; return HttpResponse.json(resultado); }));
  const vista = renderHook(() => ({ resumen: useResumenComercial(7, 0, true), guardar: useGuardarOportunidad(7),
    actividad: useRegistrarActividad(), etapa: useCambiarEtapa(), accion: useAccionOportunidad(7) }), { wrapper: envolver });
  await waitFor(() => expect(vista.result.current.resumen.isSuccess).toBe(true));
  await act(async () => {
    const actual = vista.result.current;
    if (tipo === "actividad") await actual.actividad.mutateAsync({ opportunityId: 7 } as SolicitudActividad);
    else if (tipo === "edición") await actual.guardar.mutateAsync({ title: "Nueva", version: 0 } as SolicitudEditarOportunidad);
    else if (tipo === "etapa") await actual.etapa.mutateAsync({ id: 7, stageId: 3, version: 0 });
    else await actual.accion.mutateAsync({ accion: tipo, datos: { version: 0, salesRepId: 2 } });
  });
  await waitFor(() => expect(llamadas).toBe(2)); await waitFor(() => expect(vista.result.current.resumen.isSuccess).toBe(true));
  vista.unmount(); await actualizarContextoComercial(cacheApi, 7, async () => undefined); expect(llamadas).toBe(2);
});

it("una respuesta tardía no repuebla la caché al cambiar de sesión", async () => {
  let liberar!: () => void; let solicitada = false; const pendiente = new Promise<void>(r => { liberar = r; });
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, async () => { solicitada = true; await pendiente; return HttpResponse.json(resultado); }));
  const vista = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(solicitada).toBe(true)); vista.unmount();
  guardarSesion({ ...sesion, token: "otra-sesion", usuario: { ...sesion.usuario, id: 2 } }, false); cacheApi.clear(); liberar();
  await new Promise(r => setTimeout(r, 30)); expect(cacheApi.getQueryCache().findAll({ queryKey: ["resumen-comercial"] })).toHaveLength(0);
});

it("al cambiar de ruta una respuesta completa sólo la caché de origen y al recargar se genera de nuevo", async () => {
  let liberar!: () => void; const pendiente = new Promise<void>(r => { liberar = r; }); let llamadas = 0;
  servidor.use(http.post(`${API}/opportunities/:id/commercial-summary`, async ({ params }) => {
    llamadas++; if (params.id === "7" && llamadas === 1) await pendiente;
    return HttpResponse.json({ ...resultado, opportunityId: Number(params.id) });
  }));
  const vista = renderHook(({ id }) => useResumenComercial(id, 0, true), { wrapper: envolver, initialProps: { id: 7 } });
  await waitFor(() => expect(llamadas).toBe(1)); vista.rerender({ id: 8 });
  await waitFor(() => expect(vista.result.current.data?.opportunityId).toBe(8)); liberar();
  await waitFor(() => expect(cacheApi.getQueryCache().findAll({ queryKey: ["resumen-comercial", 7] })[0]?.state.data).toBeDefined());
  expect(vista.result.current.data?.opportunityId).toBe(8);
  vista.rerender({ id: 7 }); await waitFor(() => expect(vista.result.current.data?.opportunityId).toBe(7)); expect(llamadas).toBe(2);
  vista.unmount(); cacheApi.clear();
  const recarga = renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(recarga.result.current.isSuccess).toBe(true)); expect(llamadas).toBe(3);
});

it("una cuota con espera bloquea el reintento hasta su vencimiento", async () => {
  let llamadas = 0;
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => { llamadas++; return HttpResponse.json({ code: "AI_RATE_LIMITED", detail: "Esperar cuota", retryAfterSeconds: 1 }, { status: 429 }); }));
  render(<ResumenComercial oportunidadId={7} version={0} autorizado />, { wrapper: envolver });
  await screen.findByText("Esperar cuota"); const boton = screen.getByRole("button", { name: "Reintentar" });
  expect(boton).toBeDisabled(); await userEvent.click(boton); expect(llamadas).toBe(1);
  await waitFor(() => expect(boton).toBeEnabled(), { timeout: 1500 });
});

it("401 conserva el cierre de sesión vigente y función apagada no ofrece reintento", async () => {
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => HttpResponse.json({ code: "AI_NOT_CONFIGURED", detail: "IA apagada" }, { status: 503 })));
  const vista = render(<ResumenComercial oportunidadId={7} version={0} autorizado />, { wrapper: envolver });
  await screen.findByText("IA apagada"); expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull(); vista.unmount(); cacheApi.clear();
  servidor.use(http.post(`${API}/opportunities/7/commercial-summary`, () => HttpResponse.json({ code: "UNAUTHENTICATED", detail: "Sin sesión" }, { status: 401 })));
  const expirada = vi.fn(); window.addEventListener("ztech:sesion-expirada", expirada);
  renderHook(() => useResumenComercial(7, 0, true), { wrapper: envolver });
  await waitFor(() => expect(expirada).toHaveBeenCalledOnce()); window.removeEventListener("ztech:sesion-expirada", expirada);
});
