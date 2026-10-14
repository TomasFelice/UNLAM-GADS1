import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Servicio = components["schemas"]["EventServiceResponse"];

export function useServicios(pagina = 0, filtros: { q?: string; active?: string } = {}) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "name,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({ queryKey: ["servicios-evento", "lista", pagina, filtros], queryFn: ({ signal }) => solicitarApi<components["schemas"]["PageResponseEventServiceResponse"]>(`/event-services?${parametros}`, { signal }) });
}
export function useServicio(id?: number) {
  return useQuery({ queryKey: ["servicios-evento", "detalle", String(id)], enabled: Number.isSafeInteger(id) && id! > 0,
    queryFn: ({ signal }) => solicitarApi<Servicio>(`/event-services/${id}`, { signal }) });
}
export function useGuardarServicio(id?: number) {
  const cache = useQueryClient();
  return useMutation({ mutationFn: (datos: components["schemas"]["EventServiceRequest"]) => solicitarApi<Servicio>(id ? `/event-services/${id}` : "/event-services", { method: id ? "PUT" : "POST", body: datos }),
    onSuccess: async () => { await Promise.all(["servicios-evento", "oportunidades"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); } });
}
