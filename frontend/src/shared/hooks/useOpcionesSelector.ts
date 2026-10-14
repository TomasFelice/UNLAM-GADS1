import { useInfiniteQuery, type QueryKey } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { solicitarApi } from "../api/clienteApi";

/** Páginas de la API acumuladas sólo cuando se abre el selector o se pide ver más. */
export function useOpcionesSelector<T>({ clave, ruta, filtros = {}, busqueda, abierto }: {
  clave: QueryKey; ruta: string; filtros?: Record<string, string | undefined>; busqueda: string; abierto: boolean;
}) {
  const [termino, setTermino] = useState(busqueda);
  useEffect(() => {
    const temporizador = setTimeout(() => setTermino(busqueda), 200);
    return () => clearTimeout(temporizador);
  }, [busqueda]);
  const esperando = termino !== busqueda;
  const consulta = useInfiniteQuery({
    queryKey: [...clave, "selector", filtros, termino],
    enabled: abierto && !esperando,
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => {
      const parametros = new URLSearchParams({ page: String(pageParam), size: "20", sort: "name,asc" });
      Object.entries(filtros).forEach(([nombre, valor]) => { if (valor) parametros.set(nombre, valor); });
      if (termino) parametros.set("q", termino);
      return solicitarApi<{ content?: T[]; totalPages?: number }>(`${ruta}?${parametros}`, { signal });
    },
    getNextPageParam: (ultima, _paginas, pagina) => pagina + 1 < (ultima.totalPages ?? 1) ? pagina + 1 : undefined,
  });
  return {
    opciones: esperando ? [] : consulta.data?.pages.flatMap(p => p.content ?? []) ?? [],
    cargando: esperando || consulta.isLoading,
    cargandoMas: consulta.isFetchingNextPage,
    hayMas: !esperando && !!consulta.hasNextPage,
    error: !esperando && consulta.isError,
    verMas: async () => { if (!consulta.isFetching && !esperando) await consulta.fetchNextPage(); },
    reintentar: () => { if (consulta.isFetchNextPageError) void consulta.fetchNextPage(); else void consulta.refetch(); },
  };
}
