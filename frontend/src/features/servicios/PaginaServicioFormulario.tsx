import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { AreaTexto, Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { useGuardarServicio, useServicio } from "./apiServicios";

type Datos = { nombre: string; descripcion: string; precio: string; estado: string };
const campos: Record<string, keyof Datos> = { name: "nombre", description: "descripcion", price: "precio", active: "estado" };

export function PaginaServicioFormulario() {
  const { id: textoId } = useParams();
  const id = textoId ? Number(textoId) : undefined;
  const valido = id === undefined || (Number.isSafeInteger(id) && id > 0);
  const consulta = useServicio(valido ? id : undefined);
  const guardar = useGuardarServicio(id);
  const navegar = useNavigate();
  const [error, setErrorGeneral] = useState<string>();
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Datos>({ defaultValues: { nombre: "", descripcion: "", precio: "", estado: "true" } });
  useEffect(() => { if (consulta.data) { const s = consulta.data; reset({ nombre: s.name ?? "", descripcion: s.description ?? "", precio: s.price == null ? "" : String(s.price), estado: String(s.active ?? false) }); } }, [consulta.data, reset]);
  const enviar = handleSubmit(async datos => {
    setErrorGeneral(undefined);
    try {
      const servicio = await guardar.mutateAsync({ name: datos.nombre, description: datos.descripcion, price: datos.precio === "" ? undefined : Number(datos.precio), active: datos.estado === "true" });
      navegar(`/app/servicios/${servicio.id}`);
    } catch (fallo) {
      setErrorGeneral(fallo instanceof Error ? fallo.message : "No se pudo guardar el servicio.");
      if (fallo instanceof ErrorDeApi) fallo.problema.errors.forEach(e => { if (campos[e.field]) setError(campos[e.field], { message: e.message }); });
    }
  });
  const seo = <Seo titulo={id ? "Editar servicio" : "Nuevo servicio"} descripcion="" noIndexar />;
  if (!valido || (id && consulta.isError)) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar el servicio" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<BotonEnlace a="/app/servicios">Volver a servicios</BotonEnlace>} /></>;
  if (id && consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando servicio…" descripcion="Preparando sus datos." /></>;
  return <>{seo}<CabeceraPagina titulo={id ? "Editar servicio" : "Nuevo servicio"} migas={[{ texto: "Servicios", a: "/app/servicios" }, { texto: id ? "Editar" : "Nuevo" }]} descripcion="Configurá el servicio adicional y su disponibilidad." />
    <form onSubmit={enviar}><Tarjeta><TarjetaCuerpo>{error && <p role="alert">{error}</p>}<GrupoCampos titulo="Servicio adicional">
      <Campo etiqueta="Nombre" requerido error={errors.nombre?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} {...register("nombre", { required: "Ingresá el nombre.", validate: v => !!v.trim() || "Ingresá el nombre." })} />}</Campo>
      <Campo etiqueta="Precio" error={errors.precio?.message} ayuda="Opcional, en pesos.">{(x,a) => <Entrada id={x} aria-describedby={a} type="number" min={0} step="0.01" {...register("precio")} />}</Campo>
      <Campo etiqueta="Estado" error={errors.estado?.message} ayuda="Inactivo impide nuevas asociaciones y conserva las anteriores.">{(x,a) => <Selector id={x} aria-describedby={a} {...register("estado")}><option value="true">Activo</option><option value="false">Inactivo</option></Selector>}</Campo>
      <Campo etiqueta="Descripción" ancho error={errors.descripcion?.message}>{(x,a) => <AreaTexto id={x} aria-describedby={a} {...register("descripcion")} />}</Campo>
    </GrupoCampos></TarjetaCuerpo><TarjetaPie><BotonEnlace a={id ? `/app/servicios/${id}` : "/app/servicios"} variante="secundario">Cancelar</BotonEnlace><Boton type="submit" disabled={isSubmitting || guardar.isPending}>{id ? "Guardar cambios" : "Crear servicio"}</Boton></TarjetaPie></Tarjeta></form></>;
}
