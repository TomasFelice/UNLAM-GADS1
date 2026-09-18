import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { solicitarApi } from "../../shared/api/clienteApi";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton } from "../../shared/components/Boton";
import { Campo, Entrada } from "../../shared/components/Campos";
import { Logo } from "../../shared/components/Logo";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import css from "./PaginaCambiarClave.module.css";

const CLAVE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,72}$/;
const esquema = z
  .object({
    actual: z.string().min(1, "Ingresá tu contraseña actual."),
    nueva: z.string().regex(CLAVE, "Usá entre 10 y 72 caracteres, con mayúscula, minúscula, número y símbolo."),
    repetir: z.string().min(1, "Repetí la nueva contraseña."),
  })
  .refine((datos) => datos.nueva === datos.repetir, {
    path: ["repetir"],
    message: "Las contraseñas no coinciden.",
  });

type DatosClave = z.infer<typeof esquema>;

export function PaginaCambiarClave() {
  const { cerrar } = useSesion();
  const navegar = useNavigate();
  const [errorGeneral, setErrorGeneral] = useState<string>();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosClave>({ resolver: zodResolver(esquema), mode: "onBlur" });

  const alEnviar = handleSubmit(async ({ actual, nueva }) => {
    setErrorGeneral(undefined);
    try {
      await solicitarApi<void>("/auth/change-password", {
        method: "POST",
        body: { currentPassword: actual, newPassword: nueva },
      });
      cerrar();
      navegar("/ingresar", {
        replace: true,
        state: { mensaje: "Contraseña actualizada. Volvé a ingresar con tu nueva clave." },
      });
    } catch (error) {
      setErrorGeneral(error instanceof ErrorDeApi ? error.message : "No pudimos cambiar la contraseña.");
    }
  });

  return (
    <>
      <Seo titulo="Cambiar contraseña" descripcion="Actualizá tu contraseña temporal." noIndexar />
      <main className={css.pantalla}>
        <section className={css.caja}>
          <Logo variante="compacto" tamano={34} />
          <div>
            <p className={css.sobreTitulo}>Primer acceso</p>
            <h1>Creá una contraseña personal</h1>
            <p className={css.bajada}>La clave temporal sólo sirve para iniciar este cambio.</p>
          </div>
          {errorGeneral && <p className={css.errorGeneral} role="alert">{errorGeneral}</p>}
          <form className={css.formulario} onSubmit={alEnviar} noValidate>
            <Campo etiqueta="Contraseña temporal" requerido error={errors.actual?.message}>
              {(id, describedBy) => <Entrada id={id} type="password" autoComplete="current-password" aria-describedby={describedBy} aria-invalid={Boolean(errors.actual)} {...register("actual")} />}
            </Campo>
            <Campo etiqueta="Nueva contraseña" requerido ayuda="10 a 72 caracteres; incluí mayúscula, minúscula, número y símbolo." error={errors.nueva?.message}>
              {(id, describedBy) => <Entrada id={id} type="password" autoComplete="new-password" aria-describedby={describedBy} aria-invalid={Boolean(errors.nueva)} {...register("nueva")} />}
            </Campo>
            <Campo etiqueta="Repetir nueva contraseña" requerido error={errors.repetir?.message}>
              {(id, describedBy) => <Entrada id={id} type="password" autoComplete="new-password" aria-describedby={describedBy} aria-invalid={Boolean(errors.repetir)} {...register("repetir")} />}
            </Campo>
            <Boton type="submit" tamano="lg" anchoCompleto disabled={isSubmitting}>
              {isSubmitting ? "Guardando…" : "Guardar contraseña"}
            </Boton>
          </form>
        </section>
      </main>
    </>
  );
}
