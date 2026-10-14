import { QueryClientProvider } from "@tanstack/react-query";
import { act, render, renderHook, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { HttpResponse, delay, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, expect, it } from "vitest";
import { useOportunidad, useOportunidades, useGuardarOportunidad } from "../../features/oportunidades/apiOportunidades";
import { useSalon } from "../../features/salones/apiSalones";
import { useServicio } from "../../features/servicios/apiServicios";
import { guardarSesion, obtenerSesion } from "../sesion/almacenSesion";
import { ProveedorSesion } from "../sesion/ProveedorSesion";
import { useSesion } from "../sesion/contextoSesion";
import { cacheApi } from "./cache";
import { solicitarApi } from "./clienteApi";

const API = "http://localhost:8080/api/v1";
const servidor = setupServer();
beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => { servidor.resetHandlers(); cacheApi.clear(); });
afterAll(() => servidor.close());
const envolver = ({ children }: { children: ReactNode }) => <QueryClientProvider client={cacheApi}>{children}</QueryClientProvider>;

it("separa embudo/filtros/página, cancela búsqueda anterior y reutiliza consultas vigentes", async () => {
  const consultas: string[] = []; let cancelada = false;
  servidor.use(http.get(`${API}/opportunities`, async ({ request }) => {
    const url = new URL(request.url); consultas.push(url.search);
    if (url.searchParams.get("q") === "lenta") { request.signal.addEventListener("abort", () => { cancelada = true; }); await delay(150); }
    return HttpResponse.json({ content: [{ id: Number(url.searchParams.get("pipelineId")), title: url.searchParams.get("q") }], page: Number(url.searchParams.get("page")) });
  }));
  const vista = renderHook(({ pagina, embudo, q }) => useOportunidades(pagina, { pipelineId: embudo, q }), { wrapper: envolver, initialProps: { pagina: 0, embudo: "1", q: "lenta" } });
  await waitFor(() => expect(consultas).toHaveLength(1));
  vista.rerender({ pagina: 1, embudo: "2", q: "vigente" });
  await waitFor(() => expect(vista.result.current.data?.content?.[0].title).toBe("vigente"));
  expect(vista.result.current.data?.content?.[0].id).toBe(2); expect(vista.result.current.data?.page).toBe(1);
  await waitFor(() => expect(cancelada).toBe(true));
  vista.unmount();
  const vuelta = renderHook(() => useOportunidades(1, { q: "vigente", pipelineId: "2" }), { wrapper: envolver });
  expect(vuelta.result.current.data?.content?.[0].title).toBe("vigente"); expect(consultas).toHaveLength(2);
});

it.each([NaN, 0, -1, 1.5])("omite identificadores inválidos: %s", id => {
  const vista = renderHook(() => [useOportunidad(id), useSalon(id), useServicio(id)], { wrapper: envolver });
  expect(vista.result.current.every(consulta => consulta.fetchStatus === "idle")).toBe(true);
});

it("una edición invalida recientes, lista, tablero, historia y relaciones de clientes", async () => {
  servidor.use(http.put(`${API}/opportunities/7`, () => HttpResponse.json({ id: 7 })));
  const claves = [["oportunidades", "recientes", 5], ["oportunidades", "lista", 0, { pipelineId: "2" }], ["oportunidades", "detalle", 7], ["oportunidades", "embudo", { pipelineId: "2" }], ["oportunidades", "historial", 7], ["empresas", "detalle", 8], ["contactos", "detalle", 9]];
  claves.forEach(clave => cacheApi.setQueryData(clave, {}));
  const vista = renderHook(() => useGuardarOportunidad(7), { wrapper: envolver });
  await act(async () => { await vista.result.current.mutateAsync({ title: "Actualizada", version: 0 } as Parameters<typeof vista.result.current.mutateAsync>[0]); });
  claves.forEach(clave => expect(cacheApi.getQueryState(clave)?.isInvalidated).toBe(true));
});

it.each([200, 401])("descarta respuesta tardía %s de otra cuenta sin expirar la vigente", async estado => {
  const anterior = { token: "anterior", venceEn: Date.now() + 60000, debeCambiarClave: false, usuario: { id: 1, nombre: "Ana", apellido: "A", email: "a@fixtures.invalid", rol: "ADMIN" as const } };
  guardarSesion(anterior, false);
  let solicitada = false; let liberar!: () => void;
  const espera = new Promise<void>(resolver => { liberar = resolver; });
  servidor.use(http.get(`${API}/demorada`, async () => { solicitada = true; await espera; return HttpResponse.json({ status: estado, detail: "Anterior" }, { status: estado }); }));
  const respuesta = solicitarApi("/demorada");
  const rechazada = expect(respuesta).rejects.toMatchObject({ name: "AbortError" });
  await waitFor(() => expect(solicitada).toBe(true));
  guardarSesion({ ...anterior, token: "vigente", usuario: { ...anterior.usuario, id: 2 } }, false);
  liberar(); await rechazada; expect(obtenerSesion()?.token).toBe("vigente");
});

it("un nuevo login limpia datos anteriores y el cierre de sesión vacía la caché", async () => {
  servidor.use(http.post(`${API}/auth/login`, () => HttpResponse.json({ accessToken: "cuenta-nueva", expiresInSeconds: 3600, mustChangePassword: false, user: { id: 2, email: "b@fixtures.invalid", firstName: "Bea", lastName: "B", role: "SELLER" } })));
  cacheApi.setQueryData(["oportunidades", "recientes", 5], { content: [{ title: "Cuenta anterior" }] });
  function Acceso() { const sesion = useSesion(); return <><button onClick={() => void sesion.ingresar({ email: "b@fixtures.invalid", password: "Prueba" }, false)}>Ingresar</button><button onClick={() => sesion.cerrar()}>Cerrar</button><p>{sesion.sesion?.usuario.nombre}</p></>; }
  render(<MemoryRouter>{envolver({ children: <ProveedorSesion><Acceso /></ProveedorSesion> })}</MemoryRouter>);
  await userEvent.click(screen.getByRole("button", { name: "Ingresar" }));
  expect(await screen.findByText("Bea")).toBeInTheDocument(); expect(cacheApi.getQueryCache().getAll()).toHaveLength(0);
  cacheApi.setQueryData(["empresas"], {});
  await userEvent.click(screen.getByRole("button", { name: "Cerrar" })); expect(cacheApi.getQueryCache().getAll()).toHaveLength(0); expect(obtenerSesion()).toBeNull();
});
