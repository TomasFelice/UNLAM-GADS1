import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { AvisoExito } from "../../shared/components/AvisoExito";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import pantalla from "../../shared/styles/pantalla.module.css";
import { useEtapas } from "../catalogos/apiCatalogos";
import { useConfiguracionEmbudo, useOperacionesEmbudo, type EtapaEmbudo } from "./apiEmbudos";
import css from "./Embudos.module.css";
const tipos = { OPEN: "Abierta", WON: "Ganada", LOST: "Perdida" } as const;
export function PaginaEmbudoConfiguracion() {
  const ubicacion = useLocation(); const navegar = useNavigate();
  const recienCreado = (ubicacion.state as { embudoCreado?: boolean } | null)?.embudoCreado === true;
  const id = Number(useParams().id); const valido = Number.isSafeInteger(id) && id > 0;
  const consulta = useConfiguracionEmbudo(id); const etapas = useEtapas(valido ? id : undefined, valido);
  const { guardar, predeterminar, guardarEtapa, ordenar } = useOperacionesEmbudo();
  const [nombre, setNombre] = useState(""); const [activo, setActivo] = useState("false");
  const [edicion, setEdicion] = useState<EtapaEmbudo>(); const [nombreEtapa, setNombreEtapa] = useState("");
  const [tipo, setTipo] = useState<keyof typeof tipos>("OPEN"); const [activaEtapa, setActivaEtapa] = useState("true");
  const [mensaje, setMensaje] = useState(recienCreado ? "Embudo creado." : ""); const [error, setError] = useState("");
  useEffect(() => {
    if (recienCreado) void navegar(`${ubicacion.pathname}${ubicacion.search}`, { replace: true, state: null });
  }, [recienCreado, navegar, ubicacion.pathname, ubicacion.search]);
  const [cargado, setCargado] = useState(consulta.data);
  if (consulta.data && consulta.data !== cargado) { setCargado(consulta.data); setNombre(consulta.data.name ?? ""); setActivo(String(consulta.data.active)); }
  const pendiente = guardar.isPending || predeterminar.isPending || guardarEtapa.isPending || ordenar.isPending;
  const operar = async (accion: () => Promise<unknown>, exito: string) => { setError(""); setMensaje(""); try { await accion(); setMensaje(exito); } catch (fallo) { setError(fallo instanceof Error ? fallo.message : "No se pudo guardar el cambio."); } };
  const editarEtapa = (e?: EtapaEmbudo) => { setEdicion(e); setNombreEtapa(e?.name ?? ""); setTipo(e?.kind ?? "OPEN"); setActivaEtapa(String(e?.active ?? true)); setError(""); };
  const mover = (indice: number, direccion: number) => {
    const ids = (etapas.data ?? []).map(e => e.id!); const destino = indice + direccion;
    [ids[indice], ids[destino]] = [ids[destino], ids[indice]];
    void operar(() => ordenar.mutateAsync({ id, stageIds: ids }), "Orden de etapas guardado.");
  };
  const seo = <Seo titulo="Configuración del embudo" descripcion="Etapas y opciones del proceso comercial." noIndexar />;
  if (!valido || consulta.isError) return <>{seo}<EstadoVacio icono="alerta" titulo="No pudimos consultar el embudo" descripcion={consulta.error?.message ?? "El enlace no es válido."} accion={<><BotonEnlace a="/app/configuracion/embudos">Volver</BotonEnlace>{valido && <Boton onClick={() => void consulta.refetch()}>Reintentar</Boton>}</>} /></>;
  if (consulta.isPending) return <>{seo}<EstadoVacio icono="embudo" titulo="Cargando embudo…" descripcion="Consultando su configuración." /></>;
  const embudo = consulta.data;
  return <>{seo}<CabeceraPagina titulo={embudo.name ?? "Embudo"} descripcion={`${embudo.active ? "Activo" : "Inactivo"}${embudo.defaultPipeline ? " · Predeterminado" : ""}`} migas={[{ texto: "Embudos", a: "/app/configuracion/embudos" }, { texto: embudo.name ?? "Detalle" }]} acciones={<BotonEnlace a={`/app/embudo?pipelineId=${id}`} variante="secundario">Ver oportunidades</BotonEnlace>} />
    {error && <p role="alert">{error}</p>}{mensaje && <AvisoExito>{mensaje}</AvisoExito>}
    <div className={pantalla.pila}><form onSubmit={e => { e.preventDefault(); void operar(() => guardar.mutateAsync({ id, name: nombre.trim(), active: activo === "true" }), "Embudo guardado."); }}><Tarjeta><TarjetaCuerpo><GrupoCampos titulo="Configuración general">
    <Campo etiqueta="Nombre del embudo" requerido>{x => <Entrada id={x} value={nombre} required onChange={e => setNombre(e.target.value)} />}</Campo>
    <Campo etiqueta="Estado del embudo" ayuda="Para activarlo necesita etapas activas abiertas, ganadas y perdidas.">{x => <Selector id={x} value={activo} onChange={e => setActivo(e.target.value)}><option value="true">Activo</option><option value="false">Inactivo</option></Selector>}</Campo>
    </GrupoCampos></TarjetaCuerpo><TarjetaPie><Boton type="submit" disabled={!nombre.trim() || pendiente}>Guardar embudo</Boton>{!embudo.defaultPipeline && <Boton type="button" variante="secundario" disabled={!embudo.active || pendiente} onClick={() => void operar(() => predeterminar.mutateAsync(id), "Embudo predeterminado actualizado.")}>Usar como predeterminado</Boton>}</TarjetaPie></Tarjeta></form>
    <Tarjeta><TarjetaCabecera titulo="Etapas del embudo" descripcion="El orden incluye etapas activas e inactivas. Las etapas con historia conservan su tipo y su embudo." />
    {etapas.isPending ? <TarjetaCuerpo><p role="status">Cargando etapas…</p></TarjetaCuerpo> : etapas.isError ? <TarjetaCuerpo><p role="alert">{etapas.error.message}</p><Boton onClick={() => void etapas.refetch()}>Reintentar etapas</Boton></TarjetaCuerpo> : <ol className={css.lista}>{etapas.data?.map((etapa, indice) => <li key={etapa.id} className={css.etapa}><span>{indice + 1}. {etapa.name} · {tipos[etapa.kind ?? "OPEN"]} · {etapa.active ? "Activa" : "Inactiva"}</span><span className={css.acciones}><Boton type="button" variante="secundario" disabled={pendiente || indice === 0} aria-label={`Subir ${etapa.name}`} onClick={() => mover(indice, -1)}>Subir</Boton><Boton type="button" variante="secundario" disabled={pendiente || indice === (etapas.data?.length ?? 0) - 1} aria-label={`Bajar ${etapa.name}`} onClick={() => mover(indice, 1)}>Bajar</Boton><Boton type="button" variante="secundario" disabled={pendiente} aria-label={`Editar ${etapa.name}`} onClick={() => editarEtapa(etapa)}>Editar</Boton></span></li>)}</ol>}
    </Tarjeta>
    <form onSubmit={e => { e.preventDefault(); void operar(async () => { await guardarEtapa.mutateAsync({ id: edicion?.id, pipelineId: id, name: nombreEtapa.trim(), kind: tipo, active: activaEtapa === "true" }); editarEtapa(); }, edicion ? "Etapa guardada." : "Etapa creada."); }}><Tarjeta><TarjetaCuerpo><GrupoCampos titulo={edicion ? `Editar etapa: ${edicion.name}` : "Nueva etapa"}>
    <Campo etiqueta="Nombre de la etapa" requerido>{x => <Entrada id={x} value={nombreEtapa} required onChange={e => setNombreEtapa(e.target.value)} />}</Campo><Campo etiqueta="Tipo de etapa">{x => <Selector id={x} value={tipo} onChange={e => setTipo(e.target.value as keyof typeof tipos)}>{Object.entries(tipos).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}</Selector>}</Campo><Campo etiqueta="Estado de la etapa">{x => <Selector id={x} value={activaEtapa} onChange={e => setActivaEtapa(e.target.value)}><option value="true">Activa</option><option value="false">Inactiva</option></Selector>}</Campo>
    </GrupoCampos></TarjetaCuerpo><TarjetaPie>{edicion && <Boton type="button" variante="secundario" onClick={() => editarEtapa()}>Cancelar edición de etapa</Boton>}<Boton type="submit" disabled={!nombreEtapa.trim() || pendiente}>{edicion ? "Guardar etapa" : "Crear etapa"}</Boton></TarjetaPie></Tarjeta></form>
    </div></>;
}
