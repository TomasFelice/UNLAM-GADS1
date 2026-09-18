import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { ESTADO_OPORTUNIDAD, fecha, pesos } from "../../shared/data/formato";
import type { Oportunidad } from "./apiOportunidades";
import css from "./FichaOportunidad.module.css";

type Props = {
  oportunidad: Oportunidad;
  mostrarCliente?: boolean;
  accion?: ReactNode;
  variante?: "lista" | "embudo";
};

export function FichaOportunidad({ oportunidad, mostrarCliente = true, accion, variante = "lista" }: Props) {
  const estado = ESTADO_OPORTUNIDAD[oportunidad.status ?? "ABIERTA"];

  return (
    <article className={`${css.ficha} ${variante === "embudo" ? css.embudo : ""}`}>
      <div className={css.principal}>
        <Link className={css.titulo} to={`/app/oportunidades/${oportunidad.id}`}>
          {oportunidad.title}
        </Link>
        <p className={css.meta}>
          {mostrarCliente && <span>{oportunidad.companyName || oportunidad.contactName || "Cliente individual"}</span>}
          <span>{oportunidad.venueName}</span>
          <span>{fecha(oportunidad.eventStart)}</span>
        </p>
      </div>
      <div className={css.lateral}>
        <strong className={css.valor}>{pesos(oportunidad.estimatedValue ?? 0)}</strong>
        <Etiqueta tono={estado.tono} tamano="sm">{estado.texto}</Etiqueta>
        {accion && <div className={css.accion}>{accion}</div>}
      </div>
    </article>
  );
}
