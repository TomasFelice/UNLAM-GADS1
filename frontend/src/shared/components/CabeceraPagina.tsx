import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Icono } from "./Icono";
import css from "./CabeceraPagina.module.css";

type Miga = { texto: string; a?: string };

type Props = {
  titulo: string;
  descripcion?: string;
  migas?: Miga[];
  acciones?: ReactNode;
  /** Contenido auxiliar bajo el título: etiquetas de estado, metadatos. */
  meta?: ReactNode;
};

export function CabeceraPagina({
  titulo,
  descripcion,
  migas,
  acciones,
  meta,
}: Props) {
  return (
    <header className={css.cabecera}>
      {migas && migas.length > 0 && (
        <nav aria-label="Migas de pan">
          <ol className={css.migas}>
            {migas.map((m, i) => (
              <li key={m.texto}>
                {m.a ? <Link to={m.a}>{m.texto}</Link> : <span>{m.texto}</span>}
                {i < migas.length - 1 && (
                  <Icono nombre="chevronDerecha" tamano={13} />
                )}
              </li>
            ))}
          </ol>
        </nav>
      )}

      <div className={css.fila}>
        <div className={css.identidad}>
          <h1 className={css.titulo}>{titulo}</h1>
          {descripcion && <p className={css.descripcion}>{descripcion}</p>}
          {meta && <div className={css.meta}>{meta}</div>}
        </div>
        {acciones && <div className={css.acciones}>{acciones}</div>}
      </div>
    </header>
  );
}
