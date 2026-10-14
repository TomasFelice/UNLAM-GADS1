import { QueryClient } from "@tanstack/react-query";

export const cacheApi = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (intentos, error) => {
        if (error.name === "AbortError") return false;
        const estado = (error as { problema?: { status?: number } }).problema?.status;
        return estado !== undefined && estado < 500 ? false : intentos < 2;
      },
    },
    mutations: { retry: false },
  },
});
