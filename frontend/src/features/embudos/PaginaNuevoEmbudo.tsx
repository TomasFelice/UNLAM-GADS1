import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import { useOperacionesEmbudo, usePlantillasEmbudo } from "./apiEmbudos";
import css from "./Embudos.module.css";
export function PaginaNuevoEmbudo() {
  const [nombre, setNombre] = useState(""); const [plantilla, setPlantilla] = useState("");
  const consulta = usePlantillasEmbudo(); const { crear } = useOperacionesEmbudo(); const navegar = useNavigate();
  const elegida = consulta.data?.find(p => p.key === plantilla);
  return <><Seo titulo="Nuevo embudo" descripcion="Creá un proceso comercial propio." noIndexar /><CabeceraPagina titulo="Nuevo embudo" descripcion="Comenzá vacío o copiá una plantilla. Las etapas copiadas se pueden configurar de forma independiente." migas={[{ texto: "Embudos", a: "/app/configuracion/embudos" }, { texto: "Nuevo" }]} />
    <form onSubmit={async e => { e.preventDefault(); try { const creado = await crear.mutateAsync({ name: nombre.trim(), templateKey: plantilla || undefined }); navegar(`/app/configuracion/embudos/${creado.id}`, { state: { embudoCreado: true } }); } catch { /* La mutación conserva los datos y muestra el error. */ } }}><Tarjeta><TarjetaCuerpo><GrupoCampos titulo="Proceso comercial">
    <Campo etiqueta="Nombre" requerido>{x => <Entrada id={x} value={nombre} required onChange={e => setNombre(e.target.value)} />}</Campo><Campo etiqueta="Plantilla">{x => <Selector id={x} value={plantilla} onChange={e => setPlantilla(e.target.value)}><option value="">Vacío</option>{consulta.data?.map(p => <option key={p.key} value={p.key}>{p.name}</option>)}</Selector>}</Campo></GrupoCampos>
    {consulta.isPending && <p role="status">Consultando plantillas…</p>}{consulta.isError && <p role="alert">No pudimos consultar las plantillas. <Boton type="button" variante="secundario" onClick={() => void consulta.refetch()}>Reintentar</Boton></p>}
    {elegida && <ol className={css.etapas}>{elegida.stages?.map((e, i) => <li key={i}>{e.name} · {e.kind === "OPEN" ? "Abierta" : e.kind === "WON" ? "Ganada" : "Perdida"}</li>)}</ol>}
    <p>El embudo se crea inactivo. Para activarlo necesita al menos una etapa activa abierta, una ganada y una perdida.</p>{crear.isError && <p role="alert">{crear.error.message}</p>}
    </TarjetaCuerpo><TarjetaPie><BotonEnlace a="/app/configuracion/embudos" variante="secundario">Cancelar</BotonEnlace><Boton type="submit" disabled={!nombre.trim() || crear.isPending}>Crear embudo</Boton></TarjetaPie></Tarjeta></form></>;
}
