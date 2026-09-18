import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { ErrorDeApi } from "../../shared/api/errores";
import { Boton, BotonEnlace } from "../../shared/components/Boton";
import { CabeceraPagina } from "../../shared/components/CabeceraPagina";
import { AreaTexto, Campo, Entrada, GrupoCampos, Selector } from "../../shared/components/Campos";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { Tarjeta, TarjetaCuerpo, TarjetaPie } from "../../shared/components/Tarjeta";
import { ESTADO_CLIENTE } from "../../shared/data/formato";
import { Seo } from "../../shared/seo/Seo";
import { useUsuariosAsignables } from "../acceso/apiUsuarios";
import { useEmpresa, useGuardarEmpresa } from "./apiEmpresas";
import css from "./Empresas.module.css";

const opcional = z.string().trim();
const esquema = z.object({
  razonSocial: z.string().trim().min(1, "Ingresá la razón social."),
  nombreComercial: z.string().trim().min(1, "Ingresá el nombre comercial."),
  cuit: opcional.refine((valor) => !valor || /^\d{2}-?\d{8}-?\d$/.test(valor), "Usá 11 dígitos, con o sin guiones."),
  industria: opcional,
  estado: z.enum(["POTENCIAL", "CLIENTE", "INACTIVO", "NO_CONTACTAR"]),
  email: opcional.refine((valor) => !valor || z.email().safeParse(valor).success, "Ingresá un correo válido."),
  telefono: opcional,
  direccion: opcional,
  localidad: opcional,
  sitioWeb: opcional.refine((valor) => !valor || z.url().safeParse(valor).success, "Ingresá una URL completa, por ejemplo https://empresa.com."),
  responsableId: z.string(),
  observaciones: opcional,
});

type DatosEmpresa = z.infer<typeof esquema>;

const VACIA: DatosEmpresa = {
  razonSocial: "", nombreComercial: "", cuit: "", industria: "", estado: "POTENCIAL",
  email: "", telefono: "", direccion: "", localidad: "", sitioWeb: "",
  responsableId: "", observaciones: "",
};

const camposBackend: Record<string, keyof DatosEmpresa> = {
  legalName: "razonSocial", businessName: "nombreComercial", cuit: "cuit",
  industry: "industria", status: "estado", email: "email", phone: "telefono",
  address: "direccion", locality: "localidad", website: "sitioWeb",
  salesRepId: "responsableId", notes: "observaciones",
};

const nuloSiVacio = (valor: string) => valor.trim() || undefined;

export function PaginaEmpresaFormulario() {
  const parametros = useParams();
  const id = parametros.id ? Number(parametros.id) : undefined;
  const edicion = id !== undefined;
  const navegar = useNavigate();
  const empresa = useEmpresa(id);
  const usuarios = useUsuariosAsignables();
  const guardar = useGuardarEmpresa(id);
  const [errorGeneral, setErrorGeneral] = useState<string>();
  const {
    register, handleSubmit, reset, setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosEmpresa>({ resolver: zodResolver(esquema), mode: "onBlur", defaultValues: VACIA });

  useEffect(() => {
    if (!empresa.data) return;
    reset({
      razonSocial: empresa.data.legalName ?? "",
      nombreComercial: empresa.data.businessName ?? "",
      cuit: empresa.data.cuit ?? "",
      industria: empresa.data.industry ?? "",
      estado: empresa.data.status ?? "POTENCIAL",
      email: empresa.data.email ?? "",
      telefono: empresa.data.phone ?? "",
      direccion: empresa.data.address ?? "",
      localidad: empresa.data.locality ?? "",
      sitioWeb: empresa.data.website ?? "",
      responsableId: empresa.data.salesRepId?.toString() ?? "",
      observaciones: empresa.data.notes ?? "",
    });
  }, [empresa.data, reset]);

  const alEnviar = handleSubmit(async (datos) => {
    setErrorGeneral(undefined);
    try {
      const guardada = await guardar.mutateAsync({
        legalName: datos.razonSocial.trim(),
        businessName: datos.nombreComercial.trim(),
        cuit: nuloSiVacio(datos.cuit),
        industry: nuloSiVacio(datos.industria),
        status: datos.estado,
        email: nuloSiVacio(datos.email),
        phone: nuloSiVacio(datos.telefono),
        address: nuloSiVacio(datos.direccion),
        locality: nuloSiVacio(datos.localidad),
        website: nuloSiVacio(datos.sitioWeb),
        salesRepId: datos.responsableId ? Number(datos.responsableId) : undefined,
        notes: nuloSiVacio(datos.observaciones),
      });
      navegar(`/app/empresas/${guardada.id}`, { replace: true });
    } catch (error) {
      if (error instanceof ErrorDeApi) {
        for (const detalle of error.problema.errors) {
          const campo = camposBackend[detalle.field];
          if (campo) setError(campo, { message: detalle.message });
        }
        setErrorGeneral(error.message);
      } else {
        setErrorGeneral("No pudimos guardar la empresa. Intentá nuevamente.");
      }
    }
  });

  if (edicion && empresa.isPending) {
    return <EstadoVacio icono="empresas" titulo="Cargando empresa…" descripcion="Estamos preparando el formulario." />;
  }
  if (edicion && (empresa.isError || !empresa.data)) {
    return <EstadoVacio icono="empresas" titulo="No pudimos abrir la empresa" descripcion={empresa.error?.message ?? "El enlace no es válido."} />;
  }

  return (
    <>
      <Seo titulo={edicion ? "Editar empresa" : "Nueva empresa"} descripcion="" noIndexar />
      <CabeceraPagina
        migas={[{ texto: "Empresas", a: "/app/empresas" }, { texto: edicion ? "Editar" : "Nueva empresa" }]}
        titulo={edicion ? `Editar ${empresa.data?.businessName ?? "empresa"}` : "Nueva empresa"}
        descripcion={edicion ? "Actualizá los datos de la organización y su responsable comercial." : "Registrá la organización para asociarle contactos y oportunidades."}
      />

      <form onSubmit={alEnviar} noValidate>
        <Tarjeta>
          <TarjetaCuerpo>
            {errorGeneral && <p className={css.errorGeneral} role="alert">{errorGeneral}</p>}
            <div className={css.seccionesFormulario}>
              <GrupoCampos titulo="Identificación" descripcion="Datos fiscales y comerciales de la organización.">
                <Campo etiqueta="Razón social" requerido ancho error={errors.razonSocial?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} aria-describedby={descripcion} aria-invalid={Boolean(errors.razonSocial)} {...register("razonSocial")} />}
                </Campo>
                <Campo etiqueta="Nombre comercial" requerido error={errors.nombreComercial?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} aria-describedby={descripcion} aria-invalid={Boolean(errors.nombreComercial)} {...register("nombreComercial")} />}
                </Campo>
                <Campo etiqueta="CUIT" ayuda="11 dígitos, con o sin guiones." error={errors.cuit?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} inputMode="numeric" placeholder="30-71234567-8" aria-describedby={descripcion} aria-invalid={Boolean(errors.cuit)} {...register("cuit")} />}
                </Campo>
                <Campo etiqueta="Industria o actividad" error={errors.industria?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} aria-describedby={descripcion} {...register("industria")} />}
                </Campo>
                <Campo etiqueta="Estado" requerido error={errors.estado?.message}>
                  {(campoId, descripcion) => <Selector id={campoId} aria-describedby={descripcion} {...register("estado")}>
                    {Object.entries(ESTADO_CLIENTE).map(([valor, estado]) => <option key={valor} value={valor}>{estado.texto}</option>)}
                  </Selector>}
                </Campo>
              </GrupoCampos>

              <GrupoCampos titulo="Contacto y ubicación">
                <Campo etiqueta="Correo electrónico" error={errors.email?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} type="email" aria-describedby={descripcion} aria-invalid={Boolean(errors.email)} {...register("email")} />}
                </Campo>
                <Campo etiqueta="Teléfono" error={errors.telefono?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} type="tel" aria-describedby={descripcion} {...register("telefono")} />}
                </Campo>
                <Campo etiqueta="Dirección" error={errors.direccion?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} aria-describedby={descripcion} {...register("direccion")} />}
                </Campo>
                <Campo etiqueta="Localidad" error={errors.localidad?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} aria-describedby={descripcion} {...register("localidad")} />}
                </Campo>
                <Campo etiqueta="Sitio web" ancho error={errors.sitioWeb?.message}>
                  {(campoId, descripcion) => <Entrada id={campoId} type="url" placeholder="https://empresa.com.ar" aria-describedby={descripcion} aria-invalid={Boolean(errors.sitioWeb)} {...register("sitioWeb")} />}
                </Campo>
              </GrupoCampos>

              <GrupoCampos titulo="Gestión comercial">
                <Campo etiqueta="Responsable comercial" ayuda="Si no elegís uno, el backend aplica la asignación correspondiente a tu rol." error={errors.responsableId?.message}>
                  {(campoId, descripcion) => <Selector id={campoId} aria-describedby={descripcion} {...register("responsableId")}>
                    <option value="">Asignación automática</option>
                    {usuarios.data?.map((usuario) => <option key={usuario.id} value={usuario.id}>{usuario.firstName} {usuario.lastName}</option>)}
                  </Selector>}
                </Campo>
                <Campo etiqueta="Observaciones" ancho ayuda="Notas internas del equipo comercial." error={errors.observaciones?.message}>
                  {(campoId, descripcion) => <AreaTexto id={campoId} aria-describedby={descripcion} {...register("observaciones")} />}
                </Campo>
              </GrupoCampos>
            </div>
          </TarjetaCuerpo>
          <TarjetaPie>
            <span>Los campos marcados con asterisco son obligatorios.</span>
            <span className={css.accionesFormulario}>
              <BotonEnlace a={id ? `/app/empresas/${id}` : "/app/empresas"} variante="secundario">Cancelar</BotonEnlace>
              <Boton type="submit" icono="check" disabled={isSubmitting}>{isSubmitting ? "Guardando…" : edicion ? "Guardar cambios" : "Crear empresa"}</Boton>
            </span>
          </TarjetaPie>
        </Tarjeta>
      </form>
    </>
  );
}
