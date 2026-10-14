import { Link } from "react-router-dom";
import { BarraFiltros, Filtro, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { NOMBRES_ROLES, useUsuarios } from "./apiUsuarios";

export function PaginaUsuarios() {
  const url = useBusquedaUrl();
  const filtros = { q: url.parametros.get("q") || undefined, role: url.parametros.get("role") || undefined,
    active: url.parametros.get("active") || undefined };
  const consulta = useUsuarios(url.pagina, filtros);
  return <>
    <Seo titulo="Usuarios" descripcion="Administración del equipo y sus permisos." noIndexar />
    <CabeceraPagina titulo="Usuarios" descripcion="Organizá el equipo y los permisos de acceso al CRM."
      acciones={<BotonEnlace a="/app/usuarios/nuevo" icono="mas">Nuevo usuario</BotonEnlace>} />
    <Tarjeta aSangre>
      <BarraFiltros marcador="Buscar por nombre o correo" valor={url.busqueda} alCambiar={url.setBusqueda}>
        <Filtro etiqueta="Rol" valor={filtros.role} alCambiar={valor => url.cambiar("role", valor)}
          opciones={Object.entries(NOMBRES_ROLES).map(([valor, texto]) => ({ valor, texto }))} />
        <Filtro etiqueta="Estado" valor={filtros.active} alCambiar={valor => url.cambiar("active", valor)}
          opciones={[{ valor: "true", texto: "Activo" }, { valor: "false", texto: "Inactivo" }]} />
      </BarraFiltros>
      {consulta.isPending ? <EstadoVacio icono="usuario" titulo="Cargando usuarios…" descripcion="Consultando el equipo." />
        : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar los usuarios" descripcion={consulta.error.message}
          accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} />
        : !consulta.data.content?.length ? <EstadoVacio icono="usuario" titulo="No hay usuarios para mostrar" descripcion="Probá cambiar los filtros o agregá un integrante." />
        : <>
          <Tabla resumen="Listado de usuarios">
            <EncabezadoTabla columnas={["Nombre", "Correo electrónico", "Rol", "Estado", "Contraseña"]} />
            <tbody>{consulta.data.content.map(usuario => <tr key={usuario.id}>
              <td><Link to={`/app/usuarios/${usuario.id}`}>{usuario.firstName} {usuario.lastName}</Link></td>
              <td>{usuario.email}</td><td>{usuario.role ? NOMBRES_ROLES[usuario.role] : "—"}</td>
              <td><Etiqueta tono={usuario.active ? "exito" : "neutro"}>{usuario.active ? "Activo" : "Inactivo"}</Etiqueta></td>
              <td>{usuario.mustChangePassword ? "Cambio obligatorio" : "Personal"}</td>
            </tr>)}</tbody>
          </Tabla>
          <TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements ?? 0}
            entidad="usuarios" pagina={url.pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={url.cambiarPagina} /></TarjetaPie>
        </>}
    </Tarjeta>
  </>;
}
