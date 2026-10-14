import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Actividad = components["schemas"]["ActivityResponse"];
export type PaginaActividades = components["schemas"]["PageResponseActivityResponse"];
export type Asociacion = { tipo: "companies" | "contacts" | "opportunities"; id: number };
export type SolicitudActividad = components["schemas"]["CreateActivityRequest"];
export const useActividades = (asociacion?: Asociacion, cantidad = 20, pagina = 0) => useQuery({
  queryKey: ["actividades", asociacion, cantidad, pagina],
  enabled: !asociacion || (Number.isSafeInteger(asociacion.id) && asociacion.id > 0),
  queryFn: ({ signal }) => solicitarApi<PaginaActividades>(`${asociacion ? `/${asociacion.tipo}/${asociacion.id}` : ""}/activities?page=${pagina}&size=${cantidad}&sort=occurredAt,desc`, { signal }),
});
export function useRegistrarActividad() {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: (datos: SolicitudActividad) => solicitarApi<Actividad>("/activities", { method: "POST", body: datos }),
    onSuccess: async () => {
      await Promise.all(["actividades", "empresas", "contactos", "oportunidades"].map(clave => cache.invalidateQueries({ queryKey: [clave] })));
    },
  });
}
