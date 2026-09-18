import { useQuery } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type UsuarioAsignable = components["schemas"]["UserResponse"];

export function useUsuariosAsignables() {
  return useQuery({
    queryKey: ["usuarios", "asignables"],
    queryFn: ({ signal }) => solicitarApi<UsuarioAsignable[]>("/users/assignable", { signal }),
    staleTime: 5 * 60_000,
  });
}
