import type { ReactNode } from "react";
import { Icono } from "./Icono";
import css from "./AvisoExito.module.css";

/** Confirmación persistente: no mueve el foco ni desaparece antes de poder leerla. */
export function AvisoExito({ children }: { children: ReactNode }) {
  return (
    <div className={css.aviso} role="status" aria-atomic="true">
      <Icono nombre="check" className={css.icono} />
      <div className={css.contenido}>{children}</div>
    </div>
  );
}
