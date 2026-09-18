import type { ReactNode } from "react";
import { Icono, type NombreIcono } from "./Icono";
import css from "./EstadoVacio.module.css";

type Props = {
  icono: NombreIcono;
  titulo: string;
  descripcion: string;
  accion?: ReactNode;
};

export function EstadoVacio({ icono, titulo, descripcion, accion }: Props) {
  return (
    <div className={css.vacio}>
      <span className={css.icono}>
        <Icono nombre={icono} tamano={22} />
      </span>
      <h3 className={css.titulo}>{titulo}</h3>
      <p className={css.descripcion}>{descripcion}</p>
      {accion && <div className={css.accion}>{accion}</div>}
    </div>
  );
}
