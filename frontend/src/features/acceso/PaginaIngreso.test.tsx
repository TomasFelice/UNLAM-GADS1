import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { cacheApi } from "../../shared/api/cache";
import { ProveedorSesion } from "../../shared/sesion/ProveedorSesion";
import { PaginaIngreso } from "./PaginaIngreso";

function renderizar() {
  render(
    <MemoryRouter initialEntries={["/ingresar?desde=/app/empresas"]}>
      <QueryClientProvider client={cacheApi}>
        <ProveedorSesion>
          <Routes>
            <Route path="/ingresar" element={<PaginaIngreso />} />
            <Route path="/app/empresas" element={<h1>Empresas reales</h1>} />
          </Routes>
        </ProveedorSesion>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe("inicio de sesión", () => {
  beforeEach(() => cacheApi.clear());

  it("guarda la respuesta del backend y vuelve a la ruta solicitada", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            accessToken: "jwt-real",
            tokenType: "Bearer",
            expiresInSeconds: 3600,
            mustChangePassword: false,
            user: {
              id: 1,
              email: "admin@ztech.local",
              firstName: "Admin",
              lastName: "zTech",
              role: "ADMIN",
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );
    const usuario = userEvent.setup();
    renderizar();

    await usuario.type(screen.getByLabelText(/Correo electrónico/), "admin@ztech.local");
    await usuario.type(screen.getByLabelText(/Contraseña/), "Admin123!");
    await usuario.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(await screen.findByRole("heading", { name: "Empresas reales" })).toBeInTheDocument();
    expect(sessionStorage.getItem("ztech.crm.sesion")).toContain("jwt-real");
  });

  it("muestra el detalle RFC 7807 cuando las credenciales fallan", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({ status: 401, detail: "Credenciales inválidas.", code: "UNAUTHENTICATED" }),
          { status: 401, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    const usuario = userEvent.setup();
    renderizar();

    await usuario.type(screen.getByLabelText(/Correo electrónico/), "admin@ztech.local");
    await usuario.type(screen.getByLabelText(/Contraseña/), "incorrecta");
    await usuario.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Credenciales inválidas.");
    expect(sessionStorage).toHaveLength(0);
  });
});
