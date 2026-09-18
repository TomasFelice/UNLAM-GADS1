import { Link } from "react-router-dom";
import { Avatar } from "../../shared/components/Avatar";
import { BarraFiltros, Filtro, PieListado } from "../../shared/components/BarraFiltros";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { Tarjeta, TarjetaPie } from "../../shared/components/Tarjeta";
import { EncabezadoTabla, Tabla } from "../../shared/components/Tabla";
import { ESTADO_CLIENTE } from "../../shared/data/formato";
import { useBusquedaUrl } from "../../shared/hooks/useBusquedaUrl";
import { Seo } from "../../shared/seo/Seo";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useOrigenes } from "../catalogos/apiCatalogos";
import { useEmpresas } from "../empresas/apiEmpresas";
import { useContactos } from "./apiContactos";

export function PaginaContactos() {
  const url = useBusquedaUrl();
  const filtros = { q:url.parametros.get("q")||undefined, status:url.parametros.get("status")||undefined, originId:url.parametros.get("originId")||undefined, companyId:url.parametros.get("companyId")||undefined, salesRepId:url.parametros.get("salesRepId")||undefined };
  const consulta=useContactos(url.pagina,filtros); const empresas=useEmpresas(0); const usuarios=useUsuariosAsignables(); const origenes=useOrigenes();
  return <><Seo titulo="Contactos" descripcion="Listado de contactos del CRM." noIndexar/><CabeceraPagina titulo="Contactos" descripcion="Personas con las que negociás, asociadas a una empresa o como cliente individual." acciones={<BotonEnlace a="/app/contactos/nuevo" icono="mas">Nuevo contacto</BotonEnlace>}/>
    <Tarjeta aSangre><BarraFiltros marcador="Buscar por nombre, correo o empresa" valor={url.busqueda} alCambiar={url.setBusqueda}>
      <Filtro etiqueta="Estado" valor={filtros.status} alCambiar={v=>url.cambiar("status",v)} opciones={Object.entries(ESTADO_CLIENTE).map(([valor,x])=>({valor,texto:x.texto}))}/>
      <Filtro etiqueta="Origen" valor={filtros.originId} alCambiar={v=>url.cambiar("originId",v)} opciones={(origenes.data??[]).map(o=>({valor:String(o.id),texto:o.name??"Origen"}))}/>
      <Filtro etiqueta="Empresa" valor={filtros.companyId} alCambiar={v=>url.cambiar("companyId",v)} opciones={(empresas.data?.content??[]).map(e=>({valor:String(e.id),texto:e.businessName??e.legalName??"Empresa"}))}/>
      <Filtro etiqueta="Responsable" valor={filtros.salesRepId} alCambiar={v=>url.cambiar("salesRepId",v)} opciones={(usuarios.data??[]).map(u=>({valor:String(u.id),texto:`${u.firstName} ${u.lastName}`}))}/>
    </BarraFiltros>
    {consulta.isPending?<EstadoVacio icono="contactos" titulo="Cargando contactos…" descripcion="Consultando la cartera comercial."/>:consulta.isError?<EstadoVacio icono="alerta" titulo="No pudimos cargar los contactos" descripcion={consulta.error.message} accion={<Boton variante="secundario" onClick={()=>consulta.refetch()}>Reintentar</Boton>}/>:!(consulta.data.content?.length)?<EstadoVacio icono="contactos" titulo="No hay contactos para mostrar" descripcion="Probá cambiar los filtros o registrá el primer contacto."/>:<><Tabla resumen="Listado de contactos"><EncabezadoTabla columnas={["Contacto","Empresa","Teléfono","Estado"]}/><tbody>{consulta.data.content.map(c=>{const estado=ESTADO_CLIENTE[c.status??"POTENCIAL"];return <tr key={c.id}><td><span className="persona"><Avatar iniciales={`${c.firstName?.[0]??""}${c.lastName?.[0]??""}`} tamano="sm"/><span><Link to={`/app/contactos/${c.id}`}>{c.firstName} {c.lastName}</Link><span className="secundario">{c.email||"Sin correo"}</span></span></span></td><td>{c.companyId?<Link to={`/app/empresas/${c.companyId}`}>{c.companyName}</Link>:<Etiqueta tono="neutro" tamano="sm">Cliente individual</Etiqueta>}</td><td>{c.phone||"—"}</td><td><Etiqueta tono={estado.tono} tamano="sm" punto>{estado.texto}</Etiqueta></td></tr>})}</tbody></Tabla><TarjetaPie><PieListado mostrados={consulta.data.content.length} total={consulta.data.totalElements??0} entidad="contactos" pagina={url.pagina} totalPaginas={consulta.data.totalPages??1} alCambiarPagina={url.cambiarPagina}/></TarjetaPie></>}
    </Tarjeta></>;
}
