/**
 * Tema claro / oscuro.
 *
 * El tema vive en el atributo `data-tema` de `<html>`: los tokens de
 * `shared/styles/tokens.css` se redefinen a partir de ahí y ningún componente
 * necesita saber cuál está activo.
 *
 * El primer valor lo escribe el script de arranque de `index.html`, antes del
 * primer pintado, para que no haya un fogonazo claro al recargar en oscuro.
 * Este módulo sólo lo lee y lo cambia.
 */

import { useCallback, useSyncExternalStore } from "react";

export type Tema = "claro" | "oscuro";

/** Debe coincidir con la clave del script de arranque de `index.html`. */
const CLAVE = "ztech-tema";

/** Color de la barra del navegador en móvil, por tema. */
const COLOR_BARRA: Record<Tema, string> = {
  claro: "#301854",
  oscuro: "#121019",
};

const suscriptores = new Set<() => void>();

function esTema(valor: unknown): valor is Tema {
  return valor === "claro" || valor === "oscuro";
}

export function temaActual(): Tema {
  const marcado = document.documentElement.dataset.tema;
  return esTema(marcado) ? marcado : "claro";
}

export function aplicarTema(tema: Tema) {
  document.documentElement.dataset.tema = tema;

  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute("content", COLOR_BARRA[tema]);

  // Si falla (modo privado, almacenamiento bloqueado) el tema igual se aplica:
  // sólo se pierde la preferencia al recargar.
  try {
    localStorage.setItem(CLAVE, tema);
  } catch {
    /* sin persistencia */
  }

  for (const avisar of suscriptores) avisar();
}

function suscribir(avisar: () => void) {
  suscriptores.add(avisar);
  return () => {
    suscriptores.delete(avisar);
  };
}

/** Devuelve el tema activo y la función que lo alterna. */
export function useTema(): [Tema, () => void] {
  const tema = useSyncExternalStore(suscribir, temaActual);
  const alternar = useCallback(() => {
    aplicarTema(temaActual() === "oscuro" ? "claro" : "oscuro");
  }, []);

  return [tema, alternar];
}
