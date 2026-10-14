import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import pantalla from "../../shared/styles/pantalla.module.css";
import { LineaDeActividades } from "../actividades/LineaDeActividades";
import { useActividades } from "../actividades/apiActividades";
import { FichaOportunidad } from "../oportunidades/FichaOportunidad";
import { useOportunidadesRecientes } from "../oportunidades/apiOportunidades";
import css from "./PaginaPanel.module.css";

export function PaginaPanel() {
  const recientes = useOportunidadesRecientes();
  const actividades = useActividades(undefined, 8);
  return <>
    <Seo titulo="Panel" descripcion="Actividad comercial reciente." noIndexar />
    <CabeceraPagina titulo="Panel comercial" descripcion="Retomá tus negociaciones y consultá las últimas interacciones."
      acciones={<><BotonEnlace a="/app/oportunidades/nueva" icono="mas">Nueva oportunidad</BotonEnlace><BotonEnlace a="/app/embudo" variante="secundario">Ver embudo</BotonEnlace></>} />
    <div className={pantalla.pila}>
      <div className={css.dosColumnas}>
        <section aria-label="Oportunidades recientes">
          <Tarjeta aSangre><TarjetaCabecera titulo="Oportunidades recientes" />
            {recientes.isPending ? <EstadoVacio icono="oportunidades" titulo="Cargando oportunidades…" descripcion="Buscando las últimas negociaciones." />
              : recientes.isError ? <EstadoVacio icono="alerta" titulo="No pudimos cargar las oportunidades" descripcion={recientes.error.message}
                accion={<Boton variante="secundario" onClick={() => void recientes.refetch()}>Reintentar oportunidades</Boton>} />
              : recientes.data.content?.length ? <ul className={css.listaFichas}>{recientes.data.content.map(o => <li key={o.id}><FichaOportunidad oportunidad={o} /></li>)}</ul>
              : <EstadoVacio icono="oportunidades" titulo="Todavía no hay oportunidades" descripcion="Creá una negociación para comenzar." />}
          </Tarjeta>
        </section>
        <section aria-label="Actividad reciente">
          <Tarjeta><TarjetaCabecera titulo="Actividad reciente" /><TarjetaCuerpo>
            <LineaDeActividades actividades={actividades.data?.content ?? []} cargando={actividades.isPending} error={actividades.isError ? actividades.error.message : undefined} />
            {actividades.isError && <Boton variante="secundario" onClick={() => void actividades.refetch()}>Reintentar actividades</Boton>}
          </TarjetaCuerpo></Tarjeta>
        </section>
      </div>
    </div>
  </>;
}
