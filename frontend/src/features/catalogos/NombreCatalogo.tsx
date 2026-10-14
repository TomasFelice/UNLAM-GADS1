import { useEntradaCatalogo, type Catalogo } from "./apiCatalogos";
import { Boton } from "../../shared/components/Boton";

export function NombreCatalogo({ catalogo, id }: { catalogo: Catalogo; id?: number }) {
  const consulta = useEntradaCatalogo(catalogo, id);
  if (!id) return <>Sin especificar</>;
  if (consulta.isError) return <span role="alert">No pudimos consultar esta referencia. <Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton></span>;
  return <>{consulta.data ? `${consulta.data.name}${!consulta.data.active ? " (inactivo)" : ""}` : "Consultando…"}</>;
}
