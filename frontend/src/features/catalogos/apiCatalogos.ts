import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type Etapa = components["schemas"]["StageResponse"];
export type Origen = components["schemas"]["OriginResponse"];
export type TipoEvento = components["schemas"]["EventTypeResponse"];
export type ServicioEvento = components["schemas"]["EventServiceResponse"];
export type TipoActividad = components["schemas"]["ActivityTypeResponse"];
export type EntradaCatalogo = Origen | TipoEvento | TipoActividad | components["schemas"]["LossReasonResponse"];
type PaginaCatalogo = components["schemas"]["PageResponseOriginResponse"] | components["schemas"]["PageResponseEventTypeResponse"] | components["schemas"]["PageResponseActivityTypeResponse"] | components["schemas"]["PageResponseLossReasonResponse"];

export const CATALOGOS = {
  origenes: { ruta: "origins", titulo: "Orígenes comerciales", singular: "origen", descripcion: "Cómo llegan los clientes y las consultas comerciales." },
  "motivos-perdida": { ruta: "loss-reasons", titulo: "Motivos de pérdida", singular: "motivo de pérdida", descripcion: "Razones que permiten comprender el cierre de una negociación." },
  "tipos-actividad": { ruta: "activity-types", titulo: "Tipos de actividad", singular: "tipo de actividad", descripcion: "Interacciones realizadas con empresas y contactos." },
  "tipos-evento": { ruta: "event-types", titulo: "Tipos de evento", singular: "tipo de evento", descripcion: "Eventos corporativos que se ofrecen en los salones." },
} as const;
export type Catalogo = keyof typeof CATALOGOS;

export function useCatalogo(catalogo: Catalogo, pagina = 0, filtros: { q?: string; active?: string } = {}) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "name,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({ queryKey: ["catalogos", catalogo, "lista", pagina, filtros], queryFn: ({ signal }) => solicitarApi<PaginaCatalogo>(`/${CATALOGOS[catalogo].ruta}?${parametros}`, { signal }) });
}
export function useEntradaCatalogo(catalogo: Catalogo, id?: number) {
  return useQuery({ queryKey: ["catalogos", catalogo, "detalle", id], enabled: Number.isSafeInteger(id) && id! > 0,
    queryFn: ({ signal }) => solicitarApi<EntradaCatalogo>(`/${CATALOGOS[catalogo].ruta}/${id}`, { signal }) });
}
export function useGuardarEntrada(catalogo: Catalogo, id?: number) {
  const cache = useQueryClient();
  return useMutation({ mutationFn: (datos: components["schemas"]["CatalogEntryRequest"]) => solicitarApi<EntradaCatalogo>(`/${CATALOGOS[catalogo].ruta}${id ? `/${id}` : ""}`, { method: id ? "PUT" : "POST", body: datos }),
    onSuccess: async () => { await Promise.all(["catalogos", "empresas", "contactos", "oportunidades", "actividades"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); } });
}
export const useEtapas = (embudoId?: number, habilitado = true) => useQuery({ queryKey: ["catalogos", "etapas", embudoId], enabled: habilitado && Number.isSafeInteger(embudoId) && embudoId! > 0, queryFn: ({ signal }) => solicitarApi<Etapa[]>(`/stages${embudoId ? `?pipelineId=${embudoId}` : ""}`, { signal }) });
export const useTiposActividad = (pagina = 0, q = "") => useCatalogo("tipos-actividad", pagina, { q, active: "true" });
export function useServiciosEvento(pagina = 0, q = "") {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "name,asc", active: "true", q });
  return useQuery({ queryKey: ["servicios-evento", pagina, q], queryFn: ({ signal }) => solicitarApi<components["schemas"]["PageResponseEventServiceResponse"]>(`/event-services?${parametros}`, { signal }) });
}
