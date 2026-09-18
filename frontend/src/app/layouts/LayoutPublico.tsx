import { useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { Logo } from "../../shared/components/Logo";
import { Icono } from "../../shared/components/Icono";
import { BotonEnlace } from "../../shared/components/Boton";
import { BotonTema } from "../../shared/components/BotonTema";
import { SITIO } from "../../shared/seo/sitio";
import { organizacion, sitioWeb } from "../../shared/seo/datosEstructurados";
import css from "./LayoutPublico.module.css";

const NAVEGACION = [
  { a: "/", texto: "Inicio" },
  { a: "/nosotros", texto: "Nosotros" },
  { a: "/contacto", texto: "Contacto" },
];

export function LayoutPublico() {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const { pathname } = useLocation();

  // La navegación cierra el menú. Se ajusta durante el render, que es el
  // patrón de React para reaccionar a un cambio de entrada sin un efecto.
  const [rutaPrevia, setRutaPrevia] = useState(pathname);
  if (rutaPrevia !== pathname) {
    setRutaPrevia(pathname);
    setMenuAbierto(false);
  }

  return (
    <div className={css.sitio}>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify([organizacion, sitioWeb]),
        }}
      />

      <a className="salto-contenido" href="#contenido">
        Ir al contenido
      </a>

      <header className={css.encabezado}>
        <div className={css.contenedor}>
          <Link to="/" className={css.marca} aria-label="Ztech CRM, inicio">
            <Logo tamano={30} />
          </Link>

          <nav className={css.navegacion} aria-label="Navegación principal">
            {NAVEGACION.map((item) => (
              <NavLink
                key={item.a}
                to={item.a}
                end={item.a === "/"}
                className={({ isActive }) =>
                  `${css.enlace} ${isActive ? css.activo : ""}`
                }
              >
                {item.texto}
              </NavLink>
            ))}
          </nav>

          <div className={css.accionesFijas}>
            <BotonTema />

            <button
              type="button"
              className={css.botonMenu}
              aria-expanded={menuAbierto}
              aria-controls="menu-movil"
              aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
              onClick={() => setMenuAbierto((v) => !v)}
            >
              <Icono nombre={menuAbierto ? "cerrar" : "menu"} tamano={20} />
            </button>
          </div>

          <div className={css.accionesEscritorio}>
            <BotonEnlace a="/ingresar" variante="secundario" tamano="sm">
              Iniciar sesión
            </BotonEnlace>
            <BotonEnlace a="/app" variante="primario" tamano="sm">
              Ver la demo
            </BotonEnlace>
          </div>
        </div>

        {menuAbierto && (
          <div className={css.menuMovil} id="menu-movil">
            <nav aria-label="Navegación principal, versión móvil">
              {NAVEGACION.map((item) => (
                <NavLink
                  key={item.a}
                  to={item.a}
                  end={item.a === "/"}
                  className={({ isActive }) =>
                    `${css.enlaceMovil} ${isActive ? css.activo : ""}`
                  }
                >
                  {item.texto}
                </NavLink>
              ))}
            </nav>
            <div className={css.accionesMovil}>
              <BotonEnlace a="/ingresar" variante="secundario" anchoCompleto>
                Iniciar sesión
              </BotonEnlace>
              <BotonEnlace a="/app" variante="primario" anchoCompleto>
                Ver la demo
              </BotonEnlace>
            </div>
          </div>
        )}
      </header>

      <main id="contenido" className={css.principal}>
        <Outlet />
      </main>

      <footer className={css.pie}>
        <div className={css.contenedor}>
          <div className={css.pieGrilla}>
            <div className={css.pieMarca}>
              <Logo tamano={30} />
              <p>{SITIO.lema}. Centralizá empresas, contactos y oportunidades en un solo lugar.</p>
              <p className={css.academico}>
                Proyecto académico de {SITIO.materia}, {SITIO.institucion}.
              </p>
            </div>

            <nav className={css.pieColumna} aria-label="Producto">
              <h2>Producto</h2>
              <Link to="/#capacidades">Capacidades</Link>
              <Link to="/#como-funciona">Cómo funciona</Link>
              <Link to="/app">Ver la demo</Link>
              <Link to="/ingresar">Iniciar sesión</Link>
            </nav>

            <nav className={css.pieColumna} aria-label="Institucional">
              <h2>Institucional</h2>
              <Link to="/nosotros">Nosotros</Link>
              <Link to="/contacto">Contacto</Link>
              <Link to="/contacto#preguntas">Preguntas frecuentes</Link>
            </nav>

            <nav className={css.pieColumna} aria-label="Legales">
              <h2>Legales</h2>
              <Link to="/terminos">Términos del Servicio</Link>
              <Link to="/privacidad">Política de Privacidad</Link>
              <a href={`mailto:${SITIO.email}`}>{SITIO.email}</a>
            </nav>
          </div>

          <div className={css.pieBase}>
            <p>© 2026 {SITIO.nombreLargo}. Todos los derechos reservados.</p>
            <p>{SITIO.localidad}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
