import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton } from "../../shared/components/Boton";
import { BotonTema } from "../../shared/components/BotonTema";
import { Campo, Entrada } from "../../shared/components/Campos";
import { Icono } from "../../shared/components/Icono";
import { Logo } from "../../shared/components/Logo";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import css from "./PaginaIngreso.module.css";

const GARANTIAS = [
  "Empresas, contactos y oportunidades en una sola base",
  "Embudo comercial con el historial de cada etapa",
  "Disponibilidad del salón validada antes de confirmar",
];

const esquema = z.object({
  email: z.string().trim().min(1, "Ingresá tu correo.").email("Ingresá un correo válido."),
  password: z.string().min(1, "Ingresá tu contraseña."),
  mantener: z.boolean(),
});

type DatosIngreso = z.infer<typeof esquema>;

export function PaginaIngreso() {
  const { ingresar } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [parametros] = useSearchParams();
  const [errorGeneral, setErrorGeneral] = useState<string>();
  const mensaje = (ubicacion.state as { mensaje?: string } | null)?.mensaje;
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosIngreso>({
    resolver: zodResolver(esquema),
    mode: "onBlur",
    defaultValues: { email: "", password: "", mantener: false },
  });

  const alEnviar = handleSubmit(async ({ email, password, mantener }) => {
    setErrorGeneral(undefined);
    try {
      const sesion = await ingresar({ email: email.trim(), password }, mantener);
      const solicitado = parametros.get("desde");
      const destino = solicitado?.startsWith("/app") ? solicitado : "/app";
      navegar(sesion.debeCambiarClave ? "/app/cambiar-clave" : destino, { replace: true });
    } catch (error) {
      setErrorGeneral(
        error instanceof ErrorDeApi
          ? error.message
          : "No pudimos iniciar sesión. Intentá nuevamente.",
      );
    }
  });

  return (
    <>
      <Seo
        titulo="Iniciar sesión"
        descripcion="Accedé a Ztech CRM para gestionar las oportunidades comerciales de tu salón de eventos corporativos."
        noIndexar
      />

      <div className={css.pantalla}>
        <div className={css.columnaForm}>
          <header className={css.cabecera}>
            <Link to="/" aria-label="Ztech CRM, inicio">
              <Logo tamano={32} />
            </Link>
            <BotonTema />
          </header>

          <main className={css.centro}>
            <div className={css.caja}>
              <h1 className={css.titulo}>Ingresá a tu cuenta</h1>
              <p className={css.bajada}>Usá el correo de tu equipo comercial para acceder al CRM.</p>

              {mensaje && <p className={css.exito} role="status">{mensaje}</p>}
              {errorGeneral && <p className={css.errorGeneral} role="alert">{errorGeneral}</p>}

              <form onSubmit={alEnviar} className={css.formulario} noValidate>
                <Campo etiqueta="Correo electrónico" requerido error={errors.email?.message}>
                  {(id, describedBy) => (
                    <Entrada
                      id={id}
                      type="email"
                      autoComplete="username"
                      placeholder="nombre@empresa.com.ar"
                      aria-describedby={describedBy}
                      aria-invalid={Boolean(errors.email)}
                      {...register("email")}
                    />
                  )}
                </Campo>

                <Campo etiqueta="Contraseña" requerido error={errors.password?.message}>
                  {(id, describedBy) => (
                    <Entrada
                      id={id}
                      type="password"
                      autoComplete="current-password"
                      aria-describedby={describedBy}
                      aria-invalid={Boolean(errors.password)}
                      {...register("password")}
                    />
                  )}
                </Campo>

                <div className={css.fila}>
                  <label className={css.recordar}>
                    <input type="checkbox" {...register("mantener")} />
                    <span>Mantener la sesión iniciada</span>
                  </label>
                </div>

                <Boton type="submit" tamano="lg" anchoCompleto disabled={isSubmitting}>
                  {isSubmitting ? "Ingresando…" : "Iniciar sesión"}
                </Boton>
              </form>

              <p className={css.aviso}>
                <Icono nombre="info" tamano={15} />
                Proyecto académico con acceso exclusivo para usuarios habilitados.
              </p>
            </div>
          </main>

          <footer className={css.pie}>
            <Link to="/">Volver al sitio</Link><span aria-hidden="true">·</span>
            <Link to="/terminos">Términos</Link><span aria-hidden="true">·</span>
            <Link to="/privacidad">Privacidad</Link>
          </footer>
        </div>

        <aside className={css.columnaMarca} aria-hidden="true">
          <div className={css.marcaContenido}>
            <Logo variante="completo" tamano={44} tono="claro" />
            <p className={css.marcaLema}>El control comercial de tu salón, sin planillas sueltas.</p>
            <ul className={css.garantias}>
              {GARANTIAS.map((garantia) => (
                <li key={garantia}><Icono nombre="check" tamano={16} />{garantia}</li>
              ))}
            </ul>
          </div>
        </aside>
      </div>
    </>
  );
}
