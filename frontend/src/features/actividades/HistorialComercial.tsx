import { useRef, useState } from "react";
import { AvisoExito } from "../../shared/components/AvisoExito";
import { PieListado } from "../../shared/components/BarraFiltros";
import { Boton } from "../../shared/components/Boton";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { FormularioActividad } from "./FormularioActividad";
import { LineaDeActividades } from "./LineaDeActividades";
import { useActividades, type Asociacion } from "./apiActividades";
import css from "./HistorialComercial.module.css";

export function HistorialComercial({ asociacion, puedeRegistrar }: { asociacion: Asociacion; puedeRegistrar: boolean }) {
  const [pagina, setPagina] = useState(0);
  const [formulario, setFormulario] = useState(false);
  const [registrada, setRegistrada] = useState(false);
  const boton = useRef<HTMLDivElement>(null);
  const actividades = useActividades(asociacion, 20, pagina);
  const cerrar = () => { setFormulario(false); requestAnimationFrame(() => boton.current?.querySelector("button")?.focus()); };
  return <section aria-label="Historial comercial"><Tarjeta>
    <TarjetaCabecera titulo="Historial comercial" descripcion="Interacciones realizadas con el cliente" acciones={puedeRegistrar && !formulario && <div ref={boton}><Boton variante="secundario" icono="mas" onClick={() => { setRegistrada(false); setFormulario(true); }}>Registrar actividad</Boton></div>} />
    <TarjetaCuerpo>
      {registrada && <AvisoExito>Actividad registrada.</AvisoExito>}
      {formulario ? <FormularioActividad asociacion={asociacion} alCancelar={cerrar} alGuardar={() => { setPagina(0); setRegistrada(true); cerrar(); }} /> : <>
        <LineaDeActividades actividades={actividades.data?.content ?? []} cargando={actividades.isPending} error={actividades.isError ? actividades.error.message : undefined} alReintentar={() => actividades.refetch()} />
        {actividades.data && !actividades.isError && <div className={css.pie}><PieListado entidad="actividades" mostrados={actividades.data.content?.length ?? 0} total={actividades.data.totalElements ?? 0} pagina={pagina} totalPaginas={actividades.data.totalPages ?? 1} alCambiarPagina={setPagina} /></div>}
      </>}
    </TarjetaCuerpo>
  </Tarjeta></section>;
}
