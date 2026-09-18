import css from "./Avatar.module.css";

type Props = {
  iniciales: string;
  nombre?: string;
  tamano?: "sm" | "md" | "lg";
};

/** El matiz se deriva de las iniciales para que cada persona sea reconocible. */
const MATICES = [
  "var(--marca-turquesa)",
  "var(--marca-azul)",
  "var(--marca-violeta)",
  "var(--marca-indigo)",
  "#3d6f9e",
  "#7a5aa8",
];

export function Avatar({ iniciales, nombre, tamano = "md" }: Props) {
  const indice =
    [...iniciales].reduce((suma, c) => suma + c.charCodeAt(0), 0) %
    MATICES.length;

  return (
    <span
      className={`${css.avatar} ${css[tamano]}`}
      style={{ background: MATICES[indice] }}
      title={nombre}
      aria-hidden={nombre ? undefined : true}
      role={nombre ? "img" : undefined}
      aria-label={nombre}
    >
      {iniciales}
    </span>
  );
}
