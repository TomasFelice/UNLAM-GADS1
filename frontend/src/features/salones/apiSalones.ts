import { useQuery } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Salon = components["schemas"]["VenueResponse"];
export const useSalones = () => useQuery({ queryKey: ["salones"], queryFn: ({ signal }) => solicitarApi<Salon[]>("/venues", { signal }) });
