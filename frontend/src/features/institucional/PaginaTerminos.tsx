import bruto from "./documentos/terminos.md?raw";
import { parsearDocumento } from "./documentos/parsear";
import { DocumentoLegal } from "./DocumentoLegal";

const documento = parsearDocumento(bruto);

export function PaginaTerminos() {
  return <DocumentoLegal documento={documento} ruta="/terminos" />;
}
