import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type EmpresaResumen = components["schemas"]["CompanyResponse"];
export type EmpresaDetalle = components["schemas"]["CompanyDetailResponse"];
export type SolicitudEmpresa = components["schemas"]["CompanyRequest"];
export type PaginaEmpresas = components["schemas"]["PageResponseCompanyResponse"];
export type FiltrosEmpresas = { q?: string; status?: string; originId?: string; salesRepId?: string };

export const clavesEmpresas = {
  todas: ["empresas"] as const,
  lista: (pagina: number, filtros: FiltrosEmpresas) => ["empresas", "lista", pagina, filtros] as const,
  detalle: (id: number) => ["empresas", "detalle", id] as const,
};

export function useEmpresas(pagina = 0, filtros: FiltrosEmpresas = {}) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "businessName,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({
    queryKey: clavesEmpresas.lista(pagina, filtros),
    queryFn: ({ signal }) =>
      solicitarApi<PaginaEmpresas>(`/companies?${parametros}`, { signal }),
  });
}

export function useEmpresa(id?: number) {
  return useQuery({
    queryKey: clavesEmpresas.detalle(id ?? 0),
    queryFn: ({ signal }) => solicitarApi<EmpresaDetalle>(`/companies/${id}`, { signal }),
    enabled: id !== undefined,
  });
}

export function useGuardarEmpresa(id?: number) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: (solicitud: SolicitudEmpresa) =>
      solicitarApi<EmpresaResumen>(id ? `/companies/${id}` : "/companies", {
        method: id ? "PUT" : "POST",
        body: solicitud,
      }),
    onSuccess: async (empresa) => {
      await cache.invalidateQueries({ queryKey: clavesEmpresas.todas });
      if (empresa.id) {
        await cache.invalidateQueries({ queryKey: clavesEmpresas.detalle(empresa.id) });
      }
    },
  });
}
