import { useQuery } from "@tanstack/react-query";
import type { components } from "../../shared/api/esquema-api";
import { solicitarApi } from "../../shared/api/clienteApi";

export type Etapa = components["schemas"]["StageResponse"];
export type Origen = components["schemas"]["OriginResponse"];
export type TipoEvento = components["schemas"]["EventTypeResponse"];
export type ServicioEvento = components["schemas"]["EventServiceResponse"];
export type TipoActividad = components["schemas"]["ActivityTypeResponse"];

const useConsulta = <T,>(clave: string, ruta: string) =>
  useQuery({ queryKey: ["catalogos", clave], queryFn: ({ signal }) => solicitarApi<T>(ruta, { signal }) });

export const useEtapas = () => useConsulta<Etapa[]>("etapas", "/stages");
export const useOrigenes = () => useConsulta<Origen[]>("origenes", "/origins");
export const useTiposEvento = () => useConsulta<TipoEvento[]>("tipos-evento", "/event-types");
export const useServiciosEvento = () => useConsulta<ServicioEvento[]>("servicios-evento", "/event-services");
export const useTiposActividad = () => useConsulta<TipoActividad[]>("tipos-actividad", "/activity-types");
