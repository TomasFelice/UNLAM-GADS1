import { useState } from "react";
import { SelectorBuscable } from "../../shared/components/SelectorBuscable";
import { useOpcionesSelector } from "../../shared/hooks/useOpcionesSelector";
import { useSalon, type Salon } from "./apiSalones";

export function SelectorSalon({ valor = "", alCambiar, id, describedBy, soloDisponibles = false }: {
  valor?: string; alCambiar: (valor: string) => void; id?: string; describedBy?: string; soloDisponibles?: boolean;
}) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const consulta = useOpcionesSelector<Salon>({ clave: ["salones"], ruta: "/venues", filtros: { status: soloDisponibles ? "DISPONIBLE" : undefined }, busqueda, abierto });
  const actual = useSalon(valor ? Number(valor) : undefined);
  const texto = (o: Salon) => `${o.name} (${o.capacity} personas)${o.status !== "DISPONIBLE" ? " · Asociación anterior, no disponible" : ""}`;
  const seleccionado = actual.data ?? consulta.opciones.find(o => String(o.id) === valor);
  return <SelectorBuscable {...consulta} {...{ id, busqueda, alBuscar, abierto, alAbrir, alCambiar }} descripcion={describedBy}
    nombre="Salón" marcador={soloDisponibles ? "Elegí un salón" : "Todos los salones"}
    opciones={consulta.opciones.map(o => ({ valor: String(o.id), texto: texto(o) }))}
    seleccionados={valor ? [{ valor, texto: seleccionado ? texto(seleccionado) : "Consultando selección…" }] : []}
    error={consulta.error || actual.isError} reintentar={() => { consulta.reintentar(); if (valor) void actual.refetch(); }} />;
}
