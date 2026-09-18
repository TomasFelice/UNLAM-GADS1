import { BarraFiltros, Filtro, FiltroFecha } from "../../shared/components/BarraFiltros";
import { Boton } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta } from "../../shared/components/Tarjeta";
import { ESTADO_OPORTUNIDAD } from "../../shared/data/formato";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEtapas, useOrigenes } from "../catalogos/apiCatalogos";
import { useContactos } from "../contactos/apiContactos";
import { useEmpresas } from "../empresas/apiEmpresas";
import { FichaOportunidad } from "../oportunidades/FichaOportunidad";
import { useCambiarEtapa, useEmbudo } from "../oportunidades/apiOportunidades";
import { useSalones } from "../salones/apiSalones";
import css from "./PaginaEmbudo.module.css";

export function PaginaEmbudo() {
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
  const consulta = useEmbudo(filtros);
  const usuarios = useUsuariosAsignables();
  const etapas = useEtapas();
  const origenes = useOrigenes();
  const salones = useSalones();
  const empresas = useEmpresas(0);
  const contactos = useContactos(0);
  const cambiar = useCambiarEtapa();
  const abiertas = (consulta.data?.columns ?? []).filter((c) => !c.stageName?.toLowerCase().includes("ganada") && !c.stageName?.toLowerCase().includes("perdida"));

  return <>
    <Seo titulo="Embudo comercial" descripcion="Oportunidades agrupadas por etapa." noIndexar />
    <CabeceraPagina titulo="Embudo comercial" descripcion="Oportunidades reales agrupadas por su etapa actual." />
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
        ? <EstadoVacio icono="embudo" titulo="Cargando embudo…" descripcion="Consultando las oportunidades." />
        : consulta.isError
          ? <EstadoVacio icono="alerta" titulo="No pudimos cargar el embudo" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} />
          : !abiertas.some((c) => c.opportunities?.length)
            ? <EstadoVacio icono="embudo" titulo="El embudo está vacío" descripcion="No hay oportunidades que coincidan con los filtros." />
            : <>
              {cambiar.isError && <p className={css.error} role="alert">{cambiar.error.message}</p>}
              <div className={css.tablero} role="region" aria-label="Oportunidades por etapa" tabIndex={0}>
                {abiertas.map((columna) => (
                  <section key={columna.stageId} className={css.columna} aria-labelledby={`etapa-${columna.stageId}`}>
                    <header className={css.columnaCabecera}>
                      <h2 id={`etapa-${columna.stageId}`}>{columna.stageName}</h2>
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
                              accion={o.id ? (
                                <label className={css.cambioEtapa}>
                                  <span>Cambiar etapa</span>
                                  <select
                                    aria-label={`Cambiar etapa de ${o.title}`}
                                    value={o.stageId}
                                    disabled={cambiar.isPending}
                                    onChange={(e) => cambiar.mutate({ id: o.id!, stageId: Number(e.target.value) })}
                                  >
                                    {abiertas.map((destino) => (
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
