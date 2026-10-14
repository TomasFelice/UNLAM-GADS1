import { useQueries } from "@tanstack/react-query";
import { useState } from "react";
import { solicitarApi } from "../../shared/api/clienteApi";
import { SelectorBuscable } from "../../shared/components/SelectorBuscable";
import { useOpcionesSelector } from "../../shared/hooks/useOpcionesSelector";
import { type ServicioEvento } from "./apiCatalogos";

export function SelectorServicios({ valores, alCambiar, id }: { valores: string[]; alCambiar: (valores: string[]) => void; id: string }) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const consulta = useOpcionesSelector<ServicioEvento>({ clave: ["servicios-evento"], ruta: "/event-services", filtros: { active: "true" }, busqueda, abierto });
  const seleccionados = useQueries({ queries: valores.map(valor => ({ queryKey: ["servicios-evento", "detalle", valor],
    queryFn: ({ signal }: { signal: AbortSignal }) => solicitarApi<ServicioEvento>(`/event-services/${valor}`, { signal }) })) });
  return <SelectorBuscable {...consulta} {...{ id, busqueda, alBuscar, abierto, alAbrir }} multiple nombre="Servicios adicionales" marcador="Elegí servicios adicionales"
    opciones={consulta.opciones.map(o => ({ valor: String(o.id), texto: o.name ?? "Servicio" }))}
    seleccionados={valores.map((valor, i) => ({ valor, texto: `${seleccionados[i].data?.name ?? consulta.opciones.find(o => String(o.id) === valor)?.name ?? "Consultando servicio…"}${seleccionados[i].data?.active === false ? " · Inactivo, asociación anterior" : ""}` }))}
    alCambiar={valor => alCambiar(valores.includes(valor) ? valores.filter(v => v !== valor) : [...valores, valor])}
    error={consulta.error || seleccionados.some(c => c.isError)} reintentar={() => { consulta.reintentar(); seleccionados.forEach(c => void c.refetch()); }} />;
}
