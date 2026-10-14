import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { NOMBRES_ROLES, useGuardarUsuario, useUsuario } from "./apiUsuarios";
import { esquemaClaveTemporal } from "./validacionUsuarios";
import css from "./Usuarios.module.css";

const esquema = z.object({
  nombre: z.string().trim().min(1, "Ingresá el nombre."),
  apellido: z.string().trim().min(1, "Ingresá el apellido."),
  email: z.email("Ingresá un correo válido."),
  rol: z.enum(["ADMIN", "SALES_MANAGER", "SELLER"]),
  activo: z.boolean(), claveTemporal: z.string(),
});
type DatosUsuario = z.infer<typeof esquema>;
const campos: Record<string, keyof DatosUsuario> = {
  firstName: "nombre", lastName: "apellido", email: "email", role: "rol", active: "activo", temporaryPassword: "claveTemporal",
};

export function PaginaUsuarioFormulario() {
  const { id: idTexto } = useParams();
  const id = idTexto ? Number(idTexto) : undefined;
  const editar = idTexto !== undefined;
  const valido = id === undefined || (Number.isSafeInteger(id) && id > 0);
  const consulta = useUsuario(valido ? id : undefined);
  const guardar = useGuardarUsuario(id);
  const navegar = useNavigate();
  const { sesion, cerrar } = useSesion();
  const [error, setErrorGeneral] = useState<string>();
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<DatosUsuario>({
    resolver: zodResolver(esquema), mode: "onBlur",
    defaultValues: { nombre: "", apellido: "", email: "", rol: "SELLER", activo: true, claveTemporal: "" },
  });
  useEffect(() => {
    if (consulta.data) reset({ nombre: consulta.data.firstName ?? "", apellido: consulta.data.lastName ?? "",
      email: consulta.data.email ?? "", rol: consulta.data.role ?? "SELLER", activo: consulta.data.active ?? false, claveTemporal: "" });
  }, [consulta.data, reset]);

  const enviar = handleSubmit(async datos => {
    setErrorGeneral(undefined);
    if (!editar) {
      const clave = esquemaClaveTemporal.safeParse(datos.claveTemporal);
      if (!clave.success) { setError("claveTemporal", { message: clave.error.issues[0].message }); return; }
    }
    try {
      const base = { email: datos.email, firstName: datos.nombre, lastName: datos.apellido, role: datos.rol };
      const usuario = await guardar.mutateAsync(editar ? { ...base, active: datos.activo }
        : { ...base, temporaryPassword: datos.claveTemporal });
      if (id === sesion?.usuario.id && datos.rol !== sesion.usuario.rol) {
        cerrar({ mensaje: "Tus permisos cambiaron. Volvé a ingresar." });
      } else navegar(`/app/usuarios/${usuario.id}`, { replace: true });
      reset();
    } catch (fallo) {
      if (fallo instanceof ErrorDeApi) {
        fallo.problema.errors.forEach(detalle => { if (campos[detalle.field]) setError(campos[detalle.field], { message: detalle.message }); });
      }
      setErrorGeneral(fallo instanceof Error ? fallo.message : "No pudimos guardar el usuario.");
    }
  });

  return <>
    <Seo titulo={editar ? "Editar usuario" : "Nuevo usuario"} descripcion="Perfil y permisos del equipo." noIndexar />
    {!valido ? <EstadoVacio icono="usuario" titulo="Enlace de usuario inválido" descripcion="Elegí un usuario desde el listado." />
      : editar && consulta.isPending ? <EstadoVacio icono="usuario" titulo="Cargando usuario…" descripcion="Preparando el formulario." />
      : editar && consulta.isError ? <EstadoVacio icono="alerta" titulo="No pudimos abrir el usuario" descripcion={consulta.error.message} />
      : <>
        <CabeceraPagina titulo={editar ? "Editar usuario" : "Nuevo usuario"}
          descripcion={editar ? "Actualizá los datos, el rol y el estado del integrante." : "Creá el acceso de un integrante con una contraseña temporal."}
          migas={[{ texto: "Usuarios", a: "/app/usuarios" }, { texto: editar ? "Editar" : "Nuevo" }]} />
        <form onSubmit={enviar} noValidate>
          <Tarjeta><TarjetaCuerpo>
            {error && <p role="alert" className={css.error}>{error}</p>}
            <GrupoCampos titulo="Perfil">
              <Campo etiqueta="Nombre" requerido error={errors.nombre?.message}>{(id, ayuda) => <Entrada id={id} autoComplete="given-name" aria-describedby={ayuda} aria-invalid={!!errors.nombre} {...register("nombre")} />}</Campo>
              <Campo etiqueta="Apellido" requerido error={errors.apellido?.message}>{(id, ayuda) => <Entrada id={id} autoComplete="family-name" aria-describedby={ayuda} aria-invalid={!!errors.apellido} {...register("apellido")} />}</Campo>
              <Campo etiqueta="Correo electrónico" requerido error={errors.email?.message}>{(id, ayuda) => <Entrada id={id} type="email" autoComplete="off" aria-describedby={ayuda} aria-invalid={!!errors.email} {...register("email")} />}</Campo>
              <Campo etiqueta="Rol" requerido error={errors.rol?.message}>{(id, ayuda) => <Selector id={id} aria-describedby={ayuda} {...register("rol")}>
                {Object.entries(NOMBRES_ROLES).map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}
              </Selector>}</Campo>
              {editar ? <Campo etiqueta="Estado" ayuda={id === sesion?.usuario.id ? "Tu cuenta debe permanecer activa." : "La baja conserva la autoría y el historial comercial."}>
                {idCampo => <label><input id={idCampo} type="checkbox" disabled={id === sesion?.usuario.id} {...register("activo")} /> Usuario activo</label>}
              </Campo> : <Campo etiqueta="Contraseña temporal" requerido error={errors.claveTemporal?.message}
                ayuda="10 a 72 caracteres, con mayúscula, minúscula, número y símbolo. Deberá cambiarla al ingresar.">
                {(id, ayuda) => <Entrada id={id} type="password" autoComplete="new-password" aria-describedby={ayuda} aria-invalid={!!errors.claveTemporal} {...register("claveTemporal")} />}
              </Campo>}
            </GrupoCampos>
          </TarjetaCuerpo><TarjetaPie><div className={css.acciones}>
            <BotonEnlace a={editar ? `/app/usuarios/${id}` : "/app/usuarios"} variante="secundario">Cancelar</BotonEnlace>
            <Boton type="submit" disabled={isSubmitting || guardar.isPending}>{isSubmitting ? "Guardando…" : editar ? "Guardar cambios" : "Crear usuario"}</Boton>
          </div></TarjetaPie></Tarjeta>
        </form>
      </>}
  </>;
}
