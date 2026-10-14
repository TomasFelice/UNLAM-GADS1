import { useConfiguracionEmbudo } from "./apiEmbudos";
export function NombreEmbudo({ id }: { id?: number }) {
  const consulta = useConfiguracionEmbudo(id);
  return <span>{consulta.data ? `${consulta.data.name}${consulta.data.defaultPipeline ? " · Predeterminado" : ""}${!consulta.data.active ? " · Inactivo" : ""}` : consulta.isError ? "Embudo no disponible" : "Consultando embudo…"}</span>;
}
