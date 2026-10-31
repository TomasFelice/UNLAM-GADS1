import { useEffect, useState } from "react";
import { Boton } from "../../shared/components/Boton";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ErrorDeApi } from "../../shared/api/errores";
import { fechaHora } from "../../shared/data/formato";
import { useResumenComercial } from "./apiResumenComercial";
import css from "./ResumenComercial.module.css";

const SECCIONES = [
  ["clientNeeds", "Necesidad del cliente"], ["currentSituation", "Situación actual"],
  ["agreementsAndObjections", "Acuerdos y objeciones"], ["unconfirmedInformation", "Información pendiente o contradictoria"],
] as const;
const ETIQUETAS: Record<string, string> = {
  title: "Título", notes: "Observaciones", status: "Estado actual", stage: "Etapa actual", stageKind: "Clase de etapa",
  pipeline: "Embudo", venue: "Salón", venueCapacity: "Capacidad", venueCatalogRateARS: "Tarifa de catálogo (ARS)",
  services: "Servicios y precios de catálogo", eventType: "Tipo de evento", eventStart: "Inicio", eventEnd: "Fin",
  attendees: "Asistentes", estimatedValueARS: "Valor estimado (ARS)", finalValueARS: "Valor final (ARS)",
  optionalProbability: "Probabilidad registrada", estimatedCloseDate: "Cierre estimado", closedAt: "Cierre registrado",
  lossReason: "Motivo de pérdida", type: "Tipo", description: "Descripción", result: "Resultado", note: "Nota",
  fromStageCurrentName: "Etapa anterior (nombre actual)", toStageCurrentName: "Etapa destino (nombre actual)",
  destinationStatus: "Estado de destino", snapshotComplete: "Datos históricos completos", historicalLossReason: "Motivo histórico",
};

export function ResumenComercial({ oportunidadId, version, autorizado }: { oportunidadId: number; version: number; autorizado: boolean }) {
  const consulta = useResumenComercial(oportunidadId, version, autorizado);
  const [ahora, fijarAhora] = useState(Date.now);
  const error = consulta.error instanceof ErrorDeApi ? consulta.error.problema : undefined;
  const espera = (error?.retryAfterSeconds ?? 0) * 1000;
  const habilitarEn = consulta.errorUpdatedAt + espera;
  useEffect(() => {
    if (!consulta.isError || !espera) return;
    const temporizador = setTimeout(() => fijarAhora(Date.now()), Math.max(0, habilitarEn - Date.now()));
    return () => clearTimeout(temporizador);
  }, [consulta.isError, espera, habilitarEn]);
  const recuperable = !error || [409, 429, 502, 503, 504].includes(error.status) && error.code !== "AI_NOT_CONFIGURED";
  const resultado = consulta.data;
  return <section aria-label="Resumen comercial" className={css.panel}>
    <Tarjeta><TarjetaCabecera titulo="Resumen comercial" /><TarjetaCuerpo>
      {consulta.contextoPendiente ? <p role="status">Validando acceso a la oportunidad…</p> : consulta.isPending || consulta.isFetching ?
        <p role="status">Preparando el resumen comercial… Podés seguir consultando la oportunidad.</p> : consulta.isError ?
          <div role="status" className={css.estado}>
            <p>{consulta.error.message}</p>
            {error?.status === 429 && <p>{espera ? `Esperá ${Math.ceil(espera / 1000)} segundos antes de reintentar.` : "Intentá más tarde."}</p>}
            {recuperable && <Boton variante="secundario" disabled={consulta.isFetching || espera > 0 && ahora < habilitarEn}
              onClick={() => { if (!consulta.isFetching && Date.now() >= habilitarEn) void consulta.refetch(); }}>Reintentar</Boton>}
          </div> : resultado && <div className={css.contenido}>
            <p className={css.aviso}>Generado con IA. Revisá la información y sus fuentes.</p>
            <p className={css.cobertura}>Contexto al {fechaHora(resultado.contextAsOf)} · Generado el {fechaHora(resultado.generatedAt)}</p>
            <p className={css.cobertura}>{resultado.coverage?.includedActivities} de {resultado.coverage?.totalActivities} actividades y {resultado.coverage?.includedTransitions} de {resultado.coverage?.totalTransitions} cambios comerciales.
              {resultado.coverage?.partial && " Cobertura parcial."}{resultado.coverage?.textTruncated && " Hay textos recortados."}</p>
            {resultado.coverage?.activityRange?.from && <p className={css.cobertura}>Actividades: {fechaHora(resultado.coverage.activityRange.from)} a {fechaHora(resultado.coverage.activityRange.to)}</p>}
            {resultado.coverage?.transitionRange?.from && <p className={css.cobertura}>Cambios: {fechaHora(resultado.coverage.transitionRange.from)} a {fechaHora(resultado.coverage.transitionRange.to)}</p>}
            {SECCIONES.map(([clave, titulo]) => <section key={clave} className={css.seccion}>
              <h3>{titulo}</h3>
              {!resultado.sections?.[clave]?.length ? <p className={css.cobertura}>No consta en la información analizada.</p> :
                <ul className={css.lista}>{resultado.sections[clave]?.map((afirmacion, indice) => <li key={indice}>
                  <p className={css.texto}>{afirmacion.text}</p>
                  {afirmacion.sourceIds?.map(identificador => {
                    const fuente = resultado.sources?.find(f => f.sourceId === identificador);
                    return fuente && <details key={identificador} className={css.fuente}>
                      <summary>Fuente {identificador}: {fuente.title}</summary>
                      <p>{fuente.type === "ACTIVITY" ? "Actividad" : fuente.type === "TRANSITION" ? "Cambio comercial" : "Oportunidad"} #{fuente.originalId}</p>
                      {fuente.occurredAt && <p>Fecha del hecho: {fechaHora(fuente.occurredAt)}</p>}
                      {fuente.recordedAt && <p>Registrado: {fechaHora(fuente.recordedAt)}</p>}
                      {fuente.textTruncated && <p>Fragmentos recortados al límite de análisis.</p>}
                      <dl>{Object.entries(fuente.fragments ?? {}).map(([campo, valor]) => <div key={campo}>
                        <dt>{campo === "eventType" && fuente.type === "TRANSITION" ? "Tipo de cambio" : ETIQUETAS[campo] ?? "Hecho comercial"}</dt><dd className={css.texto}>{valor}</dd>
                      </div>)}</dl>
                    </details>;
                  })}
                </li>)}</ul>}
            </section>)}
          </div>}
    </TarjetaCuerpo></Tarjeta>
  </section>;
}
