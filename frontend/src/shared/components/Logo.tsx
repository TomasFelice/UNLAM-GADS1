import isotipo from "../../assets/marca/ztech-crm-isotipo.png";
import css from "./Logo.module.css";

type Props = {
  /** `completo` incluye la bajada "Executive CRM"; `compacto` sólo el wordmark. */
  variante?: "completo" | "compacto" | "isotipo";
  /** Alto del isotipo en píxeles. */
  tamano?: number;
  /** `claro` para fondos oscuros. */
  tono?: "oscuro" | "claro";
};

export function Logo({
  variante = "completo",
  tamano = 34,
  tono = "oscuro",
}: Props) {
  return (
    <span
      className={`${css.logo} ${tono === "claro" ? css.claro : ""}`}
      data-variante={variante}
    >
      <img
        src={isotipo}
        alt=""
        width={Math.round((tamano * 221) / 183)}
        height={tamano}
        className={css.marca}
        style={{ height: tamano }}
      />
      {variante !== "isotipo" && (
        <span className={css.texto}>
          <span className={css.wordmark}>Ztech</span>
          {variante === "completo" && (
            <span className={css.bajada}>Executive CRM</span>
          )}
        </span>
      )}
      <span className="solo-lectores">Ztech Executive CRM</span>
    </span>
  );
}
