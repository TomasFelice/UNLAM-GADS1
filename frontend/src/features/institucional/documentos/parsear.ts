/**
 * Lector mínimo de los documentos legales.
 *
 * Los textos viven como Markdown para que se editen sin tocar componentes.
 * El subconjunto que se usa es acotado a propósito: frontmatter, encabezados
 * `##`, párrafos y listas con `-`. No se incorpora una librería de Markdown
 * porque ampliaría la superficie de ataque de un contenido que ya controlamos.
 */

export type Bloque =
  | { tipo: "parrafo"; texto: string }
  | { tipo: "lista"; items: string[] };

export type Seccion = {
  /** Ancla estable para enlazar una cláusula concreta. */
  id: string;
  titulo: string;
  bloques: Bloque[];
};

export type Documento = {
  titulo: string;
  descripcionSeo: string;
  vigencia: string;
  resumen: string;
  secciones: Seccion[];
};

const sinComillas = (v: string) => v.trim().replace(/^"(.*)"$/s, "$1");

const anclar = (titulo: string) =>
  titulo
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

export function parsearDocumento(bruto: string): Documento {
  const coincidencia = bruto.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n([\s\S]*)$/);
  if (!coincidencia) {
    throw new Error("El documento legal no tiene frontmatter.");
  }

  const [, frente, cuerpo] = coincidencia;
  const meta: Record<string, string> = {};
  for (const linea of frente.split(/\r?\n/)) {
    const par = linea.match(/^(\w+):\s*(.*)$/);
    if (par) meta[par[1]] = sinComillas(par[2]);
  }

  const secciones: Seccion[] = [];
  for (const trozo of cuerpo.split(/\r?\n## /).slice(1)) {
    const [encabezado, ...resto] = trozo.split(/\r?\n/);
    const titulo = encabezado.trim();
    const bloques: Bloque[] = [];

    for (const parrafo of resto.join("\n").split(/\r?\n\s*\r?\n/)) {
      const texto = parrafo.trim();
      if (!texto) continue;

      if (texto.startsWith("- ")) {
        bloques.push({
          tipo: "lista",
          items: texto
            .split(/\r?\n/)
            .map((l) => l.replace(/^-\s*/, "").replace(/;$/, "").trim())
            .filter(Boolean),
        });
      } else {
        bloques.push({ tipo: "parrafo", texto: texto.replace(/\s*\n\s*/g, " ") });
      }
    }

    secciones.push({ id: anclar(titulo), titulo, bloques });
  }

  return {
    titulo: meta.titulo ?? "",
    descripcionSeo: meta.descripcionSeo ?? "",
    vigencia: meta.vigencia ?? "",
    resumen: meta.resumen ?? "",
    secciones,
  };
}
