import { URL_API } from "./configuracion";
import { ErrorDeApi, interpretarProblema } from "./errores";
import {
  borrarSesion,
  EVENTO_SESION_EXPIRADA,
  obtenerSesion,
} from "../sesion/almacenSesion";

type Opciones = Omit<RequestInit, "body"> & {
  body?: unknown;
  autenticar?: boolean;
};

export async function solicitarApi<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const { body, autenticar = true, headers, ...resto } = opciones;
  const sesion = obtenerSesion();
  const respuesta = await fetch(`${URL_API}${ruta}`, {
    ...resto,
    headers: {
      Accept: "application/json",
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      ...(autenticar && sesion ? { Authorization: `Bearer ${sesion.token}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!respuesta.ok) {
    const problema = await interpretarProblema(respuesta);
    if (respuesta.status === 401 && autenticar) {
      borrarSesion();
      window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA));
    }
    throw new ErrorDeApi(problema);
  }

  if (respuesta.status === 204) return undefined as T;
  return (await respuesta.json()) as T;
}
