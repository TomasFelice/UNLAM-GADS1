import { useState } from "react";
import { SelectorBuscable } from "../../shared/components/SelectorBuscable";
import { useOpcionesSelector } from "../../shared/hooks/useOpcionesSelector";
import { CATALOGOS, useEntradaCatalogo, type Catalogo, type EntradaCatalogo } from "./apiCatalogos";

export function SelectorCatalogo({ catalogo, valor = "", alCambiar, id, descripcion, soloActivos = false, requerido = false }: {
  catalogo: Catalogo; valor?: string; alCambiar: (valor: string) => void; id?: string; descripcion?: string; soloActivos?: boolean; requerido?: boolean;
}) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const nombre = CATALOGOS[catalogo].singular;
  const consulta = useOpcionesSelector<EntradaCatalogo>({ clave: ["catalogos", catalogo], ruta: `/${CATALOGOS[catalogo].ruta}`, filtros: { active: soloActivos ? "true" : undefined }, busqueda, abierto });
  const actual = useEntradaCatalogo(catalogo, valor ? Number(valor) : undefined);
  const texto = (o: EntradaCatalogo) => `${o.name}${!o.active ? " (inactivo)" : ""}`;
  const seleccionado = actual.data ?? consulta.opciones.find(o => String(o.id) === valor);
  return <SelectorBuscable {...consulta} {...{ id, descripcion, busqueda, alBuscar, abierto, alAbrir, alCambiar }}
    nombre={nombre.charAt(0).toUpperCase() + nombre.slice(1)} marcador={requerido ? `Elegí un ${nombre}` : soloActivos ? "Sin especificar" : `Todos: ${nombre}`}
    opciones={consulta.opciones.map(o => ({ valor: String(o.id), texto: texto(o) }))}
    seleccionados={valor ? [{ valor, texto: seleccionado ? texto(seleccionado) : "Consultando selección…" }] : []}
    error={consulta.error || actual.isError} reintentar={() => { consulta.reintentar(); if (valor) void actual.refetch(); }} />;
}
