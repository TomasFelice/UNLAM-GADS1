import { useParams } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { CATALOGOS, useEntradaCatalogo, type Catalogo } from "./apiCatalogos";

export function PaginaCatalogoDetalle({ catalogo }: { catalogo: Catalogo }) {
  const id = Number(useParams().id);
  const consulta = useEntradaCatalogo(catalogo, id);
  const config = CATALOGOS[catalogo];
  const ruta = `/app/configuracion/${catalogo}`;
  const seo = <Seo titulo={consulta.data?.name ?? config.titulo} descripcion={config.descripcion} noIndexar />;
  if (!Number.isSafeInteger(id) || id <= 0 || consulta.isError) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar esta opción" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<><BotonEnlace a={ruta} variante="secundario">Volver al catálogo</BotonEnlace>{consulta.isError && <Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>}</>} /></>;
  if (consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando opción…" descripcion="Consultando su estado." /></>;
  return <>{seo}<CabeceraPagina titulo={consulta.data.name ?? config.titulo} descripcion="Las referencias comerciales se conservan al desactivar esta opción." migas={[{ texto: "Configuración", a: "/app/configuracion" }, { texto: config.titulo, a: ruta }, { texto: consulta.data.name ?? "Detalle" }]} acciones={<BotonEnlace a={`${ruta}/${id}/editar`} variante="secundario" icono="lapiz">Editar opción</BotonEnlace>} />
    <Tarjeta><TarjetaCuerpo><ListaDatos datos={[{ etiqueta: "Nombre", valor: consulta.data.name }, { etiqueta: "Estado", valor: consulta.data.active ? "Activo" : "Inactivo" }]} /></TarjetaCuerpo></Tarjeta></>;
}
