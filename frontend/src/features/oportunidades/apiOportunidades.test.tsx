import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { clavesOportunidades, useCambiarEtapa } from "./apiOportunidades";

const servidor = setupServer(http.post("http://localhost:8080/api/v1/opportunities/7/stage", async ({ request }) => {
  const cuerpo = await request.json();
  return HttpResponse.json({ id: 7, title: "Evento anual", stageId: (cuerpo as { stageId: number }).stageId });
}));

beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());

describe("mutación de etapa", () => {
  it("invalida listado, embudo, detalle, historial y actividades", async () => {
    const cache = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const claves = [
      clavesOportunidades.lista(0, {}),
      clavesOportunidades.embudo({}),
      clavesOportunidades.detalle(7),
      clavesOportunidades.historial(7),
      ["actividades", "oportunidad", 7],
    ] as const;
    claves.forEach((clave) => cache.setQueryData(clave, {}));
    const envoltorio = ({ children }: { children: ReactNode }) => <QueryClientProvider client={cache}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => useCambiarEtapa(), { wrapper: envoltorio });

    result.current.mutate({ id: 7, stageId: 4 });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    claves.forEach((clave) => expect(cache.getQueryState(clave)?.isInvalidated).toBe(true));
  });
});
