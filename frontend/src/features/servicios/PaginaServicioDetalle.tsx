import { useParams } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { pesos } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useServicio } from "./apiServicios";

export function PaginaServicioDetalle() {
  const id = Number(useParams().id);
  const valido = Number.isSafeInteger(id) && id > 0;
  const consulta = useServicio(valido ? id : undefined);
  const { sesion } = useSesion();
  const seo = <Seo titulo={consulta.data?.name ?? "Servicio"} descripcion="" noIndexar />;
  if (!valido || consulta.isError) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar el servicio" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>} /></>;
  if (consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando servicio…" descripcion="Consultando el catálogo." /></>;
  const servicio = consulta.data;
  return <>{seo}<CabeceraPagina titulo={servicio.name ?? "Servicio"} migas={[{ texto: "Servicios", a: "/app/servicios" }, { texto: servicio.name ?? "Detalle" }]} descripcion="Servicio adicional para eventos." acciones={sesion?.usuario.rol === "ADMIN" && <BotonEnlace a={`/app/servicios/${id}/editar`} variante="secundario" icono="lapiz">Editar servicio</BotonEnlace>} />
    <Tarjeta><TarjetaCuerpo><ListaDatos datos={[{ etiqueta: "Estado", valor: servicio.active ? "Activo" : "Inactivo" }, { etiqueta: "Precio", valor: servicio.price == null ? "Sin especificar" : pesos(servicio.price) }, { etiqueta: "Descripción", valor: servicio.description, ancho: true }]} /></TarjetaCuerpo></Tarjeta></>;
}
