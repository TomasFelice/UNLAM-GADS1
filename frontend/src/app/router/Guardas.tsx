import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useSesion } from "../../shared/sesion/contextoSesion";

export function GuardaSesion() {
  const { sesion } = useSesion();
  const ubicacion = useLocation();
  if (!sesion) {
    const desde = `${ubicacion.pathname}${ubicacion.search}${ubicacion.hash}`;
    return <Navigate to={`/ingresar?desde=${encodeURIComponent(desde)}`} replace />;
  }
  return <Outlet />;
}

export function GuardaClaveCambiada() {
  const { sesion } = useSesion();
  if (sesion?.debeCambiarClave) {
    return <Navigate to="/app/cambiar-clave" replace />;
  }
  return <Outlet />;
}
