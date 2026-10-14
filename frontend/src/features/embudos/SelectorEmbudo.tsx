import { useState } from "react";
import { SelectorBuscable } from "../../shared/components/SelectorBuscable";
import { useOpcionesSelector } from "../../shared/hooks/useOpcionesSelector";
import { useConfiguracionEmbudo, type ConfiguracionEmbudo } from "./apiEmbudos";

export function SelectorEmbudo({ valor = "", alCambiar, id, descripcion, soloActivos = false, requerido = false }: {
  valor?: string; alCambiar: (valor: string) => void; id?: string; descripcion?: string; soloActivos?: boolean; requerido?: boolean;
}) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const consulta = useOpcionesSelector<ConfiguracionEmbudo>({ clave: ["embudos"], ruta: "/pipelines", filtros: { active: soloActivos ? "true" : undefined }, busqueda, abierto });
  const actual = useConfiguracionEmbudo(valor ? Number(valor) : undefined);
  const texto = (o: ConfiguracionEmbudo) => `${o.name}${o.defaultPipeline ? " (predeterminado)" : ""}${!o.active ? " (inactivo)" : ""}`;
  const seleccionado = actual.data ?? consulta.opciones.find(o => String(o.id) === valor);
  return <SelectorBuscable {...consulta} {...{ id, descripcion, busqueda, alBuscar, abierto, alAbrir, alCambiar }}
    nombre="Embudo" marcador={requerido ? "Elegí un embudo" : soloActivos ? "Sin especificar" : "Todos los embudos"}
    opciones={consulta.opciones.map(o => ({ valor: String(o.id), texto: texto(o) }))}
    seleccionados={valor ? [{ valor, texto: seleccionado ? texto(seleccionado) : "Consultando selección…" }] : []}
    error={consulta.error || actual.isError} reintentar={() => { consulta.reintentar(); if (valor) void actual.refetch(); }} />;
}
