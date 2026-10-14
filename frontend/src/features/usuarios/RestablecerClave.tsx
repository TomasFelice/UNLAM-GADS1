import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AvisoExito } from "../../shared/components/AvisoExito";
import { Boton } from "../../shared/components/Boton";
import { Campo, Entrada } from "../../shared/components/Campos";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useRestablecerClave } from "./apiUsuarios";
import { esquemaClaveTemporal } from "./validacionUsuarios";
import css from "./Usuarios.module.css";

const esquema = z.object({ clave: esquemaClaveTemporal });

export function RestablecerClave({ usuarioId }: { usuarioId: number }) {
  const [abierto, setAbierto] = useState(false);
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");
  const { sesion, cerrar } = useSesion();
  const reiniciar = useRestablecerClave(usuarioId);
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<z.infer<typeof esquema>>({
    resolver: zodResolver(esquema), mode: "onBlur", defaultValues: { clave: "" },
  });
  const enviar = handleSubmit(async datos => {
    setError(""); setMensaje("");
    try {
      await reiniciar.mutateAsync({ temporaryPassword: datos.clave });
      reset(); setAbierto(false);
      if (usuarioId === sesion?.usuario.id) {
        cerrar({ mensaje: "Contraseña restablecida. Ingresá con la nueva clave temporal." });
      } else setMensaje("Contraseña restablecida. El usuario deberá cambiarla al ingresar; sus sesiones anteriores quedaron cerradas.");
    } catch (fallo) { setError(fallo instanceof Error ? fallo.message : "No pudimos restablecer la contraseña."); }
  });
  return <Tarjeta><TarjetaCabecera titulo="Acceso a la cuenta" /><TarjetaCuerpo>
    {mensaje && <AvisoExito>{mensaje}</AvisoExito>}
    {abierto ? <form onSubmit={enviar} noValidate className={css.formulario}>
      <p className={css.ayuda}>Se cerrarán las sesiones anteriores y el usuario deberá elegir una contraseña personal al ingresar.</p>
      {error && <p role="alert" className={css.error}>{error}</p>}
      <Campo etiqueta="Nueva contraseña temporal" requerido error={errors.clave?.message}>
        {(id, ayuda) => <Entrada id={id} type="password" autoComplete="new-password" autoFocus aria-describedby={ayuda} aria-invalid={!!errors.clave} {...register("clave")} />}
      </Campo>
      <div className={css.acciones}>
        <Boton type="button" variante="secundario" disabled={isSubmitting} onClick={() => { reset(); setAbierto(false); }}>Cancelar</Boton>
        <Boton type="submit" disabled={isSubmitting}>{isSubmitting ? "Restableciendo…" : "Confirmar restablecimiento"}</Boton>
      </div>
    </form> : <Boton variante="secundario" onClick={() => { setError(""); setAbierto(true); }}>Restablecer contraseña</Boton>}
  </TarjetaCuerpo></Tarjeta>;
}
