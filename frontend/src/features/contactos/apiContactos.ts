import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type Contacto = components["schemas"]["ContactResponse"];
export type ContactoDetalle = components["schemas"]["ContactDetailResponse"];
export type SolicitudContacto = components["schemas"]["ContactRequest"];
export type PaginaContactos = components["schemas"]["PageResponseContactResponse"];
export type FiltrosContactos = { q?: string; status?: string; originId?: string; salesRepId?: string; companyId?: string };
export const clavesContactos = { todas: ["contactos"] as const, lista: (p: number, f: FiltrosContactos) => ["contactos", "lista", p, f] as const, detalle: (id: number) => ["contactos", "detalle", id] as const };
const parametros = (pagina: number, filtros: FiltrosContactos) => {
  const p = new URLSearchParams({ page: String(pagina), size: "20", sort: "lastName,asc" });
  Object.entries(filtros).forEach(([k, v]) => { if (v) p.set(k, v); }); return p;
};
export const useContactos = (pagina = 0, filtros: FiltrosContactos = {}, habilitado = true) => useQuery({ enabled: habilitado, queryKey: clavesContactos.lista(pagina, filtros), queryFn: ({ signal }) => solicitarApi<PaginaContactos>(`/contacts?${parametros(pagina, filtros)}`, { signal }) });
export const useContacto = (id?: number) => useQuery({ queryKey: clavesContactos.detalle(id ?? 0), queryFn: ({ signal }) => solicitarApi<ContactoDetalle>(`/contacts/${id}`, { signal }), enabled: Number.isSafeInteger(id) && id! > 0 });
export const useGuardarContacto = (id?: number) => { const cache = useQueryClient(); return useMutation({ mutationFn: (body: SolicitudContacto) => solicitarApi<Contacto>(id ? `/contacts/${id}` : "/contacts", { method: id ? "PUT" : "POST", body }), onSuccess: async (value) => { await Promise.all(["empresas", "contactos", "oportunidades", "actividades"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); if (value.id) await cache.invalidateQueries({ queryKey: clavesContactos.detalle(value.id) }); } }); };
