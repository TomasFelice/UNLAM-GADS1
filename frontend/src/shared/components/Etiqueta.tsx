import type { ReactNode } from "react";
import css from "./Etiqueta.module.css";

export type Tono =
  | "neutro"
  | "exito"
  | "alerta"
  | "peligro"
  | "info"
  | "teal"
  | "blue"
  | "violet"
  | "indigo"
  | "amber"
  | "green"
  | "red";

type Props = {
  tono?: Tono;
  /** Muestra un punto de color antes del texto. */
  punto?: boolean;
  tamano?: "sm" | "md";
  children: ReactNode;
};

export function Etiqueta({
  tono = "neutro",
  punto = false,
  tamano = "md",
  children,
}: Props) {
  return (
    <span className={`${css.etiqueta} ${css[tono]} ${css[tamano]}`}>
      {punto && <span className={css.punto} aria-hidden="true" />}
      {children}
    </span>
  );
}
