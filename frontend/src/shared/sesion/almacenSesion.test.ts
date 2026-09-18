import { describe, expect, it } from "vitest";
import { borrarSesion, guardarSesion, obtenerSesion, type Sesion } from "./almacenSesion";

const sesion: Sesion = {
  token: "token-prueba",
  venceEn: Date.now() + 60_000,
  debeCambiarClave: false,
  usuario: {
    id: 7,
    email: "ana@ztech.local",
    nombre: "Ana",
    apellido: "Pérez",
    rol: "SELLER",
  },
};

describe("almacén de sesión", () => {
  it("usa sessionStorage de manera predeterminada", () => {
    guardarSesion(sesion, false);

    expect(sessionStorage).toHaveLength(1);
    expect(localStorage).toHaveLength(0);
    expect(obtenerSesion()).toEqual(sesion);
  });

  it("usa localStorage al mantener la sesión", () => {
    guardarSesion(sesion, true);

    expect(localStorage).toHaveLength(1);
    expect(sessionStorage).toHaveLength(0);
  });

  it("descarta una sesión vencida", () => {
    guardarSesion({ ...sesion, venceEn: Date.now() - 1 }, false);

    expect(obtenerSesion()).toBeNull();
    expect(sessionStorage).toHaveLength(0);
  });

  it("borra la sesión de ambos almacenes", () => {
    guardarSesion(sesion, false);
    borrarSesion();

    expect(obtenerSesion()).toBeNull();
  });
});
