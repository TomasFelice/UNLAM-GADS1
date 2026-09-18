import { Link } from "react-router-dom";
import { Seo } from "../../shared/seo/Seo";
import { Logo } from "../../shared/components/Logo";
import { BotonEnlace } from "../../shared/components/Boton";
import css from "./Institucional.module.css";

export function PaginaNoEncontrada() {
  return (
    <>
      <Seo
        titulo="Página no encontrada"
        descripcion="La página que buscás no existe o cambió de dirección. Volvé al inicio de Ztech CRM."
        noIndexar
      />

      <div className={css.noEncontrada}>
        <Link to="/" aria-label="Ztech CRM, inicio">
          <Logo tamano={34} />
        </Link>

        <p className={css.codigoError}>Error 404</p>
        <h1>Esta página no existe</h1>
        <p className={css.noEncontradaTexto}>
          Puede que la dirección esté mal escrita o que el contenido haya
          cambiado de lugar. Desde el inicio vas a encontrar todo el sitio.
        </p>

        <div className={css.noEncontradaAcciones}>
          <BotonEnlace a="/" tamano="lg" icono="flechaIzquierda">
            Volver al inicio
          </BotonEnlace>
          <BotonEnlace a="/contacto" variante="secundario" tamano="lg">
            Reportar el problema
          </BotonEnlace>
        </div>
      </div>
    </>
  );
}
