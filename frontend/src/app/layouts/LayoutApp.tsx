import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Logo } from "../../shared/components/Logo";
import { Icono, type NombreIcono } from "../../shared/components/Icono";
import { Avatar } from "../../shared/components/Avatar";
import { BotonTema } from "../../shared/components/BotonTema";
import { useSesion } from "../../shared/sesion/contextoSesion";
import css from "./LayoutApp.module.css";

type ItemNav = { a: string; texto: string; icono: NombreIcono; exacto?: boolean };

const PRINCIPAL: ItemNav[] = [
  { a: "/app", texto: "Panel", icono: "panel", exacto: true },
  { a: "/app/oportunidades", texto: "Oportunidades", icono: "oportunidades" },
  { a: "/app/embudo", texto: "Embudo", icono: "embudo" },
];

const CLIENTES: ItemNav[] = [
  { a: "/app/empresas", texto: "Empresas", icono: "empresas" },
  { a: "/app/contactos", texto: "Contactos", icono: "contactos" },
];

const CATALOGO: ItemNav[] = [
  { a: "/app/salones", texto: "Salones", icono: "salones" },
];

export function LayoutApp() {
  const [lateralAbierto, setLateralAbierto] = useState(false);
  const { pathname } = useLocation();
  const navegar = useNavigate();
  const { sesion, cerrar } = useSesion();
  const usuario = sesion!.usuario;
  const iniciales = `${usuario.nombre.charAt(0)}${usuario.apellido.charAt(0)}`;
  const roles = {
    ADMIN: "Administrador",
    SALES_MANAGER: "Responsable comercial",
    SELLER: "Vendedor",
  };

  // La navegación cierra el menú. Se ajusta durante el render, que es el
  // patrón de React para reaccionar a un cambio de entrada sin un efecto.
  const [rutaPrevia, setRutaPrevia] = useState(pathname);
  if (rutaPrevia !== pathname) {
    setRutaPrevia(pathname);
    setLateralAbierto(false);
  }

  const grupo = (titulo: string, items: ItemNav[]) => (
    <div className={css.grupo}>
      <p className={css.grupoTitulo}>{titulo}</p>
      {items.map((item) => (
        <NavLink
          key={item.a}
          to={item.a}
          end={item.exacto}
          className={({ isActive }) =>
            `${css.enlace} ${isActive ? css.activo : ""}`
          }
        >
          <Icono nombre={item.icono} tamano={17} />
          {item.texto}
        </NavLink>
      ))}
    </div>
  );

  return (
    <div className={css.app}>
      <a className="salto-contenido" href="#contenido">
        Ir al contenido
      </a>

      {lateralAbierto && (
        <button
          type="button"
          className={css.velo}
          aria-label="Cerrar la navegación"
          onClick={() => setLateralAbierto(false)}
        />
      )}

      <aside
        className={`${css.lateral} ${lateralAbierto ? css.lateralAbierto : ""}`}
      >
        <div className={css.lateralMarca}>
          <Link to="/app" aria-label="Ztech CRM, panel">
            <Logo variante="compacto" tamano={28} />
          </Link>
        </div>

        <nav className={`${css.navegacion} scroll-fino`} aria-label="Navegación de la aplicación">
          {grupo("Comercial", PRINCIPAL)}
          {grupo("Clientes", CLIENTES)}
          {grupo("Catálogo", CATALOGO)}
        </nav>

        <div className={css.lateralPie}>
          <Link to="/" className={css.volver}>
            <Icono nombre="flechaIzquierda" tamano={14} />
            Volver al sitio
          </Link>
        </div>
      </aside>

      <div className={css.marco}>
        <header className={css.superior}>
          <button
            type="button"
            className={css.botonLateral}
            aria-label="Abrir la navegación"
            aria-expanded={lateralAbierto}
            onClick={() => setLateralAbierto(true)}
          >
            <Icono nombre="menu" tamano={19} />
          </button>

          <div className={css.superiorAcciones}>
            <BotonTema className={css.iconoAccion} />

            <div className={css.usuario}>
              <Avatar iniciales={iniciales} tamano="md" />
              <span className={css.usuarioDatos}>
                <span className={css.usuarioNombre}>
                  {usuario.nombre} {usuario.apellido}
                </span>
                <span className={css.usuarioRol}>{roles[usuario.rol]}</span>
              </span>
            </div>

            <button
              type="button"
              className={css.iconoAccion}
              aria-label="Cerrar sesión"
              onClick={() => {
                cerrar();
                navegar("/ingresar", { replace: true });
              }}
            >
              <Icono nombre="salir" tamano={18} />
            </button>
          </div>
        </header>

        <main id="contenido" className={css.contenido}>
          <div className={css.envoltorio}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
