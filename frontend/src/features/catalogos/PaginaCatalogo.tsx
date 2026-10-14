import { Link } from "react-router-dom";
import { BarraFiltros, Filtro, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { CATALOGOS, useCatalogo, type Catalogo } from "./apiCatalogos";

export function PaginaCatalogo({ catalogo }: { catalogo: Catalogo }) {
  const url = useBusquedaUrl();
  const config = CATALOGOS[catalogo];
  const consulta = useCatalogo(catalogo, url.pagina, { q: url.parametros.get("q") || undefined, active: url.parametros.get("active") || undefined });
  const ruta = `/app/configuracion/${catalogo}`;
  return <><Seo titulo={config.titulo} descripcion={config.descripcion} noIndexar />
    <CabeceraPagina titulo={config.titulo} descripcion={config.descripcion} migas={[{ texto: "Configuración", a: "/app/configuracion" }, { texto: config.titulo }]} acciones={<BotonEnlace a={`${ruta}/nuevo`} icono="mas">Nuevo {config.singular}</BotonEnlace>} />
    <Tarjeta aSangre><BarraFiltros marcador={`Buscar ${config.singular}`} valor={url.busqueda} alCambiar={url.setBusqueda}>
      <Filtro etiqueta="Estado" valor={url.parametros.get("active") ?? ""} alCambiar={v => url.cambiar("active", v)} opciones={[{ valor: "true", texto: "Activo" }, { valor: "false", texto: "Inactivo" }]} />
    </BarraFiltros>
      {consulta.isPending ? <EstadoVacio icono="salones" titulo="Cargando catálogo…" descripcion="Consultando las opciones de tu organización." />
        : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar el catálogo" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>} />
          : !consulta.data.content?.length ? <EstadoVacio icono="salones" titulo="Sin opciones para mostrar" descripcion="Probá cambiar la búsqueda o creá una nueva opción." />
            : <><Tabla resumen={config.titulo}><EncabezadoTabla columnas={["Nombre", "Estado"]} /><tbody>{consulta.data.content.map(o => <tr key={o.id}><td><Link to={`${ruta}/${o.id}`}>{o.name}</Link></td><td>{o.active ? "Activo" : "Inactivo"}</td></tr>)}</tbody></Tabla>
              <TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements ?? 0} entidad="opciones" pagina={url.pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={url.cambiarPagina} /></TarjetaPie></>}
    </Tarjeta></>;
}
