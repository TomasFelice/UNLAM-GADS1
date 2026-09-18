import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv, type Plugin } from "vite";

/**
 * Rutas públicas indexables. Tienen que coincidir con las que declara
 * `src/app/router/Rutas.tsx` fuera de `/app` y `/ingresar`.
 */
const RUTAS_PUBLICAS: { ruta: string; prioridad: string; frecuencia: string }[] = [
  { ruta: "/", prioridad: "1.0", frecuencia: "weekly" },
  { ruta: "/nosotros", prioridad: "0.8", frecuencia: "monthly" },
  { ruta: "/contacto", prioridad: "0.8", frecuencia: "monthly" },
  { ruta: "/terminos", prioridad: "0.3", frecuencia: "yearly" },
  { ruta: "/privacidad", prioridad: "0.3", frecuencia: "yearly" },
];

/**
 * Genera `robots.txt` y `sitemap.xml` en el build.
 *
 * Se generan en vez de versionarse para que el dominio viva en un solo lugar
 * (`VITE_SITE_URL`). Un sitemap con una URL que no es la del despliegue es
 * peor que no tener sitemap.
 */
function seoEstatico(urlSitio: string): Plugin {
  return {
    name: "ztech-seo-estatico",
    apply: "build",
    generateBundle() {
      const hoy = new Date().toISOString().slice(0, 10);

      const urls = RUTAS_PUBLICAS.map(
        ({ ruta, prioridad, frecuencia }) =>
          `  <url>\n` +
          `    <loc>${urlSitio}${ruta}</loc>\n` +
          `    <lastmod>${hoy}</lastmod>\n` +
          `    <changefreq>${frecuencia}</changefreq>\n` +
          `    <priority>${prioridad}</priority>\n` +
          `  </url>`,
      ).join("\n");

      this.emitFile({
        type: "asset",
        fileName: "sitemap.xml",
        source:
          `<?xml version="1.0" encoding="UTF-8"?>\n` +
          `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`,
      });

      this.emitFile({
        type: "asset",
        fileName: "robots.txt",
        source: [
          `# ${urlSitio}/robots.txt`,
          "",
          "User-agent: *",
          ...RUTAS_PUBLICAS.map(({ ruta }) => `Allow: ${ruta === "/" ? "/$" : ruta}`),
          "",
          "# La aplicación es privada y no aporta contenido indexable.",
          "Disallow: /app",
          "Disallow: /ingresar",
          "",
          `Sitemap: ${urlSitio}/sitemap.xml`,
          "",
        ].join("\n"),
      });
    },
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  if (env.VERCEL && !env.VITE_API_URL) {
    throw new Error("VITE_API_URL es obligatoria en los builds publicados en Vercel.");
  }

  // En Vercel, VERCEL_PROJECT_PRODUCTION_URL trae el dominio de producción del
  // proyecto sin protocolo. Sirve de respaldo si no se definió VITE_SITE_URL.
  const urlSitio = (
    env.VITE_SITE_URL ||
    (env.VERCEL_PROJECT_PRODUCTION_URL
      ? `https://${env.VERCEL_PROJECT_PRODUCTION_URL}`
      : "http://localhost:5173")
  ).replace(/\/$/, "");

  return {
    plugins: [react(), seoEstatico(urlSitio)],
    define: {
      "import.meta.env.VITE_SITE_URL": JSON.stringify(urlSitio),
      "import.meta.env.VITE_API_URL": JSON.stringify(
        env.VITE_API_URL?.replace(/\/$/, "") || "http://localhost:8080/api/v1",
      ),
    },
  };
});
