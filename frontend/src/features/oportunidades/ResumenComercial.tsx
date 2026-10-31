import { useEffect, useState } from "react";
import { Boton } from "../../shared/components/Boton";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ErrorDeApi } from "../../shared/api/errores";
import { useResumenComercial } from "./apiResumenComercial";
import css from "./ResumenComercial.module.css";

const SECCIONES = [
  ["clientNeeds", "Necesidad del cliente"], ["currentSituation", "Situación actual"],
  ["agreementsAndObjections", "Acuerdos y objeciones"], ["unconfirmedInformation", "Información pendiente o contradictoria"],
] as const;
export function ResumenComercial({ oportunidadId, version, autorizado }: { oportunidadId: number; version: number; autorizado: boolean }) {
  const consulta = useResumenComercial(oportunidadId, version, autorizado);
  const [ahora, fijarAhora] = useState(Date.now);
  const error = consulta.error instanceof ErrorDeApi ? consulta.error.problema : undefined;
  const espera = (error?.retryAfterSeconds ?? 0) * 1000;
  const habilitarEn = consulta.errorUpdatedAt + espera;
  useEffect(() => {
    if (!consulta.isError || !espera) return;
    const temporizador = setTimeout(() => fijarAhora(Date.now()), Math.max(0, habilitarEn - Date.now()));
    return () => clearTimeout(temporizador);
  }, [consulta.isError, espera, habilitarEn]);
  const recuperable = !error || [409, 429, 502, 503, 504].includes(error.status) && error.code !== "AI_NOT_CONFIGURED";
  const resultado = consulta.data;
  return <section aria-label="Resumen comercial" className={css.panel}>
    <Tarjeta><TarjetaCabecera titulo="Resumen comercial" /><TarjetaCuerpo>
      {consulta.contextoPendiente ? <p role="status">Validando acceso a la oportunidad…</p> : consulta.isPending || consulta.isFetching ?
        <p role="status">Preparando el resumen comercial… Podés seguir consultando la oportunidad.</p> : consulta.isError ?
          <div role="status" className={css.estado}>
            <p>{consulta.error.message}</p>
            {error?.status === 429 && <p>{espera ? `Esperá ${Math.ceil(espera / 1000)} segundos antes de reintentar.` : "Intentá más tarde."}</p>}
            {recuperable && <Boton variante="secundario" disabled={consulta.isFetching || espera > 0 && ahora < habilitarEn}
              onClick={() => { if (!consulta.isFetching && Date.now() >= habilitarEn) void consulta.refetch(); }}>Reintentar</Boton>}
          </div> : resultado && <div className={css.contenido}>
            <p className={css.aviso}>Generado con IA. Revisá la información y sus fuentes.</p>
            {SECCIONES.map(([clave, titulo]) => <section key={clave} className={css.seccion}>
              <h3>{titulo}</h3>
              {!resultado.sections?.[clave]?.length ? <p className={css.sinInformacion}>No consta en la información analizada.</p> :
                <ul className={css.lista}>{resultado.sections[clave]?.map((afirmacion, indice) => <li key={indice}>
                  <p className={css.texto}>{afirmacion.text}</p>
                </li>)}</ul>}
            </section>)}
          </div>}
    </TarjetaCuerpo></Tarjeta>
  </section>;
}
