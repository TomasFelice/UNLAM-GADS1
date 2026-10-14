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

  // Una respuesta de la cuenta anterior no debe poblar caché ni expirar la nueva sesión.
  const comprobarSesion = () => {
    if (autenticar && sesion?.token !== obtenerSesion()?.token) {
      throw new DOMException("La sesión cambió durante la consulta.", "AbortError");
    }
  };
  comprobarSesion();

  if (!respuesta.ok) {
    const problema = await interpretarProblema(respuesta);
    comprobarSesion();
    if (respuesta.status === 401 && autenticar) {
      borrarSesion();
      window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA));
    }
    throw new ErrorDeApi(problema);
  }

  if (respuesta.status === 204) return undefined as T;
  const datos = (await respuesta.json()) as T;
  comprobarSesion();
  return datos;
}
