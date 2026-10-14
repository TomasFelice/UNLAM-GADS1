import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

export function useBusquedaUrl() {
  const [parametros, setParametros] = useSearchParams();
  const busquedaEnUrl = parametros.get("q") ?? "";
  const [busqueda, setBusqueda] = useState(busquedaEnUrl);
  const [busquedaAnteriorEnUrl, setBusquedaAnteriorEnUrl] = useState(busquedaEnUrl);
  if (busquedaAnteriorEnUrl !== busquedaEnUrl) {
    setBusquedaAnteriorEnUrl(busquedaEnUrl);
    setBusqueda(busquedaEnUrl);
  }
  useEffect(() => {
    if (busqueda.trim() === busquedaEnUrl) return;
    const timer = window.setTimeout(() => {
      setParametros((actuales) => {
        if ((actuales.get("q") ?? "") === busqueda.trim()) return actuales;
        const siguientes = new URLSearchParams(actuales);
        if (busqueda.trim()) siguientes.set("q", busqueda.trim()); else siguientes.delete("q");
        siguientes.delete("page");
        return siguientes;
      }, { replace: true });
    }, 350);
    return () => window.clearTimeout(timer);
  }, [busqueda, busquedaEnUrl, setParametros]);
  const pagina = Math.max(0, Number(parametros.get("page") ?? 0) || 0);
  const cambiar = (clave: string, valor: string) => setParametros((actuales) => {
    const siguientes = new URLSearchParams(actuales);
    if (valor) siguientes.set(clave, valor); else siguientes.delete(clave);
    siguientes.delete("page"); return siguientes;
  });
  const cambiarVarios = (valores: Record<string, string>) => setParametros(actuales => {
    const siguientes = new URLSearchParams(actuales);
    Object.entries(valores).forEach(([clave, valor]) => { if (valor) siguientes.set(clave, valor); else siguientes.delete(clave); });
    siguientes.delete("page"); return siguientes;
  });
  const cambiarPagina = (valor: number) => setParametros((actuales) => {
    const siguientes = new URLSearchParams(actuales); if (valor) siguientes.set("page", String(valor)); else siguientes.delete("page"); return siguientes;
  });
  return { parametros, pagina, busqueda, setBusqueda, cambiar, cambiarVarios, cambiarPagina };
}
