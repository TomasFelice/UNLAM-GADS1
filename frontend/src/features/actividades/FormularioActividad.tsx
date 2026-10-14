import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton } from "../../shared/components/Boton";
import { AreaTexto, Campo, Entrada } from "../../shared/components/Campos";
import { SelectorCatalogo } from "../catalogos/SelectorCatalogo";
import { useRegistrarActividad, type Asociacion } from "./apiActividades";
import css from "./HistorialComercial.module.css";

const esquema = z.object({
  tipo: z.string().min(1, "Elegí un tipo de actividad."),
  fecha: z.string().min(1, "Ingresá la fecha y hora del hecho.").refine(valor => {
    const instante = Date.parse(`${valor}-03:00`);
    return Number.isFinite(instante) && instante <= Date.now();
  }, "La actividad debe haber ocurrido; no puede tener una fecha futura."),
  descripcion: z.string().trim().min(1, "Describí la interacción."),
  resultado: z.string().trim().min(1, "Ingresá el resultado."),
});
type Datos = z.infer<typeof esquema>;
const campos: Record<string, keyof Datos> = { typeId: "tipo", occurredAt: "fecha", description: "descripcion", result: "resultado" };

export function FormularioActividad({ asociacion, alCancelar, alGuardar }: {
  asociacion: Asociacion; alCancelar: () => void; alGuardar: () => void;
}) {
  const guardar = useRegistrarActividad();
  const { register, control, setValue, setError, handleSubmit, formState: { errors, isSubmitting } } = useForm<Datos>({
    resolver: zodResolver(esquema), mode: "onBlur", defaultValues: { tipo: "", fecha: "", descripcion: "", resultado: "" },
  });
  const tipo = useWatch({ control, name: "tipo" });
  const enviar = handleSubmit(async datos => {
    try {
      await guardar.mutateAsync({
        typeId: Number(datos.tipo), occurredAt: new Date(`${datos.fecha}-03:00`).toISOString(),
        description: datos.descripcion, result: datos.resultado,
        ...(asociacion.tipo === "companies" ? { companyId: asociacion.id } : asociacion.tipo === "contacts" ? { contactId: asociacion.id } : { opportunityId: asociacion.id }),
      });
      alGuardar();
    } catch (error) {
      if (error instanceof ErrorDeApi) for (const campo of error.problema.errors) {
        if (campos[campo.field]) setError(campos[campo.field], { message: campo.message }, { shouldFocus: true });
      }
    }
  });
  return <form className={css.formulario} aria-label="Registrar actividad" noValidate onSubmit={enviar}>
    <h3>Registrar actividad</h3>
    <p>Registrá una interacción que ya ocurrió. Se conserva con tu autoría en el historial del cliente.</p>
    <fieldset disabled={isSubmitting} className={css.formulario}>
      <Campo etiqueta="Tipo de actividad" requerido error={errors.tipo?.message}>{(id, ayuda) => <SelectorCatalogo catalogo="tipos-actividad" id={id} descripcion={ayuda} soloActivos requerido valor={tipo} alCambiar={valor => setValue("tipo", valor, { shouldValidate: true })} />}</Campo>
      <Campo etiqueta="Fecha y hora del hecho" requerido ayuda="Hora de Buenos Aires." error={errors.fecha?.message}>{(id, ayuda) => <Entrada id={id} aria-describedby={ayuda} type="datetime-local" autoFocus aria-invalid={!!errors.fecha} {...register("fecha")} />}</Campo>
      <Campo etiqueta="Descripción" requerido error={errors.descripcion?.message}>{(id, ayuda) => <AreaTexto id={id} aria-describedby={ayuda} aria-invalid={!!errors.descripcion} {...register("descripcion")} />}</Campo>
      <Campo etiqueta="Resultado" requerido error={errors.resultado?.message}>{(id, ayuda) => <AreaTexto id={id} aria-describedby={ayuda} aria-invalid={!!errors.resultado} {...register("resultado")} />}</Campo>
    </fieldset>
    {(Object.keys(errors).length > 0 || guardar.isError) && <div role="alert" className={css.error}><p>{guardar.error?.message ?? "Revisá los campos indicados antes de guardar."}</p><ul>{Object.entries(errors).map(([campo, error]) => <li key={campo}>{error.message}</li>)}</ul></div>}
    <div className={css.acciones}><Boton type="button" variante="secundario" disabled={isSubmitting} onClick={alCancelar}>Cancelar actividad</Boton><Boton type="submit" disabled={isSubmitting}>{isSubmitting ? "Guardando…" : "Guardar actividad"}</Boton></div>
  </form>;
}
