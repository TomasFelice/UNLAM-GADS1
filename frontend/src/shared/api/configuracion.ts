export const URL_API = import.meta.env.VITE_API_URL;

if (!URL_API) {
  throw new Error("No se configuró VITE_API_URL.");
}
