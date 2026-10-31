import { useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";
import { obtenerIdentidadSesion, obtenerSesion } from "../../shared/sesion/almacenSesion";
import type { RevisionComercial } from "../../shared/api/revisionesComerciales";

export type ResumenComercialRespuesta = components["schemas"]["CommercialSummaryResponse"];
const DURACION = 5 * 60_000;
// El trabajo anterior se espera incluso al cambiar versión/revisión; no se cancela al desmontar.
const trabajos = new WeakMap<QueryClient, Map<string, Promise<unknown>>>();

export function useResumenComercial(id: number, version: number, autorizado: boolean) {
  const cache = useQueryClient();
  const sesion = obtenerIdentidadSesion();
  const usuario = obtenerSesion()?.usuario.id;
  const estadoRevision = useQuery({ queryKey: ["revision-comercial", id], queryFn: (): RevisionComercial => ({ numero: 0, validando: 0 }),
    initialData: { numero: 0, validando: 0 }, enabled: false, staleTime: Infinity }).data;
  const revision = estadoRevision.numero;
  const consulta = useQuery({
    queryKey: ["resumen-comercial", id, sesion, usuario, version, revision],
    enabled: autorizado && !!usuario && !estadoRevision.validando,
    staleTime: DURACION, gcTime: DURACION, retry: false, retryOnMount: false,
    refetchOnWindowFocus: false, refetchOnReconnect: false, refetchInterval: false,
    queryFn: async () => {
      let cola = trabajos.get(cache);
      if (!cola) { cola = new Map(); trabajos.set(cache, cola); }
      const clave = `${sesion}:${id}`;
      const anterior = cola.get(clave);
      const contextoVigente = () => {
        const detalle = cache.getQueryData<components["schemas"]["OpportunityDetailResponse"]>(["oportunidades", "detalle", id]);
        const actual = cache.getQueryData<RevisionComercial>(["revision-comercial", id]);
        return sesion === obtenerIdentidadSesion() && actual?.numero === revision && !actual.validando && (!detalle || detalle.version === version);
      };
      // Cada clave TanStack comparte su promesa. Las claves sucesivas se serializan aquí.
      const pendiente = (async () => {
        if (anterior) await anterior.catch(() => undefined);
        if (!contextoVigente()) {
          throw new DOMException("El contexto cambió.", "AbortError");
        }
        const resultado = await solicitarApi<ResumenComercialRespuesta>(`/opportunities/${id}/commercial-summary`, { method: "POST" });
        if (!contextoVigente()) {
          throw new DOMException("El contexto cambió.", "AbortError");
        }
        return resultado;
      })();
      cola.set(clave, pendiente);
      try { return await pendiente; }
      finally { if (cola.get(clave) === pendiente) cola.delete(clave); }
    },
  });
  return { ...consulta, contextoPendiente: !autorizado || estadoRevision.validando > 0 };
}
