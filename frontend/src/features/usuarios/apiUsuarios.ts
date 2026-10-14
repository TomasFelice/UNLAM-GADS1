import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type Usuario = components["schemas"]["UserResponse"];
type PaginaUsuarios = components["schemas"]["PageResponseUserResponse"];
export type AltaUsuario = components["schemas"]["CreateUserRequest"];
export type EdicionUsuario = components["schemas"]["UpdateUserRequest"];
type ReinicioClave = components["schemas"]["ResetPasswordRequest"];
type FiltrosUsuarios = { q?: string; role?: string; active?: string };

export const NOMBRES_ROLES = {
  ADMIN: "Administrador", SALES_MANAGER: "Responsable comercial", SELLER: "Vendedor",
} as const;

export function useUsuarios(pagina: number, filtros: FiltrosUsuarios) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "lastName,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({ queryKey: ["usuarios", "lista", pagina, filtros],
    queryFn: ({ signal }) => solicitarApi<PaginaUsuarios>(`/users?${parametros}`, { signal }) });
}

export function useUsuario(id?: number) {
  return useQuery({ queryKey: ["usuarios", "detalle", id],
    queryFn: ({ signal }) => solicitarApi<Usuario>(`/users/${id}`, { signal }),
    enabled: id !== undefined && Number.isSafeInteger(id) && id > 0 });
}

export function useGuardarUsuario(id?: number) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: (datos: AltaUsuario | EdicionUsuario) => solicitarApi<Usuario>(id ? `/users/${id}` : "/users", {
      method: id ? "PUT" : "POST", body: datos,
    }),
    onSuccess: async usuario => {
      cache.setQueryData(["usuarios", "detalle", usuario.id], usuario);
      await cache.invalidateQueries({ queryKey: ["usuarios"], refetchType: "none" });
      await cache.invalidateQueries({ queryKey: ["usuarios", "asignables"] });
      await Promise.all(["empresas", "contactos", "oportunidades", "actividades"].map(clave => cache.invalidateQueries({ queryKey: [clave] })));
    },
  });
}

export function useRestablecerClave(id: number) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: (datos: ReinicioClave) => solicitarApi<void>(`/users/${id}/reset-password`, { method: "POST", body: datos }),
    onSuccess: async () => {
      cache.setQueryData<Usuario>(["usuarios", "detalle", id], usuario => usuario ? { ...usuario, mustChangePassword: true } : usuario);
      await cache.invalidateQueries({ queryKey: ["usuarios"], refetchType: "none" });
      await cache.invalidateQueries({ queryKey: ["usuarios", "asignables"] });
      await Promise.all(["empresas", "contactos", "oportunidades", "actividades"].map(clave => cache.invalidateQueries({ queryKey: [clave] })));
    },
  });
}
