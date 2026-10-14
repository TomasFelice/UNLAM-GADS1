import { useQueries } from "@tanstack/react-query";
import { solicitarApi } from "../../shared/api/clienteApi";
import { Boton } from "../../shared/components/Boton";
import type { ServicioEvento } from "./apiCatalogos";

export function ResumenServicios({ ids }: { ids: number[] }) {
  const consultas = useQueries({ queries: ids.map(id => ({ queryKey: ["servicios-evento", "detalle", String(id)],
    queryFn: ({ signal }: { signal: AbortSignal }) => solicitarApi<ServicioEvento>(`/event-services/${id}`, { signal }) })) });
  if (!ids.length) return <span>Sin servicios adicionales</span>;
  return <ul>{consultas.map((consulta, indice) => <li key={ids[indice]}>
    {consulta.isError ? <>No pudimos consultar el servicio. <Boton type="button" variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton></>
      : consulta.isPending ? "Cargando servicio…" : <>{consulta.data.name}{!consulta.data.active && " · Inactivo"}</>}
  </li>)}</ul>;
}
