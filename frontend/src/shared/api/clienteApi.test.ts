import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { ErrorDeApi } from "./errores";
import { solicitarApi } from "./clienteApi";

const servidor = setupServer();

beforeAll(() => servidor.listen({ onUnhandledRequest: "error" }));
afterEach(() => servidor.resetHandlers());
afterAll(() => servidor.close());

describe("errores del contrato HTTP", () => {
  it.each([400, 409, 422])("conserva ProblemDetail para el estado %s", async (estado) => {
    servidor.use(http.post("http://localhost:8080/api/v1/prueba", () => HttpResponse.json({
      status: estado,
      detail: `Problema ${estado}`,
      code: "VALIDATION_ERROR",
      errors: [{ field: "title", message: "Dato inválido" }],
    }, { status: estado })));

    const promesa = solicitarApi("/prueba", { method: "POST", body: { title: "" }, autenticar: false });
    await expect(promesa).rejects.toBeInstanceOf(ErrorDeApi);
    await expect(promesa).rejects.toMatchObject({
      problema: { status: estado, detail: `Problema ${estado}`, errors: [{ field: "title", message: "Dato inválido" }] },
    });
  });
});
