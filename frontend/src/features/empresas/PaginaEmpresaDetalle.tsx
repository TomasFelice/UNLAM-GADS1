import { useParams } from "react-router-dom";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { Icono } from "../../shared/components/Icono";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ESTADO_CLIENTE } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import { usePuedeEditarCliente } from "../../shared/sesion/usePuedeEditarCliente";
import pantalla from "../../shared/styles/pantalla.module.css";
import { useEmpresa } from "./apiEmpresas";

import { HistorialComercial } from "../actividades/HistorialComercial";
import { ContactosEmpresa, OportunidadesCliente } from "../actividades/RelacionesCliente";

import { NombreCatalogo } from "../catalogos/NombreCatalogo";

export function PaginaEmpresaDetalle() {
  const parametros = useParams();
  const id = Number(parametros.id);
  const idValido = Number.isSafeInteger(id) && id > 0;
  const consulta = useEmpresa(idValido ? id : undefined);
  const puedeEditar = usePuedeEditarCliente(consulta.data?.salesRepId);


  if (!idValido) {
    return <><Seo titulo="Empresa no encontrada" descripcion="" noIndexar /><EstadoVacio icono="empresas" titulo="No encontramos esa empresa" descripcion="El enlace no es válido." /></>;
  }

  if (consulta.isPending) {
    return <><Seo titulo="Empresa" descripcion="" noIndexar /><EstadoVacio icono="empresas" titulo="Cargando empresa…" descripcion="Estamos consultando sus datos y contactos." /></>;
  }

  if (consulta.isError || !consulta.data) {
    return (
      <>
        <Seo titulo="Empresa no encontrada" descripcion="" noIndexar />
        <EstadoVacio
          icono="empresas"
          titulo="No encontramos esa empresa"
          descripcion={consulta.error?.message ?? "El enlace no es válido."}
          accion={<Boton variante="secundario" onClick={() => consulta.refetch()}>Reintentar</Boton>}
        />
      </>
    );
  }

  const empresa = consulta.data;
  const estado = ESTADO_CLIENTE[empresa.status ?? "POTENCIAL"];
  const nombreResponsable = empresa.salesRepName ?? "—";
  const urlSitio = empresa.website
    ? (empresa.website.startsWith("http") ? empresa.website : `https://${empresa.website}`)
    : undefined;

  return (
    <>
      <Seo titulo={empresa.legalName ?? "Empresa"} descripcion="" noIndexar />
      <CabeceraPagina
        migas={[{ texto: "Empresas", a: "/app/empresas" }, { texto: empresa.businessName ?? empresa.legalName ?? "Detalle" }]}
        titulo={empresa.businessName ?? empresa.legalName ?? "Empresa"}
        descripcion={empresa.legalName}
        meta={
          <>
            <Etiqueta tono={estado.tono} punto tamano="sm">{estado.texto}</Etiqueta>
            {empresa.industry && <span><Icono nombre="capas" tamano={14} /> {empresa.industry}</span>}
            {empresa.locality && <span><Icono nombre="ubicacion" tamano={14} /> {empresa.locality}</span>}
          </>
        }
        acciones={
          <>
            {puedeEditar && <BotonEnlace a={`/app/empresas/${empresa.id}/editar`} variante="secundario" icono="lapiz">Editar</BotonEnlace>}
            <BotonEnlace a={`/app/contactos/nuevo?empresaId=${empresa.id}`} icono="mas">Nuevo contacto</BotonEnlace>
            <BotonEnlace a={`/app/oportunidades/nueva?empresaId=${empresa.id}`} icono="mas">Nueva oportunidad</BotonEnlace>
          </>
        }
      />

      <div className={pantalla.grillaDetalle}>
        <div className={pantalla.pila}>
          <Tarjeta>
            <TarjetaCabecera titulo="Datos de la empresa" />
            <TarjetaCuerpo>
              <ListaDatos datos={[
                { etiqueta: "Razón social", valor: empresa.legalName || "—" },
                { etiqueta: "CUIT", valor: empresa.cuit || "—" },
                { etiqueta: "Industria", valor: empresa.industry || "—" },
                { etiqueta: "Correo", valor: empresa.email ? <a href={`mailto:${empresa.email}`}>{empresa.email}</a> : "—" },
                { etiqueta: "Teléfono", valor: empresa.phone ? <a href={`tel:${empresa.phone}`}>{empresa.phone}</a> : "—" },
                { etiqueta: "Sitio web", valor: urlSitio ? <a href={urlSitio} target="_blank" rel="noopener noreferrer">{empresa.website}</a> : "—" },
                { etiqueta: "Dirección", valor: [empresa.address, empresa.locality].filter(Boolean).join(", ") || "—" },
                { etiqueta: "Observaciones", valor: empresa.notes || "—", ancho: true },
              ]} />
            </TarjetaCuerpo>
          </Tarjeta>

          <ContactosEmpresa key={id} empresaId={id} />
          <OportunidadesCliente key={id} empresaId={id} />
          <HistorialComercial key={id} asociacion={{ tipo: "companies", id }} puedeRegistrar={puedeEditar} />
        </div>

        <aside className={pantalla.pila}>
          <Tarjeta>
            <TarjetaCabecera titulo="Gestión comercial" nivel={3} />
            <TarjetaCuerpo>
              <ListaDatos columnas={1} datos={[
                { etiqueta: "Responsable comercial", valor: nombreResponsable },
                { etiqueta: "Origen", valor: <NombreCatalogo catalogo="origenes" id={empresa.originId} /> },
              ]} />
            </TarjetaCuerpo>
          </Tarjeta>
        </aside>
      </div>
    </>
  );
}
