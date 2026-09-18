import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import "./shared/styles/tokens.css";
import "./shared/styles/base.css";
import { Rutas } from "./app/router/Rutas";
import { cacheApi } from "./shared/api/cache";
import { ProveedorSesion } from "./shared/sesion/ProveedorSesion";

const contenedor = document.getElementById("root");
if (!contenedor) throw new Error("No se encontró el nodo #root.");

createRoot(contenedor).render(
  <StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={cacheApi}>
        <ProveedorSesion>
          <Rutas />
        </ProveedorSesion>
      </QueryClientProvider>
    </BrowserRouter>
  </StrictMode>,
);
