import { useSesion } from "./contextoSesion";

/** DP-02: la consulta por oportunidad propia no habilita la edición del cliente. */
export function usePuedeEditarCliente(responsableId?: number) {
  const { sesion } = useSesion();
  if (!sesion) return false;
  return sesion.usuario.rol === "ADMIN" || sesion.usuario.rol === "SALES_MANAGER"
    || sesion.usuario.id === responsableId;
}
