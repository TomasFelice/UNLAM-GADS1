import type { ReactNode } from "react";
import css from "./Tabla.module.css";

type Props = {
  /** Descripción de la tabla para lectores de pantalla. */
  resumen: string;
  children: ReactNode;
};

export function Tabla({ resumen, children }: Props) {
  return (
    <div className={`${css.marco} scroll-fino`}>
      <table className={css.tabla}>
        <caption className="solo-lectores">{resumen}</caption>
        {children}
      </table>
    </div>
  );
}

export function EncabezadoTabla({ columnas }: { columnas: string[] }) {
  return (
    <thead>
      <tr>
        {columnas.map((c) => (
          <th key={c} scope="col">
            {c}
          </th>
        ))}
      </tr>
    </thead>
  );
}
