export type Rol = "ADMIN" | "SALES_MANAGER" | "SELLER";

export type UsuarioDeSesion = {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  rol: Rol;
};

export type Sesion = {
  token: string;
  venceEn: number;
  debeCambiarClave: boolean;
  usuario: UsuarioDeSesion;
};

const CLAVE = "ztech.crm.sesion";

function esSesion(valor: unknown): valor is Sesion {
  if (!valor || typeof valor !== "object") return false;
  const candidato = valor as Partial<Sesion>;
  return (
    typeof candidato.token === "string" &&
    typeof candidato.venceEn === "number" &&
    typeof candidato.debeCambiarClave === "boolean" &&
    typeof candidato.usuario?.id === "number" &&
    typeof candidato.usuario.email === "string"
  );
}

function leer(almacen: Storage): Sesion | null {
  const serializada = almacen.getItem(CLAVE);
  if (!serializada) return null;
  try {
    const sesion: unknown = JSON.parse(serializada);
    if (esSesion(sesion) && sesion.venceEn > Date.now()) return sesion;
  } catch {
    // Un valor corrupto o de una versión anterior se descarta.
  }
  almacen.removeItem(CLAVE);
  return null;
}

export function obtenerSesion(): Sesion | null {
  return leer(sessionStorage) ?? leer(localStorage);
}

export function guardarSesion(sesion: Sesion, mantener: boolean) {
  borrarSesion();
  (mantener ? localStorage : sessionStorage).setItem(CLAVE, JSON.stringify(sesion));
}

export function borrarSesion() {
  sessionStorage.removeItem(CLAVE);
  localStorage.removeItem(CLAVE);
}

export const EVENTO_SESION_EXPIRADA = "ztech:sesion-expirada";
