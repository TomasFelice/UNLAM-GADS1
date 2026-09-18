/**
 * Set de iconos propio.
 *
 * Trazo de 1.6 sobre una grilla de 24, `currentColor` y remates redondeados,
 * para que acompañen el peso de la tipografía. Se resuelve con SVG inline
 * en lugar de una librería: son pocos iconos y evita ~50 kB de dependencia.
 */

export type NombreIcono = keyof typeof TRAZOS;

const TRAZOS = {
  panel: (
    <>
      <rect x="3" y="3" width="7.5" height="7.5" rx="1.5" />
      <rect x="13.5" y="3" width="7.5" height="4.5" rx="1.5" />
      <rect x="13.5" y="10.5" width="7.5" height="10.5" rx="1.5" />
      <rect x="3" y="13.5" width="7.5" height="7.5" rx="1.5" />
    </>
  ),
  empresas: (
    <>
      <path d="M3.5 21h17" />
      <path d="M5.5 21V5.5A1.5 1.5 0 0 1 7 4h6a1.5 1.5 0 0 1 1.5 1.5V21" />
      <path d="M14.5 10H19a1.5 1.5 0 0 1 1.5 1.5V21" />
      <path d="M8.5 8h3M8.5 12h3M8.5 16h3M17 14h1M17 17.5h1" />
    </>
  ),
  contactos: (
    <>
      <circle cx="9" cy="8" r="3.25" />
      <path d="M2.75 20a6.25 6.25 0 0 1 12.5 0" />
      <path d="M16.5 5.5a3 3 0 0 1 0 5.6" />
      <path d="M17.5 14.2A5.4 5.4 0 0 1 21.25 20" />
    </>
  ),
  salones: (
    <>
      <rect x="2.75" y="3.75" width="18.5" height="11.5" rx="1.75" />
      <path d="M12 15.25V19" />
      <path d="M8.5 21l3.5-2 3.5 2" />
      <path d="M7 11.5l2.75-3 2.25 2.4L15 7.5l2 4" />
    </>
  ),
  oportunidades: (
    <>
      <rect x="2.75" y="7" width="18.5" height="13.25" rx="2" />
      <path d="M8.75 7V5.25A1.5 1.5 0 0 1 10.25 3.75h3.5a1.5 1.5 0 0 1 1.5 1.5V7" />
      <path d="M2.75 12.5h18.5" />
      <path d="M10.5 12.5v1.75h3V12.5" />
    </>
  ),
  embudo: (
    <>
      <rect x="3" y="3.75" width="5" height="16.5" rx="1.5" />
      <rect x="9.5" y="3.75" width="5" height="11" rx="1.5" />
      <rect x="16" y="3.75" width="5" height="14" rx="1.5" />
    </>
  ),
  buscar: (
    <>
      <circle cx="10.75" cy="10.75" r="6.75" />
      <path d="M15.6 15.6L21 21" />
    </>
  ),
  mas: <path d="M12 5v14M5 12h14" />,
  menos: <path d="M5 12h14" />,
  filtro: <path d="M3.5 5.5h17l-6.5 7.6V20l-4-2.2v-4.7z" />,
  ordenar: (
    <>
      <path d="M7 4.5v15M7 19.5l-3-3M7 19.5l3-3" />
      <path d="M17 19.5v-15M17 4.5l-3 3M17 4.5l3 3" />
    </>
  ),
  chevronAbajo: <path d="M6 9.5l6 6 6-6" />,
  chevronDerecha: <path d="M9.5 6l6 6-6 6" />,
  chevronIzquierda: <path d="M14.5 6l-6 6 6 6" />,
  flechaDerecha: (
    <>
      <path d="M4 12h15.5" />
      <path d="M13.5 6l6 6-6 6" />
    </>
  ),
  flechaIzquierda: (
    <>
      <path d="M20 12H4.5" />
      <path d="M10.5 6l-6 6 6 6" />
    </>
  ),
  cerrar: <path d="M6 6l12 12M18 6L6 18" />,
  menu: <path d="M3.5 6.5h17M3.5 12h17M3.5 17.5h17" />,
  check: <path d="M4.5 12.5l5 5 10-10" />,
  sol: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.75v2.25M12 19v2.25M21.25 12H19M5 12H2.75" />
      <path d="M18.55 5.45l-1.6 1.6M7.05 16.95l-1.6 1.6M18.55 18.55l-1.6-1.6M7.05 7.05l-1.6-1.6" />
    </>
  ),
  luna: <path d="M20.5 14.65A8.75 8.75 0 0 1 9.35 3.5a8.75 8.75 0 1 0 11.15 11.15z" />,
  salir: (
    <>
      <path d="M14.5 4.75h3.75A1.75 1.75 0 0 1 20 6.5v11a1.75 1.75 0 0 1-1.75 1.75H14.5" />
      <path d="M4 12h11" />
      <path d="M10.5 7.5L15 12l-4.5 4.5" />
    </>
  ),
  usuario: (
    <>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M4.75 20a7.25 7.25 0 0 1 14.5 0" />
    </>
  ),
  calendario: (
    <>
      <rect x="3.25" y="5" width="17.5" height="15.25" rx="2" />
      <path d="M3.25 9.75h17.5M8 3.25v3.5M16 3.25v3.5" />
    </>
  ),
  reloj: (
    <>
      <circle cx="12" cy="12" r="8.75" />
      <path d="M12 7.25V12l3.25 2" />
    </>
  ),
  correo: (
    <>
      <rect x="2.75" y="5" width="18.5" height="14" rx="2" />
      <path d="M3.5 6.75L12 13l8.5-6.25" />
    </>
  ),
  telefono: (
    <path d="M7.5 3.75l2.25 3.9-1.9 1.75a12 12 0 0 0 4.9 4.9l1.75-1.9 3.9 2.25-.9 3a1.6 1.6 0 0 1-1.75 1.1C9.9 17.9 6.1 14.1 4.4 6.4A1.6 1.6 0 0 1 5.5 4.65z" />
  ),
  mensaje: (
    <path d="M20.5 12.75a7.5 7.5 0 0 1-10.9 6.65L4 20.5l1.35-5.15A7.5 7.5 0 1 1 20.5 12.75z" />
  ),
  reunion: (
    <>
      <circle cx="8" cy="8.5" r="2.75" />
      <circle cx="16" cy="8.5" r="2.75" />
      <path d="M2.75 18.5a5.25 5.25 0 0 1 10.5 0M13.75 18.5a5.25 5.25 0 0 1 7.5-4.75" />
    </>
  ),
  virtual: (
    <>
      <rect x="2.75" y="6" width="12.5" height="12" rx="2" />
      <path d="M15.25 10.75l6-3.25v9l-6-3.25z" />
    </>
  ),
  demo: (
    <>
      <rect x="2.75" y="4.25" width="18.5" height="12.5" rx="2" />
      <path d="M9 20.25h6M12 16.75v3.5" />
      <path d="M10 8.25l3.75 2.25L10 12.75z" />
    </>
  ),
  propuesta: (
    <>
      <path d="M13.25 3.25H7A1.75 1.75 0 0 0 5.25 5v14A1.75 1.75 0 0 0 7 20.75h10A1.75 1.75 0 0 0 18.75 19V8.75z" />
      <path d="M13.25 3.25v5.5h5.5" />
      <path d="M8.75 13h6.5M8.75 16.5h4.5" />
    </>
  ),
  nota: (
    <>
      <path d="M5.25 4.75h13.5v10.5l-4 4H5.25z" />
      <path d="M18.75 15.25h-4v4" />
      <path d="M8.5 9h7M8.5 12.25h4.5" />
    </>
  ),
  ubicacion: (
    <>
      <path d="M12 21c4-4.4 6-7.7 6-10a6 6 0 1 0-12 0c0 2.3 2 5.6 6 10z" />
      <circle cx="12" cy="11" r="2.25" />
    </>
  ),
  sitio: (
    <>
      <circle cx="12" cy="12" r="8.75" />
      <path d="M3.5 12h17" />
      <path d="M12 3.25c2.2 2.4 3.3 5.3 3.3 8.75S14.2 18.35 12 20.75c-2.2-2.4-3.3-5.3-3.3-8.75S9.8 5.65 12 3.25z" />
    </>
  ),
  personas: (
    <>
      <circle cx="12" cy="7.25" r="3" />
      <path d="M6.25 19.75a5.75 5.75 0 0 1 11.5 0" />
      <path d="M4.75 12.5a2.25 2.25 0 1 0 0-4.5M19.25 12.5a2.25 2.25 0 1 1 0-4.5" />
    </>
  ),
  dinero: (
    <>
      <rect x="2.75" y="6" width="18.5" height="12" rx="2" />
      <circle cx="12" cy="12" r="2.75" />
      <path d="M6.25 12h.01M17.75 12h.01" />
    </>
  ),
  tendencia: (
    <>
      <path d="M3.5 16.5l5.5-5.5 3.5 3.5 7-7" />
      <path d="M15 7.5h4.5V12" />
    </>
  ),
  alerta: (
    <>
      <path d="M12 4.25l8.5 15H3.5z" />
      <path d="M12 9.75v4M12 16.75h.01" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="8.75" />
      <path d="M12 11v5.25M12 7.75h.01" />
    </>
  ),
  escudo: (
    <>
      <path d="M12 3.25l7 2.75v5.5c0 4-2.8 7.5-7 9.25-4.2-1.75-7-5.25-7-9.25V6z" />
      <path d="M9 12l2.25 2.25L15.25 10.25" />
    </>
  ),
  capas: (
    <>
      <path d="M12 3.5l8.5 4.25L12 12 3.5 7.75z" />
      <path d="M3.5 12L12 16.25 20.5 12M3.5 16.25L12 20.5l8.5-4.25" />
    </>
  ),
  lapiz: (
    <>
      <path d="M4.5 19.5l.9-3.6L16.1 5.2a1.9 1.9 0 0 1 2.7 2.7L8.1 18.6z" />
      <path d="M14.25 7.25l2.5 2.5" />
    </>
  ),
  enlaceExterno: (
    <>
      <path d="M13.5 4.5h6v6" />
      <path d="M19.5 4.5L11 13" />
      <path d="M18 14v4.75A1.75 1.75 0 0 1 16.25 20.5H5.25A1.75 1.75 0 0 1 3.5 18.75V7.75A1.75 1.75 0 0 1 5.25 6H10" />
    </>
  ),
  campana: (
    <>
      <path d="M6.25 10.5a5.75 5.75 0 0 1 11.5 0c0 3.4.9 5.1 1.75 6H4.5c.85-.9 1.75-2.6 1.75-6z" />
      <path d="M10 19.5a2.25 2.25 0 0 0 4 0" />
    </>
  ),
  llamada: (
    <path d="M7.5 3.75l2.25 3.9-1.9 1.75a12 12 0 0 0 4.9 4.9l1.75-1.9 3.9 2.25-.9 3a1.6 1.6 0 0 1-1.75 1.1C9.9 17.9 6.1 14.1 4.4 6.4A1.6 1.6 0 0 1 5.5 4.65z" />
  ),
  email: (
    <>
      <rect x="2.75" y="5" width="18.5" height="14" rx="2" />
      <path d="M3.5 6.75L12 13l8.5-6.25" />
    </>
  ),
} as const;

type Props = {
  nombre: NombreIcono;
  /** Tamaño en píxeles. */
  tamano?: number;
  /** Texto accesible. Sin él, el icono queda oculto para lectores de pantalla. */
  titulo?: string;
  className?: string;
};

export function Icono({ nombre, tamano = 18, titulo, className }: Props) {
  return (
    <svg
      className={className}
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={titulo ? "img" : undefined}
      aria-hidden={titulo ? undefined : true}
      aria-label={titulo}
      focusable="false"
    >
      {TRAZOS[nombre]}
    </svg>
  );
}
