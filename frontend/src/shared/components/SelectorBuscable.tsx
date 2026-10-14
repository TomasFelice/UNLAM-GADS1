import { useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from "react";
import { createPortal } from "react-dom";
import { Icono } from "./Icono";
import css from "./SelectorBuscable.module.css";

export type OpcionSelector = { valor: string; texto: string };
type Props = {
  id?: string; nombre: string; descripcion?: string; marcador: string;
  opciones: OpcionSelector[]; seleccionados: OpcionSelector[];
  alCambiar: (valor: string) => void; multiple?: boolean;
  busqueda: string; alBuscar: (valor: string) => void;
  abierto: boolean; alAbrir: (abierto: boolean) => void;
  cargando: boolean; cargandoMas: boolean; hayMas: boolean;
  verMas: () => Promise<unknown> | void; error?: boolean; reintentar: () => void;
};

export function SelectorBuscable({ id, nombre, descripcion, marcador, opciones, seleccionados, alCambiar, multiple,
  busqueda, alBuscar, abierto, alAbrir, cargando, cargandoMas, hayMas, verMas, error, reintentar }: Props) {
  const propioId = useId(); const controlId = id ?? propioId;
  const listaId = `${propioId}-opciones`;
  const raiz = useRef<HTMLDivElement>(null); const panel = useRef<HTMLDivElement>(null);
  const entrada = useRef<HTMLInputElement>(null);
  const accionFinal = useRef<HTMLButtonElement>(null);
  const [focoPendiente, setFocoPendiente] = useState<number>();
  const [activo, setActivo] = useState<string>();
  const [posicion, setPosicion] = useState<CSSProperties>({});
  const cerrar = () => { alAbrir(false); alBuscar(""); setActivo(undefined); setFocoPendiente(undefined); };
  const abrir = () => { alAbrir(true); setActivo(undefined); };
  const elegir = (valor: string) => { alCambiar(valor); entrada.current?.focus(); if (!multiple) cerrar(); };
  // La opción vacía también permite limpiar filtros y asociaciones opcionales.
  const unicas = opciones.filter((o, i) => opciones.findIndex(v => v.valor === o.valor) === i);
  const visibles = multiple ? unicas : [{ valor: "", texto: marcador }, ...unicas];
  const valorActivo = activo ?? (focoPendiente === undefined ? undefined : unicas[focoPendiente]?.valor);
  const indiceActivo = visibles.findIndex(o => o.valor === valorActivo);
  const texto = multiple ? (seleccionados.length ? `${seleccionados.length} ${seleccionados.length === 1 ? "servicio seleccionado" : "servicios seleccionados"}` : "") : seleccionados[0]?.texto ?? "";

  useLayoutEffect(() => {
    if (!abierto) return;
    const ubicar = () => {
      const rect = entrada.current!.getBoundingClientRect();
      const medidas = getComputedStyle(document.documentElement);
      const unidad = parseFloat(medidas.fontSize) || 0;
      const margen = (parseFloat(medidas.getPropertyValue("--e-2")) || 0) * unidad;
      const altoDeseado = (parseFloat(medidas.getPropertyValue("--e-20")) || 0) * unidad * 3;
      const debajo = window.innerHeight - rect.bottom - margen;
      const encima = rect.top - margen;
      const arriba = debajo < altoDeseado && encima > debajo;
      setPosicion({ left: Math.max(margen, rect.left), width: Math.min(rect.width, window.innerWidth - margen * 2),
        top: arriba ? undefined : rect.bottom + margen,
        bottom: arriba ? window.innerHeight - rect.top + margen : undefined,
        maxHeight: Math.max(0, (arriba ? encima : debajo) - margen) });
    };
    ubicar(); window.addEventListener("resize", ubicar); window.addEventListener("scroll", ubicar, true);
    return () => { window.removeEventListener("resize", ubicar); window.removeEventListener("scroll", ubicar, true); };
  }, [abierto]);

  useEffect(() => {
    if (!abierto) return;
    const salir = (evento: Event) => {
      if (evento.target instanceof Node && !raiz.current?.contains(evento.target) && !panel.current?.contains(evento.target)) {
        alAbrir(false); alBuscar(""); setActivo(undefined); setFocoPendiente(undefined);
      }
    };
    document.addEventListener("pointerdown", salir); document.addEventListener("focusin", salir);
    return () => { document.removeEventListener("pointerdown", salir); document.removeEventListener("focusin", salir); };
  }, [abierto, alAbrir, alBuscar]);

  useEffect(() => {
    if (abierto && indiceActivo >= 0) document.getElementById(`${listaId}-${indiceActivo}`)?.scrollIntoView?.({ block: "nearest" });
  }, [abierto, indiceActivo, listaId]);

  useEffect(() => {
    if (focoPendiente === undefined) return;
    if (abierto && opciones.length <= focoPendiente && hayMas && !error) return;
    if (abierto) entrada.current?.focus();
  }, [abierto, focoPendiente, opciones.length, hayMas, error]);

  const cargarMas = async () => {
    const cantidadAnterior = unicas.length;
    const teniaFoco = document.activeElement === accionFinal.current;
    await verMas();
    if (teniaFoco && panel.current) { setActivo(undefined); setFocoPendiente(cantidadAnterior); }
  };

  const teclado = (evento: KeyboardEvent) => {
    if (evento.key === "Escape" && abierto) { evento.preventDefault(); evento.stopPropagation(); entrada.current?.focus(); cerrar(); }
    if (evento.key === "ArrowDown" || evento.key === "ArrowUp") {
      evento.preventDefault(); if (!abierto) abrir();
      if (evento.key === "ArrowDown" && abierto && indiceActivo === visibles.length - 1 && (hayMas || error)) {
        accionFinal.current?.focus(); return;
      }
      const siguiente = evento.key === "ArrowDown" ? Math.min(indiceActivo + 1, visibles.length - 1) : indiceActivo < 0 ? visibles.length - 1 : Math.max(0, indiceActivo - 1);
      setFocoPendiente(undefined); setActivo(visibles[siguiente]?.valor);
    }
    if (evento.key === "Enter" && abierto) { evento.preventDefault(); if (indiceActivo >= 0) elegir(visibles[indiceActivo].valor); }
  };

  return <div ref={raiz} className={css.raiz}>
    <div className={css.campo}>
      <input ref={entrada} id={controlId} role="combobox" aria-label={id ? undefined : nombre}
        aria-describedby={descripcion} aria-expanded={abierto} aria-controls={abierto ? listaId : undefined}
        aria-autocomplete="list" aria-activedescendant={abierto && indiceActivo >= 0 ? `${listaId}-${indiceActivo}` : undefined}
        autoComplete="off" placeholder={abierto ? `Buscar ${nombre.toLowerCase()}…` : marcador} value={abierto ? busqueda : texto} data-valor={seleccionados.map(o => o.valor).join(",")}
        onFocus={abrir} onClick={() => { if (!abierto) abrir(); }} onKeyDown={teclado}
        onChange={e => { alBuscar(e.target.value); alAbrir(true); setActivo(undefined); setFocoPendiente(undefined); }} />
      <Icono nombre={abierto ? "buscar" : "chevronAbajo"} className={css.indicador} />
    </div>
    {multiple && seleccionados.length > 0 && <ul className={css.seleccionados} aria-label="Servicios seleccionados">
      {seleccionados.map(o => <li key={o.valor}><span>{o.texto}</span><button type="button" aria-label={`Quitar ${o.texto}`} onClick={() => alCambiar(o.valor)}><Icono nombre="cerrar" tamano={14} /></button></li>)}
    </ul>}
    {!abierto && error && <div className={css.estado} role="alert">No pudimos consultar las opciones. <button type="button" className={css.accion} onClick={reintentar}>Reintentar</button></div>}
    {abierto && createPortal(<div ref={panel} className={css.panel} style={posicion} onKeyDown={e => {
      if (e.key === "Escape") teclado(e);
      if (e.key === "ArrowUp") { e.preventDefault(); entrada.current?.focus(); setActivo(visibles.at(-1)?.valor); }
    }}>
      <div className={css.lista} id={listaId} role="listbox" aria-label={nombre} aria-multiselectable={multiple || undefined} aria-busy={cargando || cargandoMas}>
        {!cargando && visibles.map((o, i) => <div key={o.valor} id={`${listaId}-${i}`} role="option" data-valor={o.valor}
          aria-selected={o.valor === "" ? !seleccionados.length : seleccionados.some(s => s.valor === o.valor)}
          className={`${css.opcion} ${valorActivo === o.valor ? css.activa : ""}`}
          onPointerMove={() => { setActivo(o.valor); setFocoPendiente(undefined); }} onMouseDown={e => e.preventDefault()} onClick={() => elegir(o.valor)}>
          <span>{o.texto}</span>{seleccionados.some(s => s.valor === o.valor) && <Icono nombre="check" tamano={16} />}
        </div>)}
      </div>
      {cargando ? <p className={css.estado} role="status">Buscando opciones…</p> : !unicas.length && !error && <p className={css.estado} role="status">Sin resultados para esta búsqueda.</p>}
      {error && <div className={css.estado} role="alert">No pudimos consultar las opciones. <button ref={accionFinal} type="button" className={css.accion} onClick={reintentar}>Reintentar</button></div>}
      {!error && hayMas && <button ref={accionFinal} type="button" className={css.mas} disabled={cargando || cargandoMas} onClick={cargarMas}>{cargandoMas ? "Cargando más…" : "Ver más resultados"}</button>}
    </div>, document.body)}
  </div>;
}
