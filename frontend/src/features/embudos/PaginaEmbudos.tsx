import { Link } from "react-router-dom";
import { BarraFiltros, Filtro, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useEmbudos } from "./apiEmbudos";
export function PaginaEmbudos() {
  const url = useBusquedaUrl();
  const consulta = useEmbudos(url.pagina, { q: url.parametros.get("q") || undefined, active: url.parametros.get("active") || undefined });
  return <><Seo titulo="Embudos comerciales" descripcion="Configuración de procesos y etapas comerciales." noIndexar />
    <CabeceraPagina titulo="Embudos comerciales" descripcion="Cada proceso conserva sus propias etapas y oportunidades." migas={[{ texto: "Configuración", a: "/app/configuracion" }, { texto: "Embudos" }]} acciones={<BotonEnlace a="/app/configuracion/embudos/nuevo" icono="mas">Nuevo embudo</BotonEnlace>} />
    <Tarjeta aSangre><BarraFiltros marcador="Buscar embudo" valor={url.busqueda} alCambiar={url.setBusqueda}><Filtro etiqueta="Estado" valor={url.parametros.get("active") ?? ""} alCambiar={v => url.cambiar("active", v)} opciones={[{ valor: "true", texto: "Activo" }, { valor: "false", texto: "Inactivo" }]} /></BarraFiltros>
    {consulta.isPending ? <EstadoVacio icono="embudo" titulo="Cargando embudos…" descripcion="Consultando los procesos de tu organización." /> : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos consultar los embudos" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>} /> : !consulta.data.content?.length ? <EstadoVacio icono="embudo" titulo="Sin embudos para mostrar" descripcion="Probá otra búsqueda o creá un proceso nuevo." /> : <><Tabla resumen="Embudos comerciales"><EncabezadoTabla columnas={["Nombre", "Estado", "Predeterminado"]} /><tbody>{consulta.data.content.map(e => <tr key={e.id}><td><Link to={`/app/configuracion/embudos/${e.id}`}>{e.name}</Link></td><td>{e.active ? "Activo" : "Inactivo"}</td><td>{e.defaultPipeline ? "Sí" : "—"}</td></tr>)}</tbody></Tabla><TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements ?? 0} entidad="embudos" pagina={url.pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={url.cambiarPagina} /></TarjetaPie></>}
    </Tarjeta></>;
}
