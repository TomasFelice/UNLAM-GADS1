import { SITIO, urlAbsoluta } from "./sitio";

/** Organización que publica el sitio. Va en el layout público. */
export const organizacion = {
  "@context": "https://schema.org",
  "@type": "Organization",
  name: SITIO.nombreLargo,
  alternateName: SITIO.nombre,
  url: SITIO.url,
  logo: urlAbsoluta("/icono-512.png"),
  description: `${SITIO.lema}. Proyecto académico de ${SITIO.institucion}.`,
  email: SITIO.email,
  address: {
    "@type": "PostalAddress",
    addressLocality: "San Justo",
    addressRegion: "Provincia de Buenos Aires",
    addressCountry: "AR",
  },
};

/** Sitio web, con la acción de búsqueda deshabilitada por ahora. */
export const sitioWeb = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: SITIO.nombre,
  url: SITIO.url,
  inLanguage: SITIO.idioma,
  publisher: { "@type": "Organization", name: SITIO.nombreLargo },
};

/** Descripción del producto para la página de inicio. */
export const aplicacion = {
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  name: SITIO.nombreLargo,
  applicationCategory: "BusinessApplication",
  applicationSubCategory: "CRM",
  operatingSystem: "Navegador web",
  inLanguage: SITIO.idioma,
  url: SITIO.url,
  image: urlAbsoluta(SITIO.imagenSocial),
  description:
    "CRM especializado en la gestión comercial de salones de eventos corporativos: empresas, contactos, oportunidades, embudo por etapas, actividades e historial.",
  featureList: [
    "Gestión de empresas y contactos",
    "Oportunidades con salón y responsable comercial",
    "Embudo comercial por etapas",
    "Disponibilidad y capacidad de salones",
    "Actividades e historial comercial",
    "Roles y permisos",
  ],
};

/** Migas de pan para las páginas internas del sitio público. */
export const migas = (items: { nombre: string; ruta: string }[]) => ({
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  itemListElement: items.map((item, i) => ({
    "@type": "ListItem",
    position: i + 1,
    name: item.nombre,
    item: urlAbsoluta(item.ruta),
  })),
});

/** Preguntas frecuentes de la página de contacto. */
export const preguntasFrecuentes = (
  items: { pregunta: string; respuesta: string }[],
) => ({
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: items.map((item) => ({
    "@type": "Question",
    name: item.pregunta,
    acceptedAnswer: { "@type": "Answer", text: item.respuesta },
  })),
});
