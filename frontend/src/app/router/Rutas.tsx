import { Route, Routes } from "react-router-dom";
import { AlCambiarDeRuta } from "./AlCambiarDeRuta";
import { LayoutPublico } from "../layouts/LayoutPublico";
import { LayoutApp } from "../layouts/LayoutApp";

import { PaginaInicio } from "../../features/inicio/PaginaInicio";
import { PaginaNosotros } from "../../features/institucional/PaginaNosotros";
import { PaginaContacto } from "../../features/institucional/PaginaContacto";
import { PaginaTerminos } from "../../features/institucional/PaginaTerminos";
import { PaginaPrivacidad } from "../../features/institucional/PaginaPrivacidad";
import { PaginaNoEncontrada } from "../../features/institucional/PaginaNoEncontrada";

import { PaginaIngreso } from "../../features/acceso/PaginaIngreso";
import { PaginaPanel } from "../../features/panel/PaginaPanel";
import { PaginaEmpresas } from "../../features/empresas/PaginaEmpresas";
import { PaginaEmpresaDetalle } from "../../features/empresas/PaginaEmpresaDetalle";
import { PaginaEmpresaFormulario } from "../../features/empresas/PaginaEmpresaFormulario";
import { PaginaContactos } from "../../features/contactos/PaginaContactos";
import { PaginaContactoDetalle } from "../../features/contactos/PaginaContactoDetalle";
import { PaginaContactoFormulario } from "../../features/contactos/PaginaContactoFormulario";
import { PaginaSalones } from "../../features/salones/PaginaSalones";
import { PaginaOportunidades } from "../../features/oportunidades/PaginaOportunidades";
import { PaginaOportunidadDetalle } from "../../features/oportunidades/PaginaOportunidadDetalle";
import { PaginaOportunidadFormulario } from "../../features/oportunidades/PaginaOportunidadFormulario";
import { PaginaEmbudo } from "../../features/embudo/PaginaEmbudo";
import { PaginaCambiarClave } from "../../features/acceso/PaginaCambiarClave";
import { GuardaClaveCambiada, GuardaSesion } from "./Guardas";

/**
 * El sitio público es indexable; `/ingresar` y todo `/app` se marcan
 * `noindex`. Las rutas privadas conservan el destino original al pedir login.
 */
export function Rutas() {
  return (
    <>
      <AlCambiarDeRuta />
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
    </>
  );
}
