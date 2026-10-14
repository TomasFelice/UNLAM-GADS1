import { BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { Tarjeta, TarjetaCabecera, TarjetaPie } from "../../shared/components/Tarjeta";
import { Seo } from "../../shared/seo/Seo";
import pantalla from "../../shared/styles/pantalla.module.css";
import { CATALOGOS, type Catalogo } from "./apiCatalogos";

export function PaginaConfiguracion() {
  return <><Seo titulo="Configuración comercial" descripcion="Catálogos de la organización." noIndexar />
    <CabeceraPagina titulo="Configuración comercial" descripcion="Mantené las opciones que utiliza tu equipo, conservando la historia comercial." />
    <div className={pantalla.pila}><Tarjeta><TarjetaCabecera titulo="Embudos comerciales" descripcion="Procesos con etapas propias, plantillas y un embudo predeterminado." /><TarjetaPie><BotonEnlace a="/app/configuracion/embudos" variante="secundario">Administrar embudos comerciales</BotonEnlace></TarjetaPie></Tarjeta>{(Object.keys(CATALOGOS) as Catalogo[]).map(clave => <Tarjeta key={clave}>
      <TarjetaCabecera titulo={CATALOGOS[clave].titulo} descripcion={CATALOGOS[clave].descripcion} />
      <TarjetaPie><BotonEnlace a={`/app/configuracion/${clave}`} variante="secundario">Administrar {CATALOGOS[clave].titulo.toLowerCase()}</BotonEnlace></TarjetaPie>
    </Tarjeta>)}</div></>;
}
