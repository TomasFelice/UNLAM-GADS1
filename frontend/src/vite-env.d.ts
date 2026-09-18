/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL absoluta del sitio, sin barra final. La resuelve `vite.config.ts`. */
  readonly VITE_SITE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
