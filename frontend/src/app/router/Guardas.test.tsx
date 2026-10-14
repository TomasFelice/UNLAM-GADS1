import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { Contexto } from "../../shared/sesion/contextoSesion";
import type { Rol, Sesion } from "../../shared/sesion/almacenSesion";
import { LayoutApp } from "../layouts/LayoutApp";
import { GuardaClaveCambiada, GuardaRol, GuardaSesion } from "./Guardas";

function renderizar(rol: Rol | null, debeCambiarClave = false) {
  const sesion: Sesion | null = rol ? {
    token: "fixture", venceEn: Date.now() + 60_000, debeCambiarClave,
    usuario: { id: 1, email: "fixture@example.invalid", nombre: "Ana", apellido: "Prueba", rol },
  } : null;
  render(<MemoryRouter initialEntries={["/app/usuarios"]}>
    <Contexto.Provider value={{ sesion, ingresar: vi.fn(), cerrar: vi.fn() }}>
      <Routes>
        <Route path="/ingresar" element={<h1>Ingresar</h1>} />
        <Route element={<GuardaSesion />}>
          <Route path="/app/cambiar-clave" element={<h1>Cambiar contraseña</h1>} />
          <Route element={<GuardaClaveCambiada />}>
            <Route path="/app" element={<LayoutApp />}>
              <Route element={<GuardaRol roles={["ADMIN"]} />}>
                <Route path="usuarios" element={<h1>Administrar usuarios</h1>} />
              </Route>
            </Route>
          </Route>
        </Route>
      </Routes>
    </Contexto.Provider>
  </MemoryRouter>);
}

describe("acceso administrativo", () => {
  it("permite URL y menú al administrador", () => {
    renderizar("ADMIN");
    expect(screen.getByRole("heading", { name: "Administrar usuarios" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Usuarios" })).toHaveAttribute("href", "/app/usuarios");
  });

  it.each(["SELLER", "SALES_MANAGER"] as const)("rechaza URL y oculta menú a %s", rol => {
    renderizar(rol);
    expect(screen.getByRole("heading", { name: "Acceso denegado" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Usuarios" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Administrar usuarios" })).not.toBeInTheDocument();
  });

  it("mantiene el cambio de contraseña por delante de la guarda por rol", () => {
    renderizar("ADMIN", true);
    expect(screen.getByRole("heading", { name: "Cambiar contraseña" })).toBeInTheDocument();
  });

  it("mantiene el login cuando no hay sesión", () => {
    renderizar(null);
    expect(screen.getByRole("heading", { name: "Ingresar" })).toBeInTheDocument();
  });
});
