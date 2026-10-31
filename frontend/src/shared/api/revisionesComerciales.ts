import type { QueryClient } from "@tanstack/react-query";

// Se publican señales una vez por escritura confirmada, no por cada invalidación.
export type RevisionComercial = { numero: number; validando: number };
export async function actualizarContextoComercial(cache: QueryClient, id: number | undefined, actualizar: () => Promise<unknown>) {
  if (!id) { await actualizar(); return; }
  const clave = ["revision-comercial", id];
  cache.setQueryData<RevisionComercial>(clave, anterior => ({ numero: anterior?.numero ?? 0, validando: (anterior?.validando ?? 0) + 1 }));
  try { await actualizar(); }
  finally {
    // Si se cerró sesión, no recrear entradas retiradas por clear().
    if (cache.getQueryState(clave)) cache.setQueryData<RevisionComercial>(clave, anterior => ({ numero: (anterior?.numero ?? 0) + 1, validando: Math.max(0, (anterior?.validando ?? 1) - 1) }));
  }
}
