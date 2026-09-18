import type { ReactNode } from "react";
import css from "./ListaDatos.module.css";

export type Dato = {
  etiqueta: string;
  valor: ReactNode;
  /** Ocupa el ancho completo: observaciones, direcciones largas. */
  ancho?: boolean;
};

type Props = {
  datos: Dato[];
  /** Una sola columna en paneles laterales angostos. */
  columnas?: 1 | 2;
};

/** Lista de definiciones para las pantallas de detalle. */
export function ListaDatos({ datos, columnas = 2 }: Props) {
  return (
    <dl className={`${css.lista} ${columnas === 1 ? css.unaColumna : ""}`}>
      {datos.map((d) => (
        <div
          key={d.etiqueta}
          className={`${css.par} ${d.ancho ? css.ancho : ""}`}
        >
          <dt>{d.etiqueta}</dt>
          <dd>{d.valor || <span className={css.vacio}>Sin datos</span>}</dd>
        </div>
      ))}
    </dl>
  );
}
