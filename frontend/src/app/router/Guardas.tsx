import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useSesion } from "../../shared/sesion/contextoSesion";
import type { Rol } from "../../shared/sesion/almacenSesion";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { BotonEnlace } from "../../shared/components/Boton";
import { Seo } from "../../shared/seo/Seo";

export function GuardaSesion() {
  const { sesion, salida } = useSesion();
  const ubicacion = useLocation();
  if (!sesion) {
    if (salida) {
      return <Navigate to={salida.desde ? `/ingresar?desde=${encodeURIComponent(salida.desde)}` : "/ingresar"}
        state={salida.mensaje ? { mensaje: salida.mensaje } : undefined} replace />;
    }
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

export function GuardaRol({ roles }: { roles: readonly Rol[] }) {
  const { sesion } = useSesion();
  if (!sesion || !roles.includes(sesion.usuario.rol)) {
    return <>
      <Seo titulo="Acceso denegado" descripcion="Esta sección requiere permisos de administración." noIndexar />
      <EstadoVacio icono="usuario" titulo="Acceso denegado"
        descripcion="Tu rol no permite administrar esta sección."
        accion={<BotonEnlace a="/app" variante="secundario">Volver al panel</BotonEnlace>} />
    </>;
  }
  return <Outlet />;
}
