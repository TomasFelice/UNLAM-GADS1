import { Icono } from "./Icono";
import { useTema } from "../tema/tema";
import css from "./BotonTema.module.css";

type Props = {
  /** Reemplaza el estilo propio, para que encaje con la barra que lo contiene. */
  className?: string;
};

/**
 * Alterna entre el tema claro y el oscuro.
 *
 * Muestra el icono del tema al que se va a cambiar, no el del activo: el botón
 * anuncia lo que hace, y el texto accesible dice lo mismo para quien no ve el
 * icono. Por eso no lleva `aria-pressed`: no es un interruptor de un estado,
 * es una acción que cambia de nombre.
 */
export function BotonTema({ className }: Props) {
  const [tema, alternar] = useTema();
  const oscuro = tema === "oscuro";
  const etiqueta = oscuro ? "Activar el tema claro" : "Activar el tema oscuro";

  return (
    <button
      type="button"
      className={className ?? css.boton}
      onClick={alternar}
      aria-label={etiqueta}
      title={etiqueta}
    >
      <Icono nombre={oscuro ? "sol" : "luna"} tamano={18} className={css.icono} />
    </button>
  );
}
