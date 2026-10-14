import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type Oportunidad = components["schemas"]["OpportunityResponse"];
export type OportunidadDetalle = components["schemas"]["OpportunityDetailResponse"];
export type SolicitudCrearOportunidad = components["schemas"]["CreateOpportunityRequest"];
export type SolicitudEditarOportunidad = components["schemas"]["UpdateOpportunityRequest"];
export type PaginaOportunidades = components["schemas"]["PageResponseOpportunityResponse"];
export type Embudo = components["schemas"]["OpportunityBoardResponse"];
export type HistorialEtapa = components["schemas"]["StageHistoryResponse"];
export type FiltrosOportunidades = { pipelineId?: string; q?: string; status?: string; stageId?: string; originId?: string; salesRepId?: string; venueId?: string; companyId?: string; contactId?: string; eventFrom?: string; eventTo?: string };
export const clavesOportunidades = { todas: ["oportunidades"] as const, lista: (p: number, f: FiltrosOportunidades) => ["oportunidades", "lista", p, f] as const, detalle: (id: number) => ["oportunidades", "detalle", id] as const, embudo: (f: FiltrosOportunidades) => ["oportunidades", "embudo", f] as const, historial: (id: number) => ["oportunidades", "historial", id] as const };
const query = (f: FiltrosOportunidades, pagina?: number) => { const p = new URLSearchParams(); if (pagina !== undefined) { p.set("page", String(pagina)); p.set("size", "20"); p.set("sort", "eventStart,desc"); } Object.entries(f).forEach(([k,v]) => { if (!v) return; if (k === "eventFrom") p.set(k, `${v}T00:00:00-03:00`); else if (k === "eventTo") p.set(k, `${v}T23:59:59-03:00`); else p.set(k,v); }); return p; };
export const useOportunidades = (pagina=0, filtros: FiltrosOportunidades={}) => useQuery({ queryKey: clavesOportunidades.lista(pagina,filtros), queryFn: ({signal}) => solicitarApi<PaginaOportunidades>(`/opportunities?${query(filtros,pagina)}`,{signal}) });
export const useOportunidadesRecientes = () => useQuery({ queryKey: ["oportunidades", "recientes", 5], queryFn: ({ signal }) => solicitarApi<PaginaOportunidades>("/opportunities?page=0&size=5&sort=createdAt,desc", { signal }) });
export const useOportunidad = (id?: number) => useQuery({ queryKey: clavesOportunidades.detalle(id??0), queryFn: ({signal}) => solicitarApi<OportunidadDetalle>(`/opportunities/${id}`,{signal}), enabled:Number.isSafeInteger(id) && id! > 0 });
export const useEmbudo = (filtros: FiltrosOportunidades={}, habilitado = true) => useQuery({ enabled: habilitado, queryKey: clavesOportunidades.embudo(filtros), queryFn: ({signal}) => solicitarApi<Embudo>(`/opportunities/board?${query(filtros)}`,{signal}) });
export const useHistorialEtapas = (id?: number) => useQuery({ queryKey: clavesOportunidades.historial(id??0), queryFn: ({signal}) => solicitarApi<HistorialEtapa[]>(`/opportunities/${id}/stage-history`,{signal}), enabled:Number.isSafeInteger(id) && id! > 0 });
export const useGuardarOportunidad = (id?: number) => { const cache=useQueryClient(); return useMutation({ mutationFn:(body:SolicitudCrearOportunidad|SolicitudEditarOportunidad)=>solicitarApi<Oportunidad>(id?`/opportunities/${id}`:"/opportunities",{method:id?"PUT":"POST",body}), onSuccess:async(v)=>{await Promise.all(["oportunidades", "empresas", "contactos"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); if(v.id) await cache.invalidateQueries({queryKey:clavesOportunidades.detalle(v.id)});} }); };
export const useCambiarEtapa = () => { const cache=useQueryClient(); return useMutation({ mutationFn:({id,stageId,note,version}:{id:number;stageId:number;note?:string;version:number})=>solicitarApi<Oportunidad>(`/opportunities/${id}/stage`,{method:"POST",body:{stageId,note,version}}), onSuccess:async(_,v)=>{ await Promise.all([cache.invalidateQueries({queryKey:clavesOportunidades.todas}),cache.invalidateQueries({queryKey:clavesOportunidades.detalle(v.id)}),cache.invalidateQueries({queryKey:clavesOportunidades.historial(v.id)}),cache.invalidateQueries({queryKey:["actividades"]})]); } }); };

export type AccionOportunidad = "assign" | "win" | "lose" | "reopen";
type DatosAccion = components["schemas"]["AssignOpportunityRequest"] | components["schemas"]["WinOpportunityRequest"] | components["schemas"]["LoseOpportunityRequest"] | components["schemas"]["ReopenOpportunityRequest"];
export function useAccionOportunidad(id: number) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: ({ accion, datos }: { accion: AccionOportunidad; datos: DatosAccion }) => solicitarApi<Oportunidad>(`/opportunities/${id}/${accion}`, { method: "POST", body: datos }),
    onSuccess: async () => { await Promise.all([cache.invalidateQueries({ queryKey: clavesOportunidades.todas }), cache.invalidateQueries({ queryKey: ["actividades"] }), cache.invalidateQueries({ queryKey: ["empresas"] }), cache.invalidateQueries({ queryKey: ["contactos"] })]); },
  });
}
export function useDisponibilidad(id: number, habilitado: boolean) {
  return useQuery({ queryKey: ["oportunidades", "disponibilidad", id], enabled: habilitado,
    queryFn: ({ signal }) => solicitarApi<components["schemas"]["VenueAvailabilityResponse"]>(`/opportunities/${id}/availability`, { signal }), staleTime: 0 });
}
