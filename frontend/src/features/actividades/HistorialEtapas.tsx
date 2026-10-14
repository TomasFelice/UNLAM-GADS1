import { Boton } from "../../shared/components/Boton";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { fechaHora, pesosExactos } from "../../shared/data/formato";
import pantalla from "../../shared/styles/pantalla.module.css";
import { useHistorialEtapas } from "../oportunidades/apiOportunidades";
import css from "./HistorialComercial.module.css";

export function HistorialEtapas({ oportunidadId }: { oportunidadId: number }) {
  const historial = useHistorialEtapas(oportunidadId);
  return <section aria-label="Historial de etapas"><Tarjeta><TarjetaCabecera titulo="Historial de etapas" /><TarjetaCuerpo>
    {historial.isPending ? <EstadoVacio icono="embudo" titulo="Cargando historial…" descripcion="Consultando los cambios guardados." /> : historial.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar el historial" descripcion={historial.error.message} accion={<Boton variante="secundario" onClick={() => historial.refetch()}>Reintentar</Boton>} /> : !historial.data.length ? <p>Sin cambios registrados.</p> : <ol className={pantalla.linea}>
      {historial.data.map(h => <li key={h.id} className={pantalla.lineaItem}>
        <span className={pantalla.lineaMarca} aria-hidden="true">●</span>
        <div className={`${pantalla.lineaCuerpo} ${css.hecho}`}>
          <p className={pantalla.lineaTitulo}>{h.fromStage ? `De ${h.fromStage.name} a ${h.toStage?.name}` : `Alta en ${h.toStage?.name}`}<time className={pantalla.lineaFecha} dateTime={h.changedAt}>{fechaHora(h.changedAt)}</time></p>
          <p className={pantalla.lineaAutor}>{h.author?.firstName} {h.author?.lastName}</p>
          {h.note && <p>{h.note}</p>}
          {h.eventType === "WON" && <p>Ganada · Valor final: {h.finalValue == null ? "Sin dato histórico" : pesosExactos(h.finalValue)} · Cierre: {fechaHora(h.closedAt)}</p>}
          {h.eventType === "LOST" && <p>Perdida · Motivo: {h.lossReasonName || "Sin dato histórico"} · Cierre: {fechaHora(h.closedAt)}</p>}
          {h.eventType === "REOPENED" && <p>Reapertura</p>}
          {h.snapshotComplete === false && <p>Registro anterior sin datos completos del cierre.</p>}
        </div>
      </li>)}
    </ol>}
  </TarjetaCuerpo></Tarjeta></section>;
}
