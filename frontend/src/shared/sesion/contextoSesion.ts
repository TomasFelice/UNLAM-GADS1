import { createContext, useContext } from "react";
import type { components } from "../api/esquema-api";
import type { Sesion } from "./almacenSesion";

type Login = components["schemas"]["LoginRequest"];

export type ContextoSesion = {
  sesion: Sesion | null;
  ingresar: (credenciales: Login, mantener: boolean) => Promise<Sesion>;
  cerrar: () => void;
};

export const Contexto = createContext<ContextoSesion | null>(null);

export function useSesion() {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error("useSesion debe usarse dentro de ProveedorSesion.");
  return contexto;
}
