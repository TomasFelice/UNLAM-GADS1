import { Seo } from "../../shared/seo/Seo";
import { migas } from "../../shared/seo/datosEstructurados";
import { Icono } from "../../shared/components/Icono";
import type { Documento } from "./documentos/parsear";
import css from "./Institucional.module.css";

type Props = {
  documento: Documento;
  ruta: string;
};

export function DocumentoLegal({ documento, ruta }: Props) {
  const { titulo, descripcionSeo, vigencia, resumen, secciones } = documento;

  return (
    <>
      <Seo
        titulo={titulo}
        descripcion={descripcionSeo}
        canonica={ruta}
        datosEstructurados={migas([
          { nombre: "Inicio", ruta: "/" },
          { nombre: titulo, ruta },
        ])}
      />

      <div className={`${css.contenedor} ${css.legal}`}>
        <header className={css.legalCabecera}>
          <p className={css.migaSimple}>Legales</p>
          <h1>{titulo}</h1>
          <p className={css.legalResumen}>{resumen}</p>
          <p className={css.vigencia}>
            <Icono nombre="calendario" tamano={14} />
            Vigente desde el {vigencia}
          </p>
        </header>

        <div className={css.legalCuerpo}>
          <nav className={css.indice} aria-label="Índice del documento">
            <p className={css.indiceTitulo}>Contenido</p>
            <ol>
              {secciones.map((s) => (
                <li key={s.id}>
                  <a href={`#${s.id}`}>{s.titulo.replace(/^\d+\.\s*/, "")}</a>
                </li>
              ))}
            </ol>
          </nav>

          <article className={css.articulo}>
            {secciones.map((s) => (
              <section key={s.id} id={s.id} className={css.clausula}>
                <h2>{s.titulo}</h2>
                {s.bloques.map((b, i) =>
                  b.tipo === "parrafo" ? (
                    <p key={i}>{b.texto}</p>
                  ) : (
                    <ul key={i}>
                      {b.items.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  ),
                )}
              </section>
            ))}
          </article>
        </div>
      </div>
    </>
  );
}
