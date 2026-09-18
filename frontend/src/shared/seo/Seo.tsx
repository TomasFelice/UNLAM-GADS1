import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { SITIO, urlAbsoluta } from "./sitio";

type Props = {
  /** Título propio de la página. Se completa con el nombre del sitio. */
  titulo: string;
  /**
   * Meta description. Objetivo: entre 140 y 158 caracteres.
   * Las pantallas de la aplicación pueden dejarla vacía: no se indexan.
   */
  descripcion: string;
  /** Ruta canónica. Por defecto, la ruta actual. */
  canonica?: string;
  /** Imagen para Open Graph y Twitter Card. */
  imagen?: string;
  /** `true` en las pantallas de la aplicación, que no deben indexarse. */
  noIndexar?: boolean;
  /** Tipo Open Graph. */
  tipo?: "website" | "article";
  /** Datos estructurados JSON-LD adicionales de la página. */
  datosEstructurados?: Record<string, unknown> | Record<string, unknown>[];
};

const ATRIBUTO = "data-seo";

/**
 * Crea o actualiza una etiqueta del `<head>`.
 *
 * Si `index.html` ya declara esa etiqueta, la adopta en lugar de agregar una
 * segunda: dos `meta description` o dos `canonical` en el documento son un
 * error de SEO, y los rastreadores se quedan con la primera.
 */
function etiqueta(
  selector: string,
  crear: () => HTMLElement,
  aplicar: (el: HTMLElement) => void,
) {
  let el = document.head.querySelector<HTMLElement>(selector);
  if (!el) {
    el = crear();
    document.head.appendChild(el);
  }
  el.setAttribute(ATRIBUTO, "");
  aplicar(el);
}

function meta(nombre: string, contenido: string, porPropiedad = false) {
  const clave = porPropiedad ? "property" : "name";
  etiqueta(
    `meta[${clave}="${nombre}"]`,
    () => {
      const el = document.createElement("meta");
      el.setAttribute(clave, nombre);
      return el;
    },
    (el) => el.setAttribute("content", contenido),
  );
}

function enlace(rel: string, href: string) {
  etiqueta(
    `link[rel="${rel}"]`,
    () => {
      const el = document.createElement("link");
      el.setAttribute("rel", rel);
      return el;
    },
    (el) => el.setAttribute("href", href),
  );
}

/**
 * Gestiona los metadatos del documento por ruta.
 *
 * Se resuelve con un hook propio en lugar de una dependencia externa: la
 * maqueta necesita unas pocas etiquetas por página y el control explícito
 * evita duplicados con las que ya declara `index.html`.
 */
export function Seo({
  titulo,
  descripcion,
  canonica,
  imagen = SITIO.imagenSocial,
  noIndexar = false,
  tipo = "website",
  datosEstructurados,
}: Props) {
  const { pathname } = useLocation();
  const ruta = canonica ?? pathname;
  const jsonLd = datosEstructurados ? JSON.stringify(datosEstructurados) : null;
  const resumen = descripcion || `${SITIO.nombre}. ${SITIO.lema}.`;

  useEffect(() => {
    const completo =
      titulo === SITIO.nombre ? titulo : `${titulo} | ${SITIO.nombre}`;
    const url = urlAbsoluta(ruta);
    const imagenAbsoluta = urlAbsoluta(imagen);

    document.title = completo;
    meta("description", resumen);
    meta(
      "robots",
      noIndexar
        ? "noindex, nofollow"
        : "index, follow, max-image-preview:large, max-snippet:-1",
    );
    enlace("canonical", url);

    meta("og:site_name", SITIO.nombre, true);
    meta("og:locale", "es_AR", true);
    meta("og:type", tipo, true);
    meta("og:title", completo, true);
    meta("og:description", resumen, true);
    meta("og:url", url, true);
    meta("og:image", imagenAbsoluta, true);
    meta("og:image:alt", `${SITIO.nombre}. ${SITIO.lema}.`, true);

    meta("twitter:card", "summary_large_image");
    meta("twitter:title", completo);
    meta("twitter:description", resumen);
    meta("twitter:image", imagenAbsoluta);
  }, [titulo, resumen, ruta, imagen, noIndexar, tipo]);

  useEffect(() => {
    if (!jsonLd) return;
    const el = document.createElement("script");
    el.type = "application/ld+json";
    el.textContent = jsonLd;
    document.head.appendChild(el);
    return () => {
      el.remove();
    };
  }, [jsonLd]);

  return null;
}
