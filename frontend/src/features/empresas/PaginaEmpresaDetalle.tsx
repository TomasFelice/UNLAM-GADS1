import { Link, useParams } from "react-router-dom";
import { Avatar } from "../../shared/components/Avatar";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Etiqueta } from "../../shared/components/Etiqueta";
import { Icono } from "../../shared/components/Icono";
import { ListaDatos } from "../../shared/components/ListaDatos";
import { Tarjeta, TarjetaCabecera, TarjetaCuerpo } from "../../shared/components/Tarjeta";
import { ESTADO_CLIENTE, plural } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import pantalla from "../../shared/styles/pantalla.module.css";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEmpresa } from "./apiEmpresas";
import css from "./Empresas.module.css";
import { LineaDeActividades } from "../actividades/LineaDeActividades";
import { useActividades } from "../actividades/apiActividades";
import { useOrigenes } from "../catalogos/apiCatalogos";

export function PaginaEmpresaDetalle() {
  const parametros = useParams();
  const id = Number(parametros.id);
  const idValido = Number.isSafeInteger(id) && id > 0;
  const consulta = useEmpresa(idValido ? id : undefined);
  const usuarios = useUsuariosAsignables();
  const actividades = useActividades(idValido ? { tipo: "companies", id } : undefined);
  const origenes = useOrigenes();

  if (!idValido) {
    return <EstadoVacio icono="empresas" titulo="No encontramos esa empresa" descripcion="El enlace no es válido." />;
  }

  if (consulta.isPending) {
    return <EstadoVacio icono="empresas" titulo="Cargando empresa…" descripcion="Estamos consultando sus datos y contactos." />;
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
  const contactos = empresa.contacts ?? [];
  const responsable = usuarios.data?.find((usuario) => usuario.id === empresa.salesRepId);
  const nombreResponsable = responsable ? `${responsable.firstName} ${responsable.lastName}` : "—";
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
            <BotonEnlace a={`/app/empresas/${empresa.id}/editar`} variante="secundario" icono="lapiz">Editar</BotonEnlace>
            <BotonEnlace a={`/app/contactos/nuevo?empresaId=${empresa.id}`} icono="mas">Nuevo contacto</BotonEnlace>
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

          <Tarjeta aSangre>
            <TarjetaCabecera
              titulo="Contactos"
              descripcion={`${plural(contactos.length, "contacto asociado", "contactos asociados")} a esta empresa`}
              acciones={<BotonEnlace a={`/app/contactos/nuevo?empresaId=${empresa.id}`} variante="secundario" tamano="sm" icono="mas">Agregar</BotonEnlace>}
            />
            <TarjetaCuerpo>
              {!contactos.length ? (
                <EstadoVacio icono="contactos" titulo="Sin contactos" descripcion="Agregá la persona con la que negociás en esta empresa." />
              ) : (
                <ul className={css.listaContactos}>
                  {contactos.map((contacto) => (
                    <li key={contacto.id}>
                      <Avatar iniciales={`${contacto.firstName?.[0] ?? ""}${contacto.lastName?.[0] ?? ""}`} tamano="md" />
                      <div>
                        <Link to={`/app/contactos/${contacto.id}`}>{contacto.firstName} {contacto.lastName}</Link>
                        <span className={css.cargo}>{contacto.email || "Sin correo"}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </TarjetaCuerpo>
          </Tarjeta>

          <Tarjeta>
            <TarjetaCabecera titulo="Historial comercial" descripcion="Interacciones registradas con la empresa" />
            <TarjetaCuerpo>
              <LineaDeActividades actividades={actividades.data?.content ?? []} cargando={actividades.isPending}
                error={actividades.isError ? actividades.error.message : undefined} alReintentar={() => actividades.refetch()} />
            </TarjetaCuerpo>
          </Tarjeta>
        </div>

        <aside className={pantalla.pila}>
          <Tarjeta>
            <TarjetaCabecera titulo="Gestión comercial" nivel={3} />
            <TarjetaCuerpo>
              <ListaDatos columnas={1} datos={[
                { etiqueta: "Responsable comercial", valor: nombreResponsable },
                { etiqueta: "Contactos asociados", valor: contactos.length },
                { etiqueta: "Origen", valor: origenes.data?.find((origen) => origen.id === empresa.originId)?.name ?? "Sin especificar" },
              ]} />
            </TarjetaCuerpo>
          </Tarjeta>
        </aside>
      </div>
    </>
  );
}
