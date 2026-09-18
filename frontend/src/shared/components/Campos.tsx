import { useId } from "react";
import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import css from "./Campos.module.css";

type PropsCampo = {
  etiqueta: string;
  ayuda?: string;
  error?: string;
  requerido?: boolean;
  /** Ocupa las dos columnas de la grilla del formulario. */
  ancho?: boolean;
  children: (id: string, describedBy?: string) => ReactNode;
};

/**
 * Envoltorio de campo: asocia `label`, control y texto de ayuda.
 * La validación llega junto con React Hook Form y Zod (ver DF-06).
 */
export function Campo({
  etiqueta,
  ayuda,
  error,
  requerido,
  ancho,
  children,
}: PropsCampo) {
  const id = useId();
  const idAyuda = ayuda ? `${id}-ayuda` : undefined;
  const idError = error ? `${id}-error` : undefined;
  const describedBy = [idAyuda, idError].filter(Boolean).join(" ") || undefined;

  return (
    <div className={`${css.campo} ${ancho ? css.ancho : ""}`}>
      <label className={css.etiqueta} htmlFor={id}>
        {etiqueta}
        {requerido && (
          <span className={css.requerido} aria-hidden="true">
            *
          </span>
        )}
      </label>
      {children(id, describedBy)}
      {ayuda && (
        <p className={css.ayuda} id={idAyuda}>
          {ayuda}
        </p>
      )}
      {error && (
        <p className={css.error} id={idError} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export function Entrada(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={css.control} {...props} />;
}

export function AreaTexto(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={`${css.control} ${css.area}`} rows={4} {...props} />;
}

export function Selector({
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className={css.envoltorioSelector}>
      <select className={`${css.control} ${css.selector}`} {...props}>
        {children}
      </select>
      <svg
        className={css.flecha}
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M6 9.5l6 6 6-6" />
      </svg>
    </div>
  );
}

type PropsGrilla = {
  children: ReactNode;
  /** Título de la sección del formulario. */
  titulo?: string;
  descripcion?: string;
};

export function GrupoCampos({ children, titulo, descripcion }: PropsGrilla) {
  return (
    <fieldset className={css.grupo}>
      {titulo && (
        <legend className={css.leyenda}>
          {titulo}
          {descripcion && <span className={css.leyendaAyuda}>{descripcion}</span>}
        </legend>
      )}
      <div className={css.grilla}>{children}</div>
    </fieldset>
  );
}
