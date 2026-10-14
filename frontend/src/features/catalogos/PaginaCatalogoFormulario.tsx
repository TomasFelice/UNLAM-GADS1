import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { CATALOGOS, useEntradaCatalogo, useGuardarEntrada, type Catalogo } from "./apiCatalogos";

type Datos = { nombre: string; estado: string };
export function PaginaCatalogoFormulario({ catalogo }: { catalogo: Catalogo }) {
  const { id: textoId } = useParams();
  const id = textoId ? Number(textoId) : undefined;
  const valido = id === undefined || (Number.isSafeInteger(id) && id > 0);
  const consulta = useEntradaCatalogo(catalogo, id);
  const guardar = useGuardarEntrada(catalogo, id);
  const config = CATALOGOS[catalogo];
  const ruta = `/app/configuracion/${catalogo}`;
  const navegar = useNavigate();
  const [error, setErrorGeneral] = useState<string>();
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Datos>({ defaultValues: { nombre: "", estado: "true" } });
  useEffect(() => { if (consulta.data) reset({ nombre: consulta.data.name ?? "", estado: String(consulta.data.active ?? false) }); }, [consulta.data, reset]);
  const enviar = handleSubmit(async datos => {
    setErrorGeneral(undefined);
    try {
      const guardada = await guardar.mutateAsync({ name: datos.nombre.trim(), active: datos.estado === "true" });
      navegar(`${ruta}/${guardada.id}`);
    } catch (fallo) {
      setErrorGeneral(fallo instanceof Error ? fallo.message : "No se pudo guardar la opción.");
      if (fallo instanceof ErrorDeApi) fallo.problema.errors.forEach(e => { if (e.field === "name" || e.field === "active") setError(e.field === "name" ? "nombre" : "estado", { message: e.message }); });
    }
  });
  const seo = <Seo titulo={id ? `Editar ${config.singular}` : `Nuevo ${config.singular}`} descripcion={config.descripcion} noIndexar />;
  if (!valido || (id && consulta.isError)) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar esta opción" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<><BotonEnlace a={ruta}>Volver al catálogo</BotonEnlace>{consulta.isError && <Boton variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton>}</>} /></>;
  if (id && consulta.isPending) return <>{seo}<EstadoVacio icono="salones" titulo="Cargando opción…" descripcion="Preparando sus datos." /></>;
  return <>{seo}<CabeceraPagina titulo={id ? `Editar ${config.singular}` : `Nuevo ${config.singular}`} descripcion={config.descripcion} migas={[{ texto: "Configuración", a: "/app/configuracion" }, { texto: config.titulo, a: ruta }, { texto: id ? "Editar" : "Nuevo" }]} />
    <form onSubmit={enviar}><Tarjeta><TarjetaCuerpo>{error && <p role="alert">{error}</p>}<GrupoCampos titulo="Datos de la opción">
      <Campo etiqueta="Nombre" requerido error={errors.nombre?.message}>{(x,a) => <Entrada id={x} aria-describedby={a} {...register("nombre", { validate: v => !!v.trim() || "Ingresá el nombre." })} />}</Campo>
      <Campo etiqueta="Estado" error={errors.estado?.message} ayuda="Inactivo impide nuevas asociaciones y conserva las anteriores.">{(x,a) => <Selector id={x} aria-describedby={a} {...register("estado")}><option value="true">Activo</option><option value="false">Inactivo</option></Selector>}</Campo>
    </GrupoCampos></TarjetaCuerpo><TarjetaPie><BotonEnlace a={id ? `${ruta}/${id}` : ruta} variante="secundario">Cancelar</BotonEnlace><Boton type="submit" disabled={isSubmitting || guardar.isPending}>{id ? "Guardar cambios" : `Crear ${config.singular}`}</Boton></TarjetaPie></Tarjeta></form></>;
}
