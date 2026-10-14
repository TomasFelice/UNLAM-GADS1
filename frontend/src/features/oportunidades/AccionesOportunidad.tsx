import { useState } from "react";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton } from "../../shared/components/Boton";
import { AreaTexto, Campo, Entrada, Selector } from "../../shared/components/Campos";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { fechaHora, pesosExactos } from "../../shared/data/formato";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEtapas } from "../catalogos/apiCatalogos";
import { NombreCatalogo } from "../catalogos/NombreCatalogo";
import { SelectorCatalogo } from "../catalogos/SelectorCatalogo";
import { useAccionOportunidad, useDisponibilidad, type AccionOportunidad, type OportunidadDetalle } from "./apiOportunidades";
import css from "./AccionesOportunidad.module.css";

const TITULOS = { assign: "Reasignar responsable", win: "Marcar como ganada", lose: "Marcar como perdida", reopen: "Reabrir oportunidad" };
export function AccionesOportunidad({ oportunidad: o }: { oportunidad: OportunidadDetalle }) {
  const { sesion } = useSesion(); const elevado = sesion?.usuario.rol === "ADMIN" || sesion?.usuario.rol === "SALES_MANAGER";
  const [accion, setAccion] = useState<AccionOportunidad>(); const [version, setVersion] = useState<number>();
  const [etapa, setEtapa] = useState(""); const [responsable, setResponsable] = useState("");
  const [valor, setValor] = useState(""); const [motivo, setMotivo] = useState(""); const [nota, setNota] = useState("");
  const guardar = useAccionOportunidad(o.id!); const etapas = useEtapas(o.pipelineId, !!accion);
  const usuarios = useUsuariosAsignables(accion === "assign"); const disponibilidad = useDisponibilidad(o.id!, accion === "win");
  const cerrada = o.status !== "ABIERTA";
  const conflicto = guardar.error instanceof ErrorDeApi && guardar.error.problema.status === 409;
  const tipo = accion === "win" ? "WON" : accion === "lose" ? "LOST" : "OPEN";
  const destinos = (etapas.data ?? []).filter(e => e.active && e.kind === tipo);
  const abrir = (nueva: AccionOportunidad) => { setAccion(nueva); setVersion(o.version); setEtapa(""); setResponsable(""); setValor(""); setMotivo(""); setNota(""); guardar.reset(); };
  const enviar = async (evento: React.SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault(); if (!accion || version === undefined || conflicto) return;
    const comunes = { version, stageId: Number(etapa), note: nota || undefined };
    try {
      const datos = accion === "assign" ? { version, salesRepId: Number(responsable) }
        : accion === "win" ? { ...comunes, finalValue: Number(valor) }
        : accion === "lose" ? { ...comunes, lossReasonId: Number(motivo) } : { ...comunes, note: nota.trim() };
      await guardar.mutateAsync({ accion, datos }); setAccion(undefined);
    } catch { /* El error y los valores permanecen visibles. */ }
  };
  return <div>
    {cerrada && <ListaDatos columnas={1} datos={[
      { etiqueta: "Fecha del cierre", valor: fechaHora(o.closedAt) },
      ...(o.status === "GANADA" ? [{ etiqueta: "Valor final", valor: pesosExactos(o.finalValue ?? 0) }] : [{ etiqueta: "Motivo de pérdida", valor: <NombreCatalogo catalogo="motivos-perdida" id={o.lossReasonId} /> }]),
    ]} />}
    {!accion && <div className={css.acciones}>
      {!cerrada && <><Boton onClick={() => abrir("win")}>Marcar como ganada</Boton><Boton variante="peligro" onClick={() => abrir("lose")}>Marcar como perdida</Boton>{elevado && <Boton variante="secundario" onClick={() => abrir("assign")}>Reasignar responsable</Boton>}</>}
      {cerrada && elevado && <Boton variante="secundario" onClick={() => abrir("reopen")}>Reabrir oportunidad</Boton>}
    </div>}
    {accion && <form className={css.formulario} onSubmit={enviar} aria-label={TITULOS[accion]}>
      <h3>{TITULOS[accion]}</h3>
      {accion === "win" && <div className={css.aviso} role="status">{disponibilidad.isPending ? "Consultando disponibilidad…" : disponibilidad.isError ? disponibilidad.error.message : disponibilidad.data?.available ? "El salón está disponible para este evento. Se vuelve a comprobar al confirmar." : "El salón no está disponible o su capacidad es insuficiente. Revisá los datos del evento."}</div>}
      {accion === "assign" ? <Campo etiqueta="Nuevo responsable" requerido>{(id, ayuda) => <Selector id={id} aria-describedby={ayuda} required value={responsable} onChange={e => setResponsable(e.target.value)}><option value="">Elegí un responsable</option>{(usuarios.data ?? []).map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}</Selector>}</Campo>
        : <Campo etiqueta="Etapa destino" requerido>{(id, ayuda) => <Selector id={id} aria-describedby={ayuda} required value={etapa} onChange={e => setEtapa(e.target.value)}><option value="">Elegí una etapa</option>{destinos.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}</Selector>}</Campo>}
      {etapas.isError && <p role="alert">{etapas.error.message}</p>}{usuarios.isError && accion === "assign" && <p role="alert">{usuarios.error.message}</p>}
      {accion === "win" && <Campo etiqueta="Valor final" requerido>{(id, ayuda) => <Entrada id={id} aria-describedby={ayuda} type="number" required min="0" max="9999999999999.99" step="0.01" value={valor} onChange={e => setValor(e.target.value)} />}</Campo>}
      {accion === "lose" && <Campo etiqueta="Motivo de pérdida" requerido>{(id, ayuda) => <SelectorCatalogo catalogo="motivos-perdida" id={id} descripcion={ayuda} soloActivos requerido valor={motivo} alCambiar={setMotivo} />}</Campo>}
      {accion !== "assign" && <Campo etiqueta={accion === "reopen" ? "Nota de reapertura" : "Nota del cierre"} requerido={accion === "reopen"}>{(id, ayuda) => <AreaTexto id={id} aria-describedby={ayuda} required={accion === "reopen"} value={nota} onChange={e => setNota(e.target.value)} />}</Campo>}
      {guardar.isError && <div role="alert" className={css.error}><p>{guardar.error.message}</p>{conflicto && <><p>Conservamos los valores. Recargá para consultar la oportunidad vigente.</p><Boton type="button" variante="secundario" onClick={() => window.location.reload()}>Recargar datos</Boton></>}</div>}
      <div className={css.acciones}><Boton type="button" variante="secundario" disabled={guardar.isPending} onClick={() => setAccion(undefined)}>Cancelar acción</Boton><Boton type="submit" disabled={guardar.isPending || conflicto || (accion === "lose" && !motivo) || (accion === "assign" ? usuarios.isPending || usuarios.isError : etapas.isPending || etapas.isError)}>{guardar.isPending ? "Guardando…" : "Confirmar acción"}</Boton></div>
    </form>}
  </div>;
}
