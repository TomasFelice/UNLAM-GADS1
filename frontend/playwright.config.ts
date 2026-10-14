import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["json", { outputFile: "test-results/resultados.json" }]],
  use: { baseURL: "http://localhost:14173", browserName: "chromium", trace: "retain-on-failure" },
  webServer: [
    { command: "node scripts/servir-api-prueba.mjs", url: "http://localhost:18080/actuator/health", timeout: 60000, reuseExistingServer: false },
    { command: "npm run preview -- --host 127.0.0.1 --port 14173 --strictPort", url: "http://localhost:14173", timeout: 30000, reuseExistingServer: false },
  ],
});
