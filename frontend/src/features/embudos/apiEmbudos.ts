import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
export type ConfiguracionEmbudo = components["schemas"]["PipelineResponse"];
export type EtapaEmbudo = components["schemas"]["StageResponse"];
const esId = (id?: number) => Number.isSafeInteger(id) && id! > 0;
export function useEmbudos(pagina = 0, filtros: { q?: string; active?: string } = {}) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "20", sort: "name,asc" });
  Object.entries(filtros).forEach(([clave, valor]) => { if (valor) parametros.set(clave, valor); });
  return useQuery({ queryKey: ["embudos", "lista", pagina, filtros], queryFn: ({ signal }) => solicitarApi<components["schemas"]["PageResponsePipelineResponse"]>(`/pipelines?${parametros}`, { signal }) });
}
export const useConfiguracionEmbudo = (id?: number) => useQuery({ queryKey: ["embudos", "detalle", id], enabled: esId(id), queryFn: ({ signal }) => solicitarApi<ConfiguracionEmbudo>(`/pipelines/${id}`, { signal }) });
export const useEmbudoPredeterminado = (habilitado = true) => useQuery({ enabled: habilitado, queryKey: ["embudos", "predeterminado"], queryFn: ({ signal }) => solicitarApi<ConfiguracionEmbudo>("/pipelines/default", { signal }) });
export const usePlantillasEmbudo = () => useQuery({ queryKey: ["embudos", "plantillas"], queryFn: ({ signal }) => solicitarApi<components["schemas"]["PipelineTemplateResponse"][]>("/pipeline-templates", { signal }) });
export function useOperacionesEmbudo() {
  const cache = useQueryClient();
  const invalidar = async () => { await Promise.all(["embudos", "catalogos", "oportunidades"].map(clave => cache.invalidateQueries({ queryKey: [clave] }))); };
  return {
    crear: useMutation({ mutationFn: (body: components["schemas"]["CreatePipelineRequest"]) => solicitarApi<ConfiguracionEmbudo>("/pipelines", { method: "POST", body }), onSuccess: invalidar }),
    guardar: useMutation({ mutationFn: ({ id, ...body }: components["schemas"]["CatalogEntryRequest"] & { id: number }) => solicitarApi<ConfiguracionEmbudo>(`/pipelines/${id}`, { method: "PUT", body }), onSuccess: invalidar }),
    predeterminar: useMutation({ mutationFn: (id: number) => solicitarApi<ConfiguracionEmbudo>(`/pipelines/${id}/default`, { method: "POST" }), onSuccess: invalidar }),
    guardarEtapa: useMutation({ mutationFn: ({ id, ...body }: components["schemas"]["StageRequest"] & { id?: number }) => solicitarApi<EtapaEmbudo>(`/stages${id ? `/${id}` : ""}`, { method: id ? "PUT" : "POST", body }), onSuccess: invalidar }),
    ordenar: useMutation({ mutationFn: ({ id, stageIds }: { id: number; stageIds: number[] }) => solicitarApi<EtapaEmbudo[]>(`/pipelines/${id}/stages/reorder`, { method: "POST", body: { stageIds } }), onSuccess: invalidar }),
  };
}
