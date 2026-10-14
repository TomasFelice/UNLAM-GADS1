import { lazy, Suspense } from "react";
import { EstadoVacio } from "../../shared/components/EstadoVacio";
import { CATALOGOS, type Catalogo } from "../../features/catalogos/apiCatalogos";
import { Route, Routes } from "react-router-dom";
import { AlCambiarDeRuta } from "./AlCambiarDeRuta";
import { LayoutPublico } from "../layouts/LayoutPublico";
import { LayoutApp } from "../layouts/LayoutApp";

import { GuardaClaveCambiada, GuardaRol, GuardaSesion } from "./Guardas";

const PaginaEmbudos = lazy(() => import("../../features/embudos/PaginaEmbudos").then(modulo => ({ default: modulo.PaginaEmbudos })));
const PaginaNuevoEmbudo = lazy(() => import("../../features/embudos/PaginaNuevoEmbudo").then(modulo => ({ default: modulo.PaginaNuevoEmbudo })));
const PaginaEmbudoConfiguracion = lazy(() => import("../../features/embudos/PaginaEmbudoConfiguracion").then(modulo => ({ default: modulo.PaginaEmbudoConfiguracion })));
const PaginaConfiguracion = lazy(() => import("../../features/catalogos/PaginaConfiguracion").then(modulo => ({ default: modulo.PaginaConfiguracion })));
const PaginaCatalogo = lazy(() => import("../../features/catalogos/PaginaCatalogo").then(modulo => ({ default: modulo.PaginaCatalogo })));
const PaginaCatalogoDetalle = lazy(() => import("../../features/catalogos/PaginaCatalogoDetalle").then(modulo => ({ default: modulo.PaginaCatalogoDetalle })));
const PaginaCatalogoFormulario = lazy(() => import("../../features/catalogos/PaginaCatalogoFormulario").then(modulo => ({ default: modulo.PaginaCatalogoFormulario })));
const PaginaSalonFormulario = lazy(() => import("../../features/salones/PaginaSalonFormulario").then(modulo => ({ default: modulo.PaginaSalonFormulario })));
const PaginaSalonDetalle = lazy(() => import("../../features/salones/PaginaSalonDetalle").then(modulo => ({ default: modulo.PaginaSalonDetalle })));
const PaginaServicios = lazy(() => import("../../features/servicios/PaginaServicios").then(modulo => ({ default: modulo.PaginaServicios })));
const PaginaServicioFormulario = lazy(() => import("../../features/servicios/PaginaServicioFormulario").then(modulo => ({ default: modulo.PaginaServicioFormulario })));
const PaginaServicioDetalle = lazy(() => import("../../features/servicios/PaginaServicioDetalle").then(modulo => ({ default: modulo.PaginaServicioDetalle })));
const PaginaInicio = lazy(() => import("../../features/inicio/PaginaInicio").then(modulo => ({ default: modulo.PaginaInicio })));
const PaginaNosotros = lazy(() => import("../../features/institucional/PaginaNosotros").then(modulo => ({ default: modulo.PaginaNosotros })));
const PaginaContacto = lazy(() => import("../../features/institucional/PaginaContacto").then(modulo => ({ default: modulo.PaginaContacto })));
const PaginaTerminos = lazy(() => import("../../features/institucional/PaginaTerminos").then(modulo => ({ default: modulo.PaginaTerminos })));
const PaginaPrivacidad = lazy(() => import("../../features/institucional/PaginaPrivacidad").then(modulo => ({ default: modulo.PaginaPrivacidad })));
const PaginaNoEncontrada = lazy(() => import("../../features/institucional/PaginaNoEncontrada").then(modulo => ({ default: modulo.PaginaNoEncontrada })));
const PaginaIngreso = lazy(() => import("../../features/acceso/PaginaIngreso").then(modulo => ({ default: modulo.PaginaIngreso })));
const PaginaPanel = lazy(() => import("../../features/panel/PaginaPanel").then(modulo => ({ default: modulo.PaginaPanel })));
const PaginaEmpresas = lazy(() => import("../../features/empresas/PaginaEmpresas").then(modulo => ({ default: modulo.PaginaEmpresas })));
const PaginaEmpresaDetalle = lazy(() => import("../../features/empresas/PaginaEmpresaDetalle").then(modulo => ({ default: modulo.PaginaEmpresaDetalle })));
const PaginaEmpresaFormulario = lazy(() => import("../../features/empresas/PaginaEmpresaFormulario").then(modulo => ({ default: modulo.PaginaEmpresaFormulario })));
const PaginaContactos = lazy(() => import("../../features/contactos/PaginaContactos").then(modulo => ({ default: modulo.PaginaContactos })));
const PaginaContactoDetalle = lazy(() => import("../../features/contactos/PaginaContactoDetalle").then(modulo => ({ default: modulo.PaginaContactoDetalle })));
const PaginaContactoFormulario = lazy(() => import("../../features/contactos/PaginaContactoFormulario").then(modulo => ({ default: modulo.PaginaContactoFormulario })));
const PaginaSalones = lazy(() => import("../../features/salones/PaginaSalones").then(modulo => ({ default: modulo.PaginaSalones })));
const PaginaOportunidades = lazy(() => import("../../features/oportunidades/PaginaOportunidades").then(modulo => ({ default: modulo.PaginaOportunidades })));
const PaginaOportunidadDetalle = lazy(() => import("../../features/oportunidades/PaginaOportunidadDetalle").then(modulo => ({ default: modulo.PaginaOportunidadDetalle })));
const PaginaOportunidadFormulario = lazy(() => import("../../features/oportunidades/PaginaOportunidadFormulario").then(modulo => ({ default: modulo.PaginaOportunidadFormulario })));
const PaginaEmbudo = lazy(() => import("../../features/embudo/PaginaEmbudo").then(modulo => ({ default: modulo.PaginaEmbudo })));
const PaginaCambiarClave = lazy(() => import("../../features/acceso/PaginaCambiarClave").then(modulo => ({ default: modulo.PaginaCambiarClave })));
const PaginaUsuarios = lazy(() => import("../../features/usuarios/PaginaUsuarios").then(modulo => ({ default: modulo.PaginaUsuarios })));
const PaginaUsuarioDetalle = lazy(() => import("../../features/usuarios/PaginaUsuarioDetalle").then(modulo => ({ default: modulo.PaginaUsuarioDetalle })));
const PaginaUsuarioFormulario = lazy(() => import("../../features/usuarios/PaginaUsuarioFormulario").then(modulo => ({ default: modulo.PaginaUsuarioFormulario })));

/**
 * El sitio público es indexable; `/ingresar` y todo `/app` se marcan
 * `noindex`. Las rutas privadas conservan el destino original al pedir login.
 */
export function Rutas() {
  return (
    <>
      <AlCambiarDeRuta />
      <Suspense fallback={<EstadoVacio icono="panel" titulo="Cargando pantalla…" descripcion="Preparando la información." />}>
        <Routes>
          <Route element={<LayoutPublico />}>
            <Route index element={<PaginaInicio />} />
            <Route path="nosotros" element={<PaginaNosotros />} />
            <Route path="contacto" element={<PaginaContacto />} />
            <Route path="terminos" element={<PaginaTerminos />} />
            <Route path="privacidad" element={<PaginaPrivacidad />} />
          </Route>

          <Route path="ingresar" element={<PaginaIngreso />} />

          <Route element={<GuardaSesion />}>
            <Route path="app/cambiar-clave" element={<PaginaCambiarClave />} />

            <Route element={<GuardaClaveCambiada />}>
              <Route path="app" element={<LayoutApp />}>
                <Route index element={<PaginaPanel />} />
                <Route element={<GuardaRol roles={["ADMIN"]} />}>
                  <Route path="salones/nuevo" element={<PaginaSalonFormulario />} />
                  <Route path="salones/:id/editar" element={<PaginaSalonFormulario />} />
                  <Route path="servicios/nuevo" element={<PaginaServicioFormulario />} />
                  <Route path="servicios/:id/editar" element={<PaginaServicioFormulario />} />
                  <Route path="configuracion" element={<PaginaConfiguracion />} />
                  <Route path="configuracion/embudos" element={<PaginaEmbudos />} />
                  <Route path="configuracion/embudos/nuevo" element={<PaginaNuevoEmbudo />} />
                  <Route path="configuracion/embudos/:id" element={<PaginaEmbudoConfiguracion />} />
                  {(Object.keys(CATALOGOS) as Catalogo[]).map(catalogo => <Route key={catalogo} path={`configuracion/${catalogo}`}>
                    <Route index element={<PaginaCatalogo catalogo={catalogo} />} />
                    <Route path="nuevo" element={<PaginaCatalogoFormulario catalogo={catalogo} />} />
                    <Route path=":id" element={<PaginaCatalogoDetalle catalogo={catalogo} />} />
                    <Route path=":id/editar" element={<PaginaCatalogoFormulario catalogo={catalogo} />} />
                  </Route>)}
                  <Route path="usuarios" element={<PaginaUsuarios />} />
                  <Route path="usuarios/nuevo" element={<PaginaUsuarioFormulario />} />
                  <Route path="usuarios/:id" element={<PaginaUsuarioDetalle />} />
                  <Route path="usuarios/:id/editar" element={<PaginaUsuarioFormulario />} />
                </Route>

            <Route path="empresas" element={<PaginaEmpresas />} />
            <Route path="empresas/nueva" element={<PaginaEmpresaFormulario />} />
            <Route path="empresas/:id" element={<PaginaEmpresaDetalle />} />
            <Route
              path="empresas/:id/editar"
              element={<PaginaEmpresaFormulario />}
            />

            <Route path="contactos" element={<PaginaContactos />} />
            <Route path="contactos/nuevo" element={<PaginaContactoFormulario />} />
            <Route path="contactos/:id" element={<PaginaContactoDetalle />} />
            <Route
              path="contactos/:id/editar"
              element={<PaginaContactoFormulario />}
            />

            <Route path="salones" element={<PaginaSalones />} />
            <Route path="salones/:id" element={<PaginaSalonDetalle />} />
            <Route path="servicios" element={<PaginaServicios />} />
            <Route path="servicios/:id" element={<PaginaServicioDetalle />} />

            <Route path="oportunidades" element={<PaginaOportunidades />} />
            <Route
              path="oportunidades/nueva"
              element={<PaginaOportunidadFormulario />}
            />
            <Route
              path="oportunidades/:id"
              element={<PaginaOportunidadDetalle />}
            />
            <Route
              path="oportunidades/:id/editar"
              element={<PaginaOportunidadFormulario />}
            />

                <Route path="embudo" element={<PaginaEmbudo />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<PaginaNoEncontrada />} />
        </Routes>
      </Suspense>
    </>
  );
}
