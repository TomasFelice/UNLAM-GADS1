import { Seo } from "../../shared/seo/Seo";
import { migas } from "../../shared/seo/datosEstructurados";
import { Icono } from "../../shared/components/Icono";
import { BotonEnlace } from "../../shared/components/Boton";
import { SITIO } from "../../shared/seo/sitio";
import {
  NOSOTROS_HERO,
  NOSOTROS_HISTORIA,
  NOSOTROS_STACK,
  NOSOTROS_VALORES,
  SEO_NOSOTROS,
} from "./contenido";
import css from "./Institucional.module.css";

export function PaginaNosotros() {
  return (
    <>
      <Seo
        titulo={SEO_NOSOTROS.titulo}
        descripcion={SEO_NOSOTROS.descripcion}
        canonica="/nosotros"
        datosEstructurados={migas([
          { nombre: "Inicio", ruta: "/" },
          { nombre: "Nosotros", ruta: "/nosotros" },
        ])}
      />

      <header className={css.portada}>
        <div className={css.contenedor}>
          <p className={css.migaSimple}>Nosotros</p>
          <h1>{NOSOTROS_HERO.titular}</h1>
          <p className={css.portadaBajada}>{NOSOTROS_HERO.bajada}</p>
        </div>
      </header>

      <div className={css.contenedor}>
        {/* --- Historia ------------------------------------------------ */}
        <section className={css.seccion} aria-labelledby="titulo-historia">
          <div className={css.dosColumnas}>
            <h2 id="titulo-historia" className={css.tituloLateral}>
              {NOSOTROS_HISTORIA.titulo}
            </h2>
            <div className={css.prosa}>
              {NOSOTROS_HISTORIA.parrafos.map((p) => (
                <p key={p.slice(0, 40)}>{p}</p>
              ))}
            </div>
          </div>
        </section>

        {/* --- Valores -------------------------------------------------- */}
        <section className={css.seccion} aria-labelledby="titulo-valores">
          <div className={css.dosColumnas}>
            <h2 id="titulo-valores" className={css.tituloLateral}>
              {NOSOTROS_VALORES.titulo}
            </h2>
            <ul className={css.listaValores}>
              {NOSOTROS_VALORES.items.map((v) => (
                <li key={v.titulo}>
                  <span className={css.valorIcono}>
                    <Icono nombre={v.icono} tamano={18} />
                  </span>
                  <div>
                    <h3>{v.titulo}</h3>
                    <p>{v.texto}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* --- Stack ---------------------------------------------------- */}
        <section className={css.seccion} aria-labelledby="titulo-stack">
          <div className={css.dosColumnas}>
            <div>
              <h2 id="titulo-stack" className={css.tituloLateral}>
                {NOSOTROS_STACK.titulo}
              </h2>
              <p className={css.tituloLateralBajada}>{NOSOTROS_STACK.bajada}</p>
            </div>
            <dl className={css.stack}>
              {NOSOTROS_STACK.items.map((s) => (
                <div key={s.titulo} className={css.stackItem}>
                  <dt>{s.titulo}</dt>
                  <dd>{s.texto}</dd>
                </div>
              ))}
            </dl>
          </div>
        </section>

        {/* --- Cierre --------------------------------------------------- */}
        <section className={css.seccion}>
          <div className={css.cajaCierre}>
            <div>
              <h2>Conocé el producto por dentro</h2>
              <p>
                La demo recorre el flujo completo, de la empresa a la reserva
                confirmada, con datos de ejemplo. Si tenés dudas sobre el
                proyecto, escribinos a{" "}
                <a href={`mailto:${SITIO.email}`}>{SITIO.email}</a>.
              </p>
            </div>
            <div className={css.cierreAcciones}>
              <BotonEnlace a="/app" tamano="lg" iconoFinal="flechaDerecha">
                Ver la demo
              </BotonEnlace>
              <BotonEnlace a="/contacto" variante="secundario" tamano="lg">
                Contacto
              </BotonEnlace>
            </div>
          </div>
        </section>
      </div>
    </>
  );
}
