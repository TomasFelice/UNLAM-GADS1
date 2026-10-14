import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { solicitarApi } from "../../shared/api/clienteApi";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { AreaTexto, Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { useSesion } from "../../shared/sesion/contextoSesion";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEtapas } from "../catalogos/apiCatalogos";
import { SelectorCatalogo } from "../catalogos/SelectorCatalogo";
import { SelectorServicios } from "../catalogos/SelectorServicios";
import { SelectorCliente } from "../clientes/SelectorCliente";
import { clavesContactos, useContacto, type ContactoDetalle } from "../contactos/apiContactos";
import { useEmpresa, type EmpresaDetalle } from "../empresas/apiEmpresas";
import { useEmbudoPredeterminado } from "../embudos/apiEmbudos";
import { NombreEmbudo } from "../embudos/NombreEmbudo";
import { SelectorEmbudo } from "../embudos/SelectorEmbudo";
import { useSalon } from "../salones/apiSalones";
import { SelectorSalon } from "../salones/SelectorSalon";
import { useGuardarOportunidad, useOportunidad, type OportunidadDetalle } from "./apiOportunidades";
import css from "./FormularioOportunidad.module.css";

const esquema = z.object({
  titulo: z.string().trim().min(1, "Ingresá un título."), empresaId: z.string(), contactoId: z.string(), responsableId: z.string(),
  embudoId: z.string().min(1, "Elegí un embudo."), etapaId: z.string().min(1, "Elegí una etapa abierta."),
  origenId: z.string(), salonId: z.string().min(1, "Elegí un salón."), tipoEventoId: z.string().min(1, "Elegí un tipo de evento."),
  servicios: z.array(z.string()), inicio: z.string().min(1, "Ingresá el inicio."), fin: z.string().min(1, "Ingresá el fin."),
  asistentes: z.string().refine(v => Number.isInteger(Number(v)) && Number(v) > 0, "Ingresá una cantidad entera mayor que cero."),
  valor: z.string().refine(v => !v || Number(v) >= 0, "El valor no puede ser negativo."),
  probabilidad: z.string().refine(v => !v || Number(v) >= 0 && Number(v) <= 100, "Usá un valor entre 0 y 100."), cierre: z.string(), observaciones: z.string(),
}).refine(d => d.empresaId || d.contactoId, { message: "Elegí una empresa o un contacto.", path: ["empresaId"] })
  .refine(d => !d.inicio || !d.fin || d.fin > d.inicio, { message: "El fin debe ser posterior al inicio.", path: ["fin"] });
type Datos = z.infer<typeof esquema>;
const aLocal = (iso?: string) => iso ? new Date(new Date(iso).getTime() - 3 * 3600000).toISOString().slice(0, 16) : "";
const aUtc = (local: string) => new Date(`${local}:00-03:00`).toISOString();
const numero = (valor: string) => valor ? Number(valor) : undefined;
const esId = (id?: number) => Number.isSafeInteger(id) && id! > 0;

export function PaginaOportunidadFormulario() {
  const { id: textoId } = useParams(); const [url] = useSearchParams();
  const id = textoId ? Number(textoId) : undefined;
  const contactoId = !textoId && url.has("contactoId") ? Number(url.get("contactoId")) : undefined;
  const empresaId = !textoId && !url.has("contactoId") && url.has("empresaId") ? Number(url.get("empresaId")) : undefined;
  const consulta = useOportunidad(esId(id) ? id : undefined);
  const contacto = useContacto(esId(contactoId) ? contactoId : undefined);
  const empresa = useEmpresa(esId(empresaId) ? empresaId : undefined);
  const invalido = [id, contactoId, empresaId].some(v => v !== undefined && !esId(v));
  const error = consulta.error || contacto.error || empresa.error;
  const seo = <Seo titulo={id ? "Editar oportunidad" : "Nueva oportunidad"} descripcion="Negociación y datos del evento." noIndexar />;
  if (invalido || error) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos abrir el formulario" descripcion={error?.message ?? "El enlace no es válido."} accion={<BotonEnlace a="/app/oportunidades" variante="secundario">Volver a oportunidades</BotonEnlace>} /></>;
  if (id !== undefined && consulta.isPending || contactoId !== undefined && contacto.isPending || empresaId !== undefined && empresa.isPending) return <>{seo}<EstadoVacio icono="oportunidades" titulo="Cargando oportunidad…" descripcion="Preparando los datos del cliente." /></>;
  if (consulta.data && consulta.data.status !== "ABIERTA") return <>{seo}<EstadoVacio icono="oportunidades" titulo="La oportunidad está cerrada" descripcion="Consultá su detalle para ver las acciones disponibles." accion={<BotonEnlace a={`/app/oportunidades/${id}`}>Volver al detalle</BotonEnlace>} /></>;
  return <FormularioOportunidad key={`${id ?? "nueva"}-${contactoId ?? ""}-${empresaId ?? ""}`} id={id} oportunidad={consulta.data} contactoInicial={contacto.data} empresaInicial={empresa.data} embudoInicial={url.get("pipelineId") ?? ""} />;
}

function FormularioOportunidad({ id, oportunidad: o, contactoInicial, empresaInicial, embudoInicial }: { id?: number; oportunidad?: OportunidadDetalle; contactoInicial?: ContactoDetalle; empresaInicial?: EmpresaDetalle; embudoInicial: string }) {
  const [versionLeida] = useState(o?.version);
  const editar = id !== undefined; const breve = !editar && !!(contactoInicial || empresaInicial);
  const navegar = useNavigate(); const cache = useQueryClient(); const { sesion } = useSesion();
  const guardar = useGuardarOportunidad(id); const usuarios = useUsuariosAsignables(); const predeterminado = useEmbudoPredeterminado(!editar);
  const [errorGeneral, setErrorGeneral] = useState<string>(); const [seleccionando, setSeleccionando] = useState(false); const turno = useRef(0);
  const { register, handleSubmit, setError, setValue, control, formState: { errors, isSubmitting } } = useForm<Datos>({ resolver: zodResolver(esquema), mode: "onBlur", defaultValues: {
    titulo: o?.title ?? "", empresaId: String(o?.company?.id ?? contactoInicial?.company?.id ?? empresaInicial?.id ?? ""), contactoId: String(o?.contact?.id ?? contactoInicial?.id ?? ""),
    responsableId: "", embudoId: String(o?.pipelineId ?? embudoInicial), etapaId: String(o?.stage?.id ?? ""), origenId: String(o?.originId ?? ""), salonId: String(o?.venue?.id ?? ""),
    tipoEventoId: String(o?.eventTypeId ?? ""), servicios: (o?.serviceIds ?? []).map(String), inicio: aLocal(o?.eventStart), fin: aLocal(o?.eventEnd), asistentes: String(o?.attendeeCount ?? ""),
    valor: String(o?.estimatedValue ?? ""), probabilidad: String(o?.probability ?? ""), cierre: o?.estimatedCloseDate ?? "", observaciones: o?.notes ?? "",
  } });
  const valores = useWatch({ control }); const embudoId = valores.embudoId ?? "";
  const etapas = useEtapas(esId(numero(embudoId)) ? Number(embudoId) : undefined, esId(numero(embudoId)));
  const contacto = useContacto(numero(valores.contactoId ?? "")); const empresa = useEmpresa(numero(valores.empresaId ?? ""));
  const salon = useSalon(numero(valores.salonId ?? "")); const asistentes = Number(valores.asistentes ?? 0);
  useEffect(() => { if (!editar && !embudoId && predeterminado.data?.id) setValue("embudoId", String(predeterminado.data.id)); }, [editar, embudoId, predeterminado.data, setValue]);
  useEffect(() => { if (!editar && !valores.etapaId && etapas.data) { const primera = etapas.data.find(e => e.kind === "OPEN" && e.active); if (primera?.id) setValue("etapaId", String(primera.id)); } }, [editar, valores.etapaId, etapas.data, setValue]);
  const cambiarEmpresa = (valor: string) => { turno.current++; setSeleccionando(false); setValue("empresaId", valor, { shouldDirty: true, shouldValidate: true }); if (valor !== valores.empresaId) setValue("contactoId", "", { shouldDirty: true }); };
  const cambiarContacto = async (valor: string) => {
    const actual = ++turno.current; setErrorGeneral(undefined); setValue("contactoId", valor, { shouldDirty: true });
    if (!valor) { setSeleccionando(false); return; }
    setSeleccionando(true);
    try {
      const elegido = await cache.fetchQuery({ queryKey: clavesContactos.detalle(Number(valor)), queryFn: ({ signal }) => solicitarApi<ContactoDetalle>(`/contacts/${valor}`, { signal }) });
      if (turno.current === actual) setValue("empresaId", String(elegido.company?.id ?? ""), { shouldDirty: true, shouldValidate: true });
    } catch (fallo) { if (turno.current === actual) setErrorGeneral(fallo instanceof Error ? fallo.message : "No se pudo consultar el contacto."); }
    finally { if (turno.current === actual) setSeleccionando(false); }
  };
  const cancelar = id ? `/app/oportunidades/${id}` : contactoInicial?.id ? `/app/contactos/${contactoInicial.id}` : empresaInicial?.id ? `/app/empresas/${empresaInicial.id}` : "/app/oportunidades";
  const nombreHeredado = contacto.data?.salesRepName || empresa.data?.salesRepName || (sesion ? `${sesion.usuario.nombre} ${sesion.usuario.apellido}` : "tu usuario");
  const enviar = handleSubmit(async datos => {
    if (salon.data?.capacity && Number(datos.asistentes) > salon.data.capacity) { setError("asistentes", { message: `La capacidad máxima de ${salon.data.name} es ${salon.data.capacity}.` }); return; }
    setErrorGeneral(undefined);
    try {
      const base = { pipelineId: Number(datos.embudoId), title: datos.titulo, companyId: numero(datos.empresaId), contactId: numero(datos.contactoId), venueId: Number(datos.salonId), eventTypeId: Number(datos.tipoEventoId), estimatedValue: numero(datos.valor), probability: numero(datos.probabilidad), eventStart: aUtc(datos.inicio), eventEnd: aUtc(datos.fin), attendeeCount: Number(datos.asistentes), estimatedCloseDate: datos.cierre || undefined, originId: numero(datos.origenId), serviceIds: datos.servicios.map(Number), notes: datos.observaciones || undefined };
      const guardada = await guardar.mutateAsync(editar ? { ...base, version: versionLeida! } : { ...base, salesRepId: numero(datos.responsableId), stageId: Number(datos.etapaId) });
      navegar(`/app/oportunidades/${guardada.id}`);
    } catch (fallo) {
      setErrorGeneral(fallo instanceof Error ? fallo.message : "No se pudo guardar la oportunidad.");
      if (fallo instanceof ErrorDeApi) fallo.problema.errors.forEach(e => setError(({ pipelineId: "embudoId", stageId: "etapaId", title: "titulo", companyId: "empresaId", contactId: "contactoId", salesRepId: "responsableId", venueId: "salonId", eventTypeId: "tipoEventoId", eventStart: "inicio", eventEnd: "fin", attendeeCount: "asistentes", estimatedValue: "valor", probability: "probabilidad", notes: "observaciones" } as Record<string, keyof Datos>)[e.field] ?? "observaciones", { message: e.message }));
    }
  });
  const conflicto = guardar.error instanceof ErrorDeApi && guardar.error.problema.status === 409;
  const bloqueado = conflicto || isSubmitting || guardar.isPending || seleccionando || etapas.isPending || etapas.isError;
  return <><Seo titulo={editar ? "Editar oportunidad" : breve ? "Nueva oportunidad desde cliente" : "Nueva oportunidad"} descripcion="Negociación y datos del evento." noIndexar />
    {conflicto && <div role="alert" className={css.resumen}><p>Conservamos lo que escribiste. Recargá para consultar la versión vigente antes de guardar.</p><Boton type="button" variante="secundario" onClick={() => window.location.reload()}>Recargar datos</Boton></div>}
    <CabeceraPagina migas={[{ texto: "Oportunidades", a: "/app/oportunidades" }, { texto: editar ? "Editar" : "Nueva" }]} titulo={editar ? "Editar oportunidad" : "Nueva oportunidad"} descripcion={breve ? "El cliente ya está seleccionado. Completá el evento para iniciar la negociación." : "Completá la negociación, el evento y el seguimiento comercial."} />
    <form onSubmit={enviar}><Tarjeta><TarjetaCuerpo>
    {(errorGeneral || Object.keys(errors).length > 0) && <div role="alert" className={css.resumen}><p>{errorGeneral ?? "Revisá los campos indicados antes de guardar."}</p>{Object.keys(errors).length > 0 && <ul>{Object.entries(errors).map(([campo, error]) => <li key={campo}>{error.message}</li>)}</ul>}</div>}
    <GrupoCampos titulo="Negociación">
      <Campo etiqueta="Título" requerido ancho error={errors.titulo?.message}>{(x, a) => <Entrada id={x} aria-describedby={a} {...register("titulo")} />}</Campo>
      <Campo etiqueta="Empresa" error={errors.empresaId?.message}>{(x, a) => <SelectorCliente tipo="empresa" id={x} descripcion={a} valor={valores.empresaId} alCambiar={cambiarEmpresa} />}</Campo>
      <Campo etiqueta="Contacto" error={errors.contactoId?.message}>{(x, a) => <SelectorCliente key={valores.empresaId} tipo="contacto" empresaId={valores.empresaId} id={x} descripcion={a} valor={valores.contactoId} alCambiar={v => void cambiarContacto(v)} />}</Campo>
      <Campo etiqueta="Embudo" requerido error={errors.embudoId?.message}>{(x, a) => editar ? <NombreEmbudo id={o?.pipelineId} /> : <SelectorEmbudo id={x} descripcion={a} valor={embudoId} soloActivos requerido alCambiar={v => { setValue("embudoId", v, { shouldDirty: true, shouldValidate: true }); setValue("etapaId", ""); }} />}</Campo>
      {!editar && <Campo etiqueta="Etapa inicial" requerido error={errors.etapaId?.message}>{(x, a) => <Selector id={x} aria-describedby={a} {...register("etapaId")}><option value="">Elegí una etapa</option>{etapas.data?.filter(e => e.kind === "OPEN" && e.active).map(e => <option key={e.id} value={e.id}>{e.name}</option>)}</Selector>}</Campo>}
    </GrupoCampos>
    {!editar && <p>Responsable propuesto: {nombreHeredado}. Se hereda del contacto, luego de la empresa y, si no hay asignación, queda a tu cargo.</p>}
    {seleccionando && <p role="status">Consultando la relación del contacto…</p>}
    {(predeterminado.isError && !editar || etapas.isError) && <p role="alert">{etapas.error?.message ?? predeterminado.error?.message}<Boton type="button" variante="secundario" onClick={() => { void etapas.refetch(); if (!editar) void predeterminado.refetch(); }}>Reintentar etapas</Boton></p>}
    <GrupoCampos titulo="Evento y salón">
      <Campo etiqueta="Salón" requerido error={errors.salonId?.message}>{(x, a) => <SelectorSalon id={x} describedBy={a} valor={valores.salonId} alCambiar={v => setValue("salonId", v, { shouldValidate: true, shouldDirty: true })} soloDisponibles />}</Campo>
      <Campo etiqueta="Tipo de evento" requerido error={errors.tipoEventoId?.message}>{(x, a) => <SelectorCatalogo id={x} descripcion={a} catalogo="tipos-evento" valor={valores.tipoEventoId} alCambiar={v => setValue("tipoEventoId", v, { shouldDirty: true, shouldValidate: true })} soloActivos requerido />}</Campo>
      <Campo etiqueta="Inicio" requerido error={errors.inicio?.message}>{(x, a) => <Entrada id={x} type="datetime-local" aria-describedby={a} {...register("inicio")} />}</Campo>
      <Campo etiqueta="Fin" requerido error={errors.fin?.message}>{(x, a) => <Entrada id={x} type="datetime-local" aria-describedby={a} {...register("fin")} />}</Campo>
      <Campo etiqueta="Asistentes" requerido error={errors.asistentes?.message} ayuda={asistentes > 50 ? "Más de 50 asistentes: confirmá que el salón elegido tenga capacidad suficiente." : undefined}>{(x, a) => <Entrada id={x} type="number" min={1} aria-describedby={a} {...register("asistentes")} />}</Campo>
    </GrupoCampos>
    <details className={css.opcionales} open={!breve}><summary>Más datos de la negociación</summary><div className={css.contenidoOpcional}>
    <GrupoCampos titulo="Datos opcionales">
      {!editar && <Campo etiqueta="Responsable" error={errors.responsableId?.message}>{(x, a) => <Selector id={x} aria-describedby={a} {...register("responsableId")}><option value="">Hereda del cliente: {nombreHeredado}</option>{usuarios.data?.filter(u => sesion?.usuario.rol !== "SELLER" || u.id === sesion.usuario.id).map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}</Selector>}</Campo>}
      <Campo etiqueta="Origen">{x => <SelectorCatalogo id={x} catalogo="origenes" valor={valores.origenId} alCambiar={v => setValue("origenId", v, { shouldDirty: true })} soloActivos />}</Campo>
      <Campo etiqueta="Servicios" ancho>{x => <SelectorServicios id={x} valores={valores.servicios ?? []} alCambiar={v => setValue("servicios", v, { shouldDirty: true })} />}</Campo>
      <Campo etiqueta="Valor estimado" error={errors.valor?.message}>{(x, a) => <Entrada id={x} type="number" min={0} aria-describedby={a} {...register("valor")} />}</Campo>
      <Campo etiqueta="Probabilidad" error={errors.probabilidad?.message}>{(x, a) => <Entrada id={x} type="number" min={0} max={100} aria-describedby={a} {...register("probabilidad")} />}</Campo>
      <Campo etiqueta="Cierre estimado">{x => <Entrada id={x} type="date" {...register("cierre")} />}</Campo>
      <Campo etiqueta="Observaciones" ancho error={errors.observaciones?.message}>{(x, a) => <AreaTexto id={x} aria-describedby={a} {...register("observaciones")} />}</Campo>
    </GrupoCampos>{usuarios.isError && <p role="alert">No pudimos cargar los responsables. <Boton type="button" variante="secundario" onClick={() => void usuarios.refetch()}>Reintentar</Boton></p>}
    </div></details>
    </TarjetaCuerpo><TarjetaPie><div className={css.acciones}><BotonEnlace a={cancelar} variante="secundario">Cancelar</BotonEnlace><Boton type="submit" disabled={bloqueado} icono="check">{isSubmitting || guardar.isPending ? "Guardando…" : editar ? "Guardar cambios" : "Crear oportunidad"}</Boton></div></TarjetaPie></Tarjeta></form></>;
}
