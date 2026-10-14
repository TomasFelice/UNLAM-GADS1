import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Salon = components["schemas"]["VenueResponse"];
export function useSalones(pagina = 0, filtros: { q?: string; status?: string } = {}) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "name,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({ queryKey: ["salones", "lista", pagina, filtros], queryFn: ({ signal }) => solicitarApi<components["schemas"]["PageResponseVenueResponse"]>(`/venues?${parametros}`, { signal }) });
}
export function useSalon(id?: number) {
  return useQuery({ queryKey: ["salones", "detalle", id], enabled: Number.isSafeInteger(id) && id! > 0,
    queryFn: ({ signal }) => solicitarApi<Salon>(`/venues/${id}`, { signal }) });
}

export function useGuardarSalon(id?: number) {
  const cache = useQueryClient();
  return useMutation({ mutationFn: (datos: components["schemas"]["VenueRequest"]) => solicitarApi<Salon>(id ? `/venues/${id}` : "/venues", { method: id ? "PUT" : "POST", body: datos }),
    onSuccess: async () => { await Promise.all(["salones", "oportunidades"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); } });
}
