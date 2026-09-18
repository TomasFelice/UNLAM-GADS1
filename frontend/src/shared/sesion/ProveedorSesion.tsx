import { useEffect, useState, type ReactNode } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import type { components } from "../api/esquema-api";
import { solicitarApi } from "../api/clienteApi";
import { cacheApi } from "../api/cache";
import {
  borrarSesion,
  EVENTO_SESION_EXPIRADA,
  guardarSesion,
  obtenerSesion,
  type Rol,
  type Sesion,
} from "./almacenSesion";
import { Contexto } from "./contextoSesion";

type Login = components["schemas"]["LoginRequest"];
type RespuestaLogin = components["schemas"]["LoginResponse"];

function convertirLogin(respuesta: RespuestaLogin, mantener: boolean): Sesion {
  const usuario = respuesta.user;
  if (
    !respuesta.accessToken ||
    !respuesta.expiresInSeconds ||
    !usuario?.id ||
    !usuario.email ||
    !usuario.firstName ||
    !usuario.lastName ||
    !["ADMIN", "SALES_MANAGER", "SELLER"].includes(usuario.role ?? "")
  ) {
    throw new Error("El servidor devolvió una sesión incompleta.");
  }
  const sesion: Sesion = {
    token: respuesta.accessToken,
    venceEn: Date.now() + respuesta.expiresInSeconds * 1000,
    debeCambiarClave: respuesta.mustChangePassword === true,
    usuario: {
      id: usuario.id,
      email: usuario.email,
      nombre: usuario.firstName,
      apellido: usuario.lastName,
      rol: usuario.role as Rol,
    },
  };
  guardarSesion(sesion, mantener);
  return sesion;
}

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState(obtenerSesion);
  const navegar = useNavigate();
  const ubicacion = useLocation();

  const cerrar = () => {
    borrarSesion();
    cacheApi.clear();
    setSesion(null);
  };

  useEffect(() => {
    const alExpirar = () => {
      cerrar();
      const desde = `${ubicacion.pathname}${ubicacion.search}${ubicacion.hash}`;
      navegar(`/ingresar?desde=${encodeURIComponent(desde)}`, { replace: true });
    };
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
  });

  const ingresar = async (credenciales: Login, mantener: boolean) => {
    const respuesta = await solicitarApi<RespuestaLogin>("/auth/login", {
      method: "POST",
      body: credenciales,
      autenticar: false,
    });
    const nuevaSesion = convertirLogin(respuesta, mantener);
    setSesion(nuevaSesion);
    return nuevaSesion;
  };

  return <Contexto value={{ sesion, ingresar, cerrar }}>{children}</Contexto>;
}
