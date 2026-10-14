import { SelectorCliente } from "../clientes/SelectorCliente";
import { useEffect } from "react";
import { useEmbudoPredeterminado } from "../embudos/apiEmbudos";
import { SelectorEmbudo } from "../embudos/SelectorEmbudo";
import { BarraFiltros, Filtro, FiltroFecha } from "../../shared/components/BarraFiltros";
import { Boton } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta } from "../../shared/components/Tarjeta";
import { ESTADO_OPORTUNIDAD } from "../../shared/data/formato";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEtapas } from "../catalogos/apiCatalogos";
import { SelectorCatalogo } from "../catalogos/SelectorCatalogo";
import { FichaOportunidad } from "../oportunidades/FichaOportunidad";
import { useCambiarEtapa, useEmbudo } from "../oportunidades/apiOportunidades";
import { SelectorSalon } from "../salones/SelectorSalon";
import css from "./PaginaEmbudo.module.css";

export function PaginaEmbudo() {
  const url = useBusquedaUrl();
  const predeterminado = useEmbudoPredeterminado();
  useEffect(() => { if (!url.parametros.get("pipelineId") && predeterminado.data?.id) url.cambiarVarios({ pipelineId: String(predeterminado.data.id), stageId: "" }); }, [url, predeterminado.data]);
  const filtros = {
    pipelineId: url.parametros.get("pipelineId") || undefined,
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
  const consulta = useEmbudo(filtros, !!filtros.pipelineId);
  const usuarios = useUsuariosAsignables();
  const etapas = useEtapas(filtros.pipelineId ? Number(filtros.pipelineId) : undefined, !!filtros.pipelineId);


  const cambiar = useCambiarEtapa();
  const columnas = consulta.data?.columns ?? [];
  const destinos = columnas.filter(c => c.kind === "OPEN" && c.active);

  return <>
    <Seo titulo="Embudo comercial" descripcion="Oportunidades agrupadas por etapa." noIndexar />
    <CabeceraPagina titulo={consulta.data?.pipeline?.name ?? "Embudo comercial"} descripcion={`${consulta.data?.pipeline?.defaultPipeline ? "Predeterminado · " : ""}${consulta.data?.pipeline?.active === false ? "Inactivo · " : ""}Oportunidades agrupadas por su etapa actual.`} />
    <Tarjeta aSangre>
      <BarraFiltros marcador="Buscar por título" valor={url.busqueda} alCambiar={url.setBusqueda}>
        <SelectorEmbudo valor={filtros.pipelineId} requerido alCambiar={v => url.cambiarVarios({ pipelineId: v, stageId: "" })} />
        <Filtro etiqueta="Etapa" valor={filtros.stageId} alCambiar={(v) => url.cambiar("stageId", v)} opciones={(etapas.data ?? []).map((e) => ({ valor: String(e.id), texto: e.name ?? "Etapa" }))} />
        <Filtro etiqueta="Estado" valor={filtros.status} alCambiar={(v) => url.cambiar("status", v)} opciones={Object.entries(ESTADO_OPORTUNIDAD).map(([valor, e]) => ({ valor, texto: e.texto }))} />
        <Filtro etiqueta="Responsable" valor={filtros.salesRepId} alCambiar={(v) => url.cambiar("salesRepId", v)} opciones={(usuarios.data ?? []).map((u) => ({ valor: String(u.id), texto: `${u.firstName} ${u.lastName}` }))} />
        <SelectorCatalogo catalogo="origenes" valor={filtros.originId} alCambiar={v => url.cambiar("originId", v)} />
        <SelectorSalon valor={filtros.venueId} alCambiar={(v) => url.cambiar("venueId", v)} />
        <SelectorCliente tipo="empresa" valor={filtros.companyId} filtro alCambiar={v => url.cambiarVarios({ companyId: v, contactId: "" })} />
        <SelectorCliente key={filtros.companyId} tipo="contacto" empresaId={filtros.companyId} valor={filtros.contactId} filtro alCambiar={v => url.cambiar("contactId", v)} />
        <FiltroFecha etiqueta="Evento desde" valor={filtros.eventFrom} alCambiar={(v) => url.cambiar("eventFrom", v)} />
        <FiltroFecha etiqueta="Evento hasta" valor={filtros.eventTo} alCambiar={(v) => url.cambiar("eventTo", v)} />
      </BarraFiltros>
      {predeterminado.isError && !filtros.pipelineId ? <EstadoVacio icono="alerta" titulo="No pudimos consultar el predeterminado" descripcion={predeterminado.error.message} accion={<Boton onClick={() => void predeterminado.refetch()}>Reintentar</Boton>} /> : consulta.isPending
        ? <EstadoVacio icono="embudo" titulo="Cargando embudo…" descripcion="Consultando las oportunidades." />
        : consulta.isError
          ? <EstadoVacio icono="alerta" titulo="No pudimos cargar el embudo" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} />
          : !columnas.some((c) => c.opportunities?.length)
            ? <EstadoVacio icono="embudo" titulo="El embudo está vacío" descripcion="No hay oportunidades que coincidan con los filtros." />
            : <>
              {cambiar.isError && <p className={css.error} role="alert">{cambiar.error.message}</p>}
              <div className={css.tablero} role="region" aria-label="Oportunidades por etapa" tabIndex={0}>
                {columnas.map((columna) => (
                  <section key={columna.stageId} className={css.columna} aria-labelledby={`etapa-${columna.stageId}`}>
                    <header className={css.columnaCabecera}>
                      <h2 id={`etapa-${columna.stageId}`}>{columna.stageName}{!columna.active ? " · Inactiva" : ""}</h2>
                      <span className={css.cuenta} aria-label={`${columna.opportunities?.length ?? 0} oportunidades`}>
                        {columna.opportunities?.length ?? 0}
                      </span>
                    </header>
                    {columna.opportunities?.length ? (
                      <ul className={css.fichas}>
                        {columna.opportunities.map((o) => (
                          <li key={o.id}>
                            <FichaOportunidad
                              oportunidad={o}
                              variante="embudo"
                              accion={o.id && o.status === "ABIERTA" ? (
                                <label className={css.cambioEtapa}>
                                  <span>Cambiar etapa</span>
                                  <select
                                    aria-label={`Cambiar etapa de ${o.title}`}
                                    value={o.stageId}
                                    disabled={cambiar.isPending}
                                    onChange={(e) => cambiar.mutate({ id: o.id!, stageId: Number(e.target.value), version: o.version! })}
                                  >
                                    {!destinos.some(d => d.stageId === o.stageId) && <option value={o.stageId}>{columna.stageName} (actual)</option>}
                                    {destinos.map((destino) => (
                                      <option key={destino.stageId} value={destino.stageId}>{destino.stageName}</option>
                                    ))}
                                  </select>
                                </label>
                              ) : undefined}
                            />
                          </li>
                        ))}
                      </ul>
                    ) : <p className={css.columnaVacia}>Sin oportunidades en esta etapa</p>}
                  </section>
                ))}
              </div>
            </>}
    </Tarjeta>
  </>;
}
