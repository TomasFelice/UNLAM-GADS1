import { Link } from "react-router-dom";
import { Seo } from "../../shared/seo/Seo";
import { aplicacion } from "../../shared/seo/datosEstructurados";
import { Icono } from "../../shared/components/Icono";
import { BotonEnlace } from "../../shared/components/Boton";
import { VistaPreviaEmbudo } from "./VistaPreviaEmbudo";
import { CAPACIDADES, CIERRE, COMO_FUNCIONA, HERO, PROBLEMA, SEO } from "./contenido";
import css from "./PaginaInicio.module.css";

/** Cifras del propio producto, verificables en la maqueta. */
const DATOS_PRODUCTO = [
  { valor: "7", texto: "etapas en el embudo comercial" },
  { valor: "3", texto: "roles con permisos diferenciados" },
  { valor: "50", texto: "asistentes como tope por evento" },
];

export function PaginaInicio() {
  return (
    <>
      <Seo
        titulo={SEO.titulo}
        descripcion={SEO.descripcion}
        canonica="/"
        datosEstructurados={aplicacion}
      />

      {/* --- Portada --------------------------------------------------- */}
      <section className={css.hero}>
        <div className={css.heroFondo} aria-hidden="true" />
        <div className={`${css.contenedor} ${css.heroGrilla}`}>
          <div className={css.heroTexto}>
            <p className={css.kicker}>
              <span className={css.kickerPunto} aria-hidden="true" />
              {HERO.kicker}
            </p>
            <h1 className={css.titular}>{HERO.titular}</h1>
            <p className={css.bajada}>{HERO.bajada}</p>

            <div className={css.heroAcciones}>
              <BotonEnlace a="/app" tamano="lg" iconoFinal="flechaDerecha">
                {HERO.ctaPrimario}
              </BotonEnlace>
              <BotonEnlace a="#como-funciona" variante="secundario" tamano="lg">
                {HERO.ctaSecundario}
              </BotonEnlace>
            </div>

            <p className={css.heroNota}>
              Proyecto académico en desarrollo con un recorrido comercial conectado a la API.
            </p>
          </div>

          <VistaPreviaEmbudo />
        </div>

        <div className={`${css.contenedor} ${css.franja}`}>
          <dl className={css.datos}>
            {DATOS_PRODUCTO.map((d) => (
              <div key={d.texto} className={css.dato}>
                <dt className={css.datoValor}>{d.valor}</dt>
                <dd className={css.datoTexto}>{d.texto}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* --- Problema -------------------------------------------------- */}
      <section className={`${css.contenedor} ${css.seccion}`}>
        <header className={css.encabezadoSeccion}>
          <h2>{PROBLEMA.titulo}</h2>
          <p>{PROBLEMA.bajada}</p>
        </header>

        <ul className={css.listaProblemas}>
          {PROBLEMA.items.map((item) => (
            <li key={item.titulo} className={css.problema}>
              <h3>{item.titulo}</h3>
              <p>{item.texto}</p>
            </li>
          ))}
        </ul>
      </section>

      {/* --- Capacidades ----------------------------------------------- */}
      <section
        id="capacidades"
        className={`${css.contenedor} ${css.seccion}`}
        aria-labelledby="titulo-capacidades"
      >
        <header className={css.encabezadoSeccion}>
          <h2 id="titulo-capacidades">{CAPACIDADES.titulo}</h2>
          <p>{CAPACIDADES.bajada}</p>
        </header>

        <ul className={css.grillaCapacidades}>
          {CAPACIDADES.items.map((item) => (
            <li key={item.titulo} className={css.capacidad}>
              <span className={css.capacidadIcono}>
                <Icono nombre={item.icono} tamano={19} />
              </span>
              <h3>{item.titulo}</h3>
              <p>{item.texto}</p>
            </li>
          ))}
        </ul>
      </section>

      {/* --- Cómo funciona --------------------------------------------- */}
      <section
        id="como-funciona"
        className={css.bloquePasos}
        aria-labelledby="titulo-pasos"
      >
        <div className={css.contenedor}>
          <header className={css.encabezadoSeccion}>
            <h2 id="titulo-pasos">{COMO_FUNCIONA.titulo}</h2>
          </header>

          <ol className={css.pasos}>
            {COMO_FUNCIONA.pasos.map((paso, i) => (
              <li key={paso.titulo} className={css.paso}>
                <span className={css.pasoNumero} aria-hidden="true">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <h3>{paso.titulo}</h3>
                <p>{paso.texto}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* --- Cierre ----------------------------------------------------- */}
      <section className={`${css.contenedor} ${css.seccion}`}>
        <div className={css.cierre}>
          <div>
            <h2>{CIERRE.titulo}</h2>
            <p>{CIERRE.bajada}</p>
          </div>
          <div className={css.cierreAcciones}>
            <BotonEnlace a="/app" tamano="lg" iconoFinal="flechaDerecha">
              {CIERRE.cta}
            </BotonEnlace>
            <Link to="/contacto" className={css.cierreEnlace}>
              Escribinos una consulta
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
