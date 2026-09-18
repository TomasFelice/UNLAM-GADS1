import { useQuery } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Actividad = components["schemas"]["ActivityResponse"];
export type PaginaActividades = components["schemas"]["PageResponseActivityResponse"];
type Asociacion = { tipo: "companies" | "contacts" | "opportunities"; id: number };
export const useActividades = (asociacion?: Asociacion, size = 20) => useQuery({ queryKey: ["actividades", asociacion, size], queryFn: ({ signal }) => solicitarApi<PaginaActividades>(`${asociacion ? `/${asociacion.tipo}/${asociacion.id}` : ""}/activities?page=0&size=${size}&sort=occurredAt,desc`, { signal }) });
