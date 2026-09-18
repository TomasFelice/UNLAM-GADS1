import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Link } from "react-router-dom";
import { Icono, type NombreIcono } from "./Icono";
import css from "./Boton.module.css";

type Variante = "primario" | "secundario" | "sutil" | "fantasma" | "peligro";
type Tamano = "sm" | "md" | "lg";

type Comun = {
  variante?: Variante;
  tamano?: Tamano;
  icono?: NombreIcono;
  iconoFinal?: NombreIcono;
  anchoCompleto?: boolean;
  children?: ReactNode;
  className?: string;
};

const clases = (p: Comun) =>
  [
    css.boton,
    css[p.variante ?? "primario"],
    css[p.tamano ?? "md"],
    p.anchoCompleto ? css.completo : "",
    !p.children ? css.soloIcono : "",
    p.className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

const contenido = (p: Comun) => (
  <>
    {p.icono && <Icono nombre={p.icono} tamano={p.tamano === "lg" ? 19 : 17} />}
    {p.children && <span>{p.children}</span>}
    {p.iconoFinal && (
      <Icono nombre={p.iconoFinal} tamano={p.tamano === "lg" ? 19 : 17} />
    )}
  </>
);

type PropsBoton = Comun & ButtonHTMLAttributes<HTMLButtonElement>;

export function Boton({
  variante,
  tamano,
  icono,
  iconoFinal,
  anchoCompleto,
  className,
  children,
  ...resto
}: PropsBoton) {
  const p = { variante, tamano, icono, iconoFinal, anchoCompleto, className, children };
  return (
    <button className={clases(p)} {...resto}>
      {contenido(p)}
    </button>
  );
}

type PropsEnlace = Comun & {
  a: string;
  externo?: boolean;
  "aria-label"?: string;
};

export function BotonEnlace({ a, externo, ...p }: PropsEnlace) {
  if (externo) {
    return (
      <a
        className={clases(p)}
        href={a}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={p["aria-label"]}
      >
        {contenido(p)}
      </a>
    );
  }
  return (
    <Link className={clases(p)} to={a} aria-label={p["aria-label"]}>
      {contenido(p)}
    </Link>
  );
}
