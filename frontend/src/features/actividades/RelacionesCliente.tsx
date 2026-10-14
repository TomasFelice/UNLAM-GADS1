import { useState } from "react";
import { Link } from "react-router-dom";
import { PieListado } from "../../shared/components/BarraFiltros";
import { Boton } from "../../shared/components/Boton";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ESTADO_CLIENTE, ESTADO_OPORTUNIDAD, fechaHora } from "../../shared/data/formato";
import { useContactos } from "../contactos/apiContactos";
import { useOportunidades } from "../oportunidades/apiOportunidades";
import css from "./HistorialComercial.module.css";

export function OportunidadesCliente({ empresaId, contactoId }: { empresaId?: number; contactoId?: number }) {
  const [pagina, setPagina] = useState(0);
  const consulta = useOportunidades(pagina, { companyId: empresaId?.toString(), contactId: contactoId?.toString() });
  return <section aria-label="Oportunidades del cliente"><Tarjeta><TarjetaCabecera titulo="Oportunidades" descripcion="Negociaciones abiertas y cerradas visibles para vos" /><TarjetaCuerpo>
    {consulta.isPending ? <p role="status">Cargando oportunidades…</p> : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar las oportunidades" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} /> : <>
      {!consulta.data.content?.length ? <p>Sin oportunidades asociadas.</p> : <ul className={css.lista}>{consulta.data.content.map(o => <li key={o.id}><Link to={`/app/oportunidades/${o.id}`}>{o.title}</Link><p>{ESTADO_OPORTUNIDAD[o.status ?? "ABIERTA"].texto} · {o.stageName} · {fechaHora(o.eventStart)}</p></li>)}</ul>}
      <div className={css.pie}><PieListado entidad="oportunidades" mostrados={consulta.data.content?.length ?? 0} total={consulta.data.totalElements ?? 0} pagina={pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={setPagina} /></div>
    </>}
  </TarjetaCuerpo></Tarjeta></section>;
}

export function ContactosEmpresa({ empresaId }: { empresaId: number }) {
  const [pagina, setPagina] = useState(0);
  const consulta = useContactos(pagina, { companyId: String(empresaId) });
  return <section aria-label="Contactos de la empresa"><Tarjeta><TarjetaCabecera titulo="Contactos" descripcion="Personas asociadas, incluidos contactos inactivos" /><TarjetaCuerpo>
    {consulta.isPending ? <p role="status">Cargando contactos…</p> : consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar los contactos" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>} /> : <>
      {!consulta.data.content?.length ? <p>Sin contactos asociados.</p> : <ul className={css.lista}>{consulta.data.content.map(c => <li key={c.id}><Link to={`/app/contactos/${c.id}`}>{c.firstName} {c.lastName}</Link><p>{ESTADO_CLIENTE[c.status ?? "POTENCIAL"].texto} · {c.email || "Sin correo"}</p></li>)}</ul>}
      <div className={css.pie}><PieListado entidad="contactos" mostrados={consulta.data.content?.length ?? 0} total={consulta.data.totalElements ?? 0} pagina={pagina} totalPaginas={consulta.data.totalPages ?? 1} alCambiarPagina={setPagina} /></div>
    </>}
  </TarjetaCuerpo></Tarjeta></section>;
}
