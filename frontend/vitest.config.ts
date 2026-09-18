import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  define: {
    "import.meta.env.VITE_API_URL": JSON.stringify("http://localhost:8080/api/v1"),
    "import.meta.env.VITE_SITE_URL": JSON.stringify("http://localhost:5173"),
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/configurar.ts",
    restoreMocks: true,
  },
});
