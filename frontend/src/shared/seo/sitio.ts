/**
 * Constantes del sitio usadas por SEO, datos estructurados y pie de página.
 *
 * `url` viene de `VITE_SITE_URL`, que resuelve `vite.config.ts` y que también
 * alimenta el `robots.txt` y el `sitemap.xml` generados en el build: el
 * dominio se define en un solo lugar. En desarrollo cae a localhost.
 */
export const SITIO = {
  nombre: "Ztech CRM",
  nombreLargo: "Ztech — Executive CRM",
  lema: "CRM para salones de eventos corporativos",
  url: import.meta.env.VITE_SITE_URL,
  imagenSocial: "/imagenes/ztech-crm-og.jpg",
  email: "contacto@ztechcrm.com.ar",
  idioma: "es-AR",
  institucion: "Universidad Nacional de La Matanza",
  materia: "Gestión Aplicada al Desarrollo de Software II",
  localidad: "San Justo, Provincia de Buenos Aires, Argentina",
} as const;

export const urlAbsoluta = (ruta: string): string =>
  ruta.startsWith("http") ? ruta : `${SITIO.url}${ruta}`;
