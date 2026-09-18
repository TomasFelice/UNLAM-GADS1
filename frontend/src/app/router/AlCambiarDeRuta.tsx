import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * En una SPA el navegador conserva la posición de scroll al cambiar de ruta.
 * Se vuelve al inicio, salvo que la navegación traiga un ancla.
 */
export function AlCambiarDeRuta() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (hash) {
      document.querySelector(hash)?.scrollIntoView({ behavior: "smooth" });
      return;
    }
    window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior });
  }, [pathname, hash]);

  return null;
}
