import type { ReactNode } from "react";
import css from "./Tarjeta.module.css";

type PropsTarjeta = {
  children: ReactNode;
  /** Sin padding interno, para tablas y listas a sangre. */
  aSangre?: boolean;
  className?: string;
};

export function Tarjeta({ children, aSangre, className = "" }: PropsTarjeta) {
  return (
    <section
      className={`${css.tarjeta} ${aSangre ? css.aSangre : ""} ${className}`}
    >
      {children}
    </section>
  );
}

type PropsCabecera = {
  titulo: string;
  descripcion?: string;
  /** Nivel de encabezado real, para no romper la jerarquía de la página. */
  nivel?: 2 | 3;
  acciones?: ReactNode;
};

export function TarjetaCabecera({
  titulo,
  descripcion,
  nivel = 2,
  acciones,
}: PropsCabecera) {
  const Titulo = nivel === 2 ? "h2" : "h3";
  return (
    <header className={css.cabecera}>
      <div>
        <Titulo className={css.titulo}>{titulo}</Titulo>
        {descripcion && <p className={css.descripcion}>{descripcion}</p>}
      </div>
      {acciones && <div className={css.acciones}>{acciones}</div>}
    </header>
  );
}

export function TarjetaCuerpo({ children }: { children: ReactNode }) {
  return <div className={css.cuerpo}>{children}</div>;
}

export function TarjetaPie({ children }: { children: ReactNode }) {
  return <footer className={css.pie}>{children}</footer>;
}
