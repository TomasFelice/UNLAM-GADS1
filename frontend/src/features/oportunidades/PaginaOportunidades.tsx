import { Link } from "react-router-dom";
import { BarraFiltros, Filtro, FiltroFecha, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { ESTADO_OPORTUNIDAD, fecha, pesos } from "../../shared/data/formato";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEtapas, useOrigenes } from "../catalogos/apiCatalogos";
import { useContactos } from "../contactos/apiContactos";
import { useEmpresas } from "../empresas/apiEmpresas";
import { useSalones } from "../salones/apiSalones";
import { useOportunidades } from "./apiOportunidades";

export function PaginaOportunidades() {
  const url = useBusquedaUrl();
  const filtros = {
    q: url.parametros.get("q") || undefined,
    status: url.parametros.get("status") || undefined,
    stageId: url.parametros.get("stageId") || undefined,
    originId: url.parametros.get("originId") || undefined,
    salesRepId: url.parametros.get("salesRepId") || undefined,
    venueId: url.parametros.get("venueId") || undefined,
    companyId: url.parametros.get("companyId") || undefined,
    contactId: url.parametros.get("contactId") || undefined,
    eventFrom: url.parametros.get("eventFrom") || undefined,
    eventTo: url.parametros.get("eventTo") || undefined,
  };
  const consulta = useOportunidades(url.pagina, filtros);
  const etapas = useEtapas();
  const origenes = useOrigenes();
  const usuarios = useUsuariosAsignables();
  const salones = useSalones();
  const empresas = useEmpresas(0);
  const contactos = useContactos(0);

  return <>
    <Seo titulo="Oportunidades" descripcion="Listado de oportunidades comerciales." noIndexar />
    <CabeceraPagina titulo="Oportunidades" descripcion="Negociaciones con responsable, etapa y fecha de evento." acciones={<><BotonEnlace a="/app/embudo" variante="secundario" icono="embudo">Ver embudo</BotonEnlace><BotonEnlace a="/app/oportunidades/nueva" icono="mas">Nueva oportunidad</BotonEnlace></>} />
    <Tarjeta aSangre>
      <BarraFiltros marcador="Buscar por título" valor={url.busqueda} alCambiar={url.setBusqueda}>
        <Filtro etiqueta="Etapa" valor={filtros.stageId} alCambiar={(v) => url.cambiar("stageId", v)} opciones={(etapas.data ?? []).map((e) => ({ valor: String(e.id), texto: e.name ?? "Etapa" }))} />
        <Filtro etiqueta="Estado" valor={filtros.status} alCambiar={(v) => url.cambiar("status", v)} opciones={Object.entries(ESTADO_OPORTUNIDAD).map(([valor, e]) => ({ valor, texto: e.texto }))} />
        <Filtro etiqueta="Responsable" valor={filtros.salesRepId} alCambiar={(v) => url.cambiar("salesRepId", v)} opciones={(usuarios.data ?? []).map((u) => ({ valor: String(u.id), texto: `${u.firstName} ${u.lastName}` }))} />
        <Filtro etiqueta="Origen" valor={filtros.originId} alCambiar={(v) => url.cambiar("originId", v)} opciones={(origenes.data ?? []).map((o) => ({ valor: String(o.id), texto: o.name ?? "Origen" }))} />
        <Filtro etiqueta="Salón" valor={filtros.venueId} alCambiar={(v) => url.cambiar("venueId", v)} opciones={(salones.data ?? []).map((s) => ({ valor: String(s.id), texto: s.name ?? "Salón" }))} />
        <Filtro etiqueta="Empresa" valor={filtros.companyId} alCambiar={(v) => url.cambiar("companyId", v)} opciones={(empresas.data?.content ?? []).map((e) => ({ valor: String(e.id), texto: e.businessName ?? e.legalName ?? "Empresa" }))} />
        <Filtro etiqueta="Contacto" valor={filtros.contactId} alCambiar={(v) => url.cambiar("contactId", v)} opciones={(contactos.data?.content ?? []).map((c) => ({ valor: String(c.id), texto: `${c.firstName} ${c.lastName}` }))} />
        <FiltroFecha etiqueta="Evento desde" valor={filtros.eventFrom} alCambiar={(v) => url.cambiar("eventFrom", v)} />
        <FiltroFecha etiqueta="Evento hasta" valor={filtros.eventTo} alCambiar={(v) => url.cambiar("eventTo", v)} />
      </BarraFiltros>
      {consulta.isPending
        ? <EstadoVacio icono="oportunidades" titulo="Cargando oportunidades…" descripcion="Consultando las negociaciones." />
        : consulta.isError
          ? <EstadoVacio icono="alerta" titulo="No pudimos cargar las oportunidades" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} />
          : !consulta.data.content?.length
            ? <EstadoVacio icono="oportunidades" titulo="No hay oportunidades para mostrar" descripcion="Probá cambiar los filtros o creá la primera oportunidad." />
            : <>
              <Tabla resumen="Listado de oportunidades">
                <EncabezadoTabla columnas={["Oportunidad", "Cliente", "Salón", "Evento", "Responsable", "Valor", "Etapa", "Estado"]} />
                <tbody>{consulta.data.content.map((o) => {
                  const estado = ESTADO_OPORTUNIDAD[o.status ?? "ABIERTA"];
                  return <tr key={o.id}>
                    <td><Link to={`/app/oportunidades/${o.id}`}>{o.title}</Link></td>
                    <td>{o.companyName || o.contactName || "Cliente individual"}</td>
                    <td>{o.venueName}<span className="secundario">{o.attendeeCount} asistentes</span></td>
                    <td>{fecha(o.eventStart)}</td><td>{o.salesRepName}</td><td>{pesos(o.estimatedValue ?? 0)}</td><td>{o.stageName}</td>
                    <td><Etiqueta tono={estado.tono} tamano="sm" punto>{estado.texto}</Etiqueta></td>
                  </tr>;
                })}</tbody>
              </Tabla>
              <TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements ?? 0} entidad="oportunidades" pagina={url.pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={url.cambiarPagina} /></TarjetaPie>
            </>}
    </Tarjeta>
  </>;
}
