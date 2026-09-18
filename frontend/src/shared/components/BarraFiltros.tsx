import type { ReactNode } from "react";
import { Icono } from "./Icono";
import css from "./BarraFiltros.module.css";

type PropsBarra = {
  marcador: string;
  valor?: string;
  alCambiar?: (valor: string) => void;
  children?: ReactNode;
};

/** Barra de búsqueda y filtros controlados mediante la URL del listado. */
export function BarraFiltros({ marcador, valor, alCambiar, children }: PropsBarra) {
  return (
    <div className={css.barra} role="search">
      <div className={css.busqueda}>
        <Icono nombre="buscar" tamano={16} />
        <input type="search" placeholder={marcador} aria-label={marcador} value={valor ?? ""}
          onChange={(evento) => alCambiar?.(evento.target.value)} />
      </div>
      {children && <div className={css.filtros}>{children}</div>}
    </div>
  );
}

type PropsFiltro = {
  etiqueta: string;
  opciones: Array<string | { valor: string; texto: string }>;
  valor?: string;
  alCambiar?: (valor: string) => void;
};

export function Filtro({ etiqueta, opciones, valor, alCambiar }: PropsFiltro) {
  return (
    <label className={css.filtro}>
      <span className="solo-lectores">{etiqueta}</span>
      <select value={valor ?? ""} aria-label={etiqueta} onChange={(evento) => alCambiar?.(evento.target.value)}>
        <option value="">{etiqueta}</option>
        {opciones.map((opcion) => {
          const item = typeof opcion === "string" ? { valor: opcion, texto: opcion } : opcion;
          return <option key={item.valor} value={item.valor}>
            {item.texto}
          </option>
        })}
      </select>
      <Icono nombre="chevronAbajo" tamano={14} />
    </label>
  );
}

export function FiltroFecha({
  etiqueta,
  valor,
  alCambiar,
}: {
  etiqueta: string;
  valor?: string;
  alCambiar?: (valor: string) => void;
}) {
  return (
    <label className={css.filtroFecha}>
      <span>{etiqueta}</span>
      <input
        type="date"
        value={valor ?? ""}
        aria-label={etiqueta}
        onChange={(evento) => alCambiar?.(evento.target.value)}
      />
    </label>
  );
}

export function PieListado({
  mostrados,
  total,
  entidad,
  pagina = 0,
  totalPaginas = 1,
  alCambiarPagina,
}: {
  mostrados: number;
  total: number;
  entidad: string;
  pagina?: number;
  totalPaginas?: number;
  alCambiarPagina?: (pagina: number) => void;
}) {
  return (
    <>
      <p className={css.conteo}>
        Mostrando {mostrados} de {total} {entidad}
      </p>
      <nav className={css.paginacion} aria-label="Paginación">
        <button type="button" disabled={pagina <= 0} aria-label="Página anterior" onClick={() => alCambiarPagina?.(pagina - 1)}>
          <Icono nombre="chevronIzquierda" tamano={15} />
        </button>
        <span aria-current="page">{pagina + 1}</span>
        <button type="button" disabled={pagina + 1 >= totalPaginas} aria-label="Página siguiente" onClick={() => alCambiarPagina?.(pagina + 1)}>
          <Icono nombre="chevronDerecha" tamano={15} />
        </button>
      </nav>
    </>
  );
}
