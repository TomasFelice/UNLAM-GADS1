import { Link } from "react-router-dom";
import { BarraFiltros, Filtro, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { pesos } from "../../shared/data/formato";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useServicios } from "./apiServicios";

export function PaginaServicios() {
  const url = useBusquedaUrl();
  const { sesion } = useSesion();
  const consulta = useServicios(url.pagina, { q: url.parametros.get("q") || undefined, active: url.parametros.get("active") || undefined });
  return <><Seo titulo="Servicios adicionales" descripcion="Servicios para eventos corporativos." noIndexar />
    <CabeceraPagina titulo="Servicios adicionales" descripcion="Oferta complementaria y disponibilidad para nuevas negociaciones." acciones={sesion?.usuario.rol === "ADMIN" && <BotonEnlace a="/app/servicios/nuevo" icono="mas">Nuevo servicio</BotonEnlace>} />
    <Tarjeta aSangre><BarraFiltros marcador="Buscar servicios" valor={url.busqueda} alCambiar={url.setBusqueda}><Filtro etiqueta="Estado" valor={url.parametros.get("active") ?? ""} alCambiar={v => url.cambiar("active", v)} opciones={[{ valor: "true", texto: "Activo" }, { valor: "false", texto: "Inactivo" }]} /></BarraFiltros>
      {consulta.isPending ? <EstadoVacio icono="salones" titulo="Cargando servicios…" descripcion="Consultando la oferta." />
        : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar los servicios" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>} />
          : !consulta.data.content?.length ? <EstadoVacio icono="salones" titulo="Sin servicios para mostrar" descripcion="Probá cambiar los filtros." />
            : <><Tabla resumen="Servicios adicionales"><EncabezadoTabla columnas={["Servicio", "Precio", "Estado"]} /><tbody>{consulta.data.content.map(s => <tr key={s.id}><td><Link to={`/app/servicios/${s.id}`}>{s.name}</Link><span className="secundario">{s.description}</span></td><td>{s.price == null ? "Sin especificar" : pesos(s.price)}</td><td>{s.active ? "Activo" : "Inactivo"}</td></tr>)}</tbody></Tabla><TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements ?? 0} entidad="servicios" pagina={url.pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={url.cambiarPagina} /></TarjetaPie></>}
    </Tarjeta></>;
}
