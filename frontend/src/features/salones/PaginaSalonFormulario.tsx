import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { AreaTexto, Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { ESTADO_SALON } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import { useGuardarSalon, useSalon } from "./apiSalones";

type Datos = { nombre: string; capacidad: string; tarifa: string; direccion: string; localidad: string; descripcion: string; equipamiento: string; estado: "DISPONIBLE" | "MANTENIMIENTO" | "INACTIVO" };
const campos: Record<string, keyof Datos> = { name: "nombre", capacity: "capacidad", rate: "tarifa", address: "direccion", locality: "localidad", description: "descripcion", equipment: "equipamiento", status: "estado" };

export function PaginaSalonFormulario() {
  const { id: textoId } = useParams();
  const id = textoId ? Number(textoId) : undefined;
  const valido = id === undefined || (Number.isSafeInteger(id) && id > 0);
  const consulta = useSalon(valido ? id : undefined);
  const guardar = useGuardarSalon(id);
  const navegar = useNavigate();
  const [error, setErrorGeneral] = useState<string>();
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Datos>({ defaultValues: {
    nombre: "", capacidad: "", tarifa: "", direccion: "", localidad: "", descripcion: "", equipamiento: "", estado: "DISPONIBLE",
  } });
  useEffect(() => { if (consulta.data) { const s = consulta.data; reset({ nombre: s.name ?? "", capacidad: String(s.capacity ?? ""), tarifa: s.rate == null ? "" : String(s.rate), direccion: s.address ?? "", localidad: s.locality ?? "", descripcion: s.description ?? "", equipamiento: (s.equipment ?? []).join("\n"), estado: s.status ?? "INACTIVO" }); } }, [consulta.data, reset]);
  const enviar = handleSubmit(async datos => {
    setErrorGeneral(undefined);
    try {
      const salon = await guardar.mutateAsync({ name: datos.nombre, capacity: Number(datos.capacidad), rate: datos.tarifa === "" ? undefined : Number(datos.tarifa), address: datos.direccion, locality: datos.localidad, description: datos.descripcion, equipment: datos.equipamiento.split("\n").map(e => e.trim()).filter(Boolean), status: datos.estado });
      navegar(`/app/salones/${salon.id}`);
    } catch (fallo) {
      setErrorGeneral(fallo instanceof Error ? fallo.message : "No se pudo guardar el salón.");
      if (fallo instanceof ErrorDeApi) fallo.problema.errors.forEach(e => { const campo = campos[e.field] ?? (e.field.startsWith("equipment") ? "equipamiento" : undefined); if (campo) setError(campo, { message: e.message }); });
    }
  });
  const seo = <Seo titulo={id ? "Editar salón" : "Nuevo salón"} descripcion="" noIndexar />;
  if (!valido || (id && consulta.isError)) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar el salón" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<BotonEnlace a="/app/salones">Volver a salones</BotonEnlace>} /></>;
  if (id && consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando salón…" descripcion="Preparando sus datos." /></>;
  return <>{seo}<CabeceraPagina titulo={id ? "Editar salón" : "Nuevo salón"} migas={[{ texto: "Salones", a: "/app/salones" }, { texto: id ? "Editar" : "Nuevo" }]} descripcion="Definí el espacio y su disponibilidad para eventos." />
    <form onSubmit={enviar}><Tarjeta><TarjetaCuerpo>{error && <p role="alert">{error}</p>}<GrupoCampos titulo="Espacio y capacidad">
      <Campo etiqueta="Nombre" requerido error={errors.nombre?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} {...register("nombre", { required: "Ingresá el nombre.", validate: v => !!v.trim() || "Ingresá el nombre." })} />}</Campo>
      <Campo etiqueta="Capacidad" requerido error={errors.capacidad?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} type="number" min={1} step={1} {...register("capacidad", { required: "Ingresá la capacidad.", validate: v => Number.isInteger(Number(v)) && Number(v) > 0 || "Ingresá una cantidad entera positiva." })} />}</Campo>
      <Campo etiqueta="Tarifa por jornada" error={errors.tarifa?.message} ayuda="Opcional, en pesos.">{(x,a) => <Entrada id={x} aria-describedby={a} type="number" min={0} step="0.01" {...register("tarifa")} />}</Campo>
      <Campo etiqueta="Estado" requerido error={errors.estado?.message} ayuda="Mantenimiento o inactivo impiden nuevas asociaciones; conservan las anteriores.">{(x,a) => <Selector id={x} aria-describedby={a} {...register("estado")}>{Object.entries(ESTADO_SALON).map(([valor, estado]) => <option key={valor} value={valor}>{estado.texto}</option>)}</Selector>}</Campo>
    </GrupoCampos><GrupoCampos titulo="Ubicación y características">
      <Campo etiqueta="Dirección" error={errors.direccion?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} {...register("direccion")} />}</Campo>
      <Campo etiqueta="Localidad" error={errors.localidad?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} {...register("localidad")} />}</Campo>
      <Campo etiqueta="Descripción" ancho error={errors.descripcion?.message}>{(x,a) => <AreaTexto id={x} aria-describedby={a} {...register("descripcion")} />}</Campo>
      <Campo etiqueta="Equipamiento" ancho error={errors.equipamiento?.message} ayuda="Un elemento por línea.">{(x,a) => <AreaTexto id={x} aria-describedby={a} {...register("equipamiento")} />}</Campo>
    </GrupoCampos></TarjetaCuerpo><TarjetaPie><BotonEnlace a={id ? `/app/salones/${id}` : "/app/salones"} variante="secundario">Cancelar</BotonEnlace><Boton type="submit" disabled={isSubmitting || guardar.isPending}>{id ? "Guardar cambios" : "Crear salón"}</Boton></TarjetaPie></Tarjeta></form></>;
}
