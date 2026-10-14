import { useParams } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { NOMBRES_ROLES, useUsuario } from "./apiUsuarios";
import { RestablecerClave } from "./RestablecerClave";
import css from "./Usuarios.module.css";

export function PaginaUsuarioDetalle() {
  const id = Number(useParams().id);
  const valido = Number.isSafeInteger(id) && id > 0;
  const consulta = useUsuario(valido ? id : undefined);
  const usuario = consulta.data;
  return <>
    <Seo titulo="Usuario" descripcion="Perfil y permisos de acceso." noIndexar />
    {!valido ? <EstadoVacio icono="usuario" titulo="Enlace de usuario inválido" descripcion="Volvé al listado para seleccionar un usuario."
      accion={<BotonEnlace a="/app/usuarios">Volver a usuarios</BotonEnlace>} />
      : consulta.isPending ? <EstadoVacio icono="usuario" titulo="Cargando usuario…" descripcion="Consultando el perfil." />
      : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos abrir el usuario" descripcion={consulta.error.message}
        accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} />
      : usuario && <>
        <CabeceraPagina titulo={`${usuario.firstName} ${usuario.lastName}`} descripcion={usuario.email ?? ""}
          migas={[{ texto: "Usuarios", a: "/app/usuarios" }, { texto: "Detalle" }]}
          acciones={<BotonEnlace a={`/app/usuarios/${id}/editar`} variante="secundario" icono="lapiz">Editar usuario</BotonEnlace>} />
        <div className={css.pila}><Tarjeta><TarjetaCuerpo><ListaDatos datos={[
          { etiqueta: "Rol", valor: usuario.role ? NOMBRES_ROLES[usuario.role] : "—" },
          { etiqueta: "Estado", valor: usuario.active ? "Activo" : "Inactivo" },
          { etiqueta: "Contraseña", valor: usuario.mustChangePassword ? "Debe cambiar la contraseña temporal" : "Contraseña personal" },
        ]} /></TarjetaCuerpo></Tarjeta><RestablecerClave usuarioId={id} /></div>
      </>}
  </>;
}
