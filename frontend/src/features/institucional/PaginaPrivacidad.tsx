import bruto from "./documentos/privacidad.md?raw";
import { parsearDocumento } from "./documentos/parsear";
import { DocumentoLegal } from "./DocumentoLegal";

const documento = parsearDocumento(bruto);

export function PaginaPrivacidad() {
  return <DocumentoLegal documento={documento} ruta="/privacidad" />;
}
