import { useParams } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ESTADO_SALON, pesos } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useSalon } from "./apiSalones";

export function PaginaSalonDetalle() {
  const id = Number(useParams().id);
  const valido = Number.isSafeInteger(id) && id > 0;
  const consulta = useSalon(valido ? id : undefined);
  const { sesion } = useSesion();
  const seo = <Seo titulo={consulta.data?.name ?? "Salón"} descripcion="" noIndexar />;
  if (!valido || consulta.isError) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar el salón" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>} /></>;
  if (consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando salón…" descripcion="Consultando el espacio." /></>;
  const salon = consulta.data;
  return <>{seo}<CabeceraPagina titulo={salon.name ?? "Salón"} migas={[{ texto: "Salones", a: "/app/salones" }, { texto: salon.name ?? "Detalle" }]} descripcion="Características y disponibilidad del espacio." acciones={sesion?.usuario.rol === "ADMIN" && <BotonEnlace a={`/app/salones/${id}/editar`} variante="secundario" icono="lapiz">Editar salón</BotonEnlace>} />
    <Tarjeta><TarjetaCuerpo><ListaDatos datos={[{ etiqueta: "Estado", valor: ESTADO_SALON[salon.status ?? "INACTIVO"].texto }, { etiqueta: "Capacidad", valor: `${salon.capacity} personas` }, { etiqueta: "Tarifa por jornada", valor: salon.rate == null ? "Sin especificar" : pesos(salon.rate) }, { etiqueta: "Ubicación", valor: [salon.address, salon.locality].filter(Boolean).join(", ") }, { etiqueta: "Descripción", valor: salon.description, ancho: true }, { etiqueta: "Equipamiento", valor: salon.equipment?.length ? <ul>{salon.equipment.map(e => <li key={e}>{e}</li>)}</ul> : "Sin especificar", ancho: true }]} /></TarjetaCuerpo></Tarjeta></>;
}
