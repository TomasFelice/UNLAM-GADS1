import { useState } from "react";
import { SelectorBuscable } from "../../shared/components/SelectorBuscable";
import { useOpcionesSelector } from "../../shared/hooks/useOpcionesSelector";
import { useContacto, type Contacto } from "../contactos/apiContactos";
import { useEmpresa, type EmpresaResumen } from "../empresas/apiEmpresas";

type Props = {
  tipo: "empresa" | "contacto"; valor?: string; alCambiar: (valor: string) => void; empresaId?: string; id?: string; descripcion?: string; filtro?: boolean;
};
export function SelectorCliente({ tipo, valor = "", alCambiar, empresaId, id, descripcion, filtro = false }: Props) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const [contexto, setContexto] = useState({ tipo, empresaId });
  if (contexto.tipo !== tipo || contexto.empresaId !== empresaId) {
    setContexto({ tipo, empresaId }); alBuscar(""); alAbrir(false);
  }
  const consulta = useOpcionesSelector<EmpresaResumen | Contacto>({ clave: [tipo === "empresa" ? "empresas" : "contactos"],
    ruta: tipo === "empresa" ? "/companies" : "/contacts", busqueda, abierto,
    filtros: { companyId: tipo === "contacto" ? empresaId : undefined, sort: tipo === "empresa" ? "businessName,asc" : "lastName,asc" } });
  const empresa = useEmpresa(tipo === "empresa" && valor ? Number(valor) : undefined);
  const contacto = useContacto(tipo === "contacto" && valor ? Number(valor) : undefined);
  const actual = tipo === "empresa" ? empresa : contacto;
  const opciones = consulta.opciones.map(o => ({ valor: String(o.id), texto: "firstName" in o ? `${o.firstName} ${o.lastName}` : (o as EmpresaResumen).businessName || (o as EmpresaResumen).legalName || "Empresa" }));
  const nombreActual = tipo === "empresa" ? empresa.data?.businessName || empresa.data?.legalName : contacto.data ? `${contacto.data.firstName} ${contacto.data.lastName}` : undefined;
  return <SelectorBuscable {...consulta} {...{ id, descripcion, busqueda, alBuscar, abierto, alAbrir, alCambiar, opciones }}
    nombre={tipo === "empresa" ? "Empresa" : "Contacto"} marcador={filtro ? tipo === "empresa" ? "Todas las empresas" : "Todos los contactos" : tipo === "empresa" ? "Sin empresa" : "Sin contacto"}
    seleccionados={valor ? [{ valor, texto: nombreActual ?? opciones.find(o => o.valor === valor)?.texto ?? "Consultando selección…" }] : []}
    error={consulta.error || actual.isError} reintentar={() => { consulta.reintentar(); if (valor) void actual.refetch(); }} />;
}
