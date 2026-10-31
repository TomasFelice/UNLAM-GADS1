# Validación de persistencia — 31/10/2026

Pedido: guardar el resumen y regenerarlo sólo cuando cambie su contexto. Decisión:
[ADR-008](../../../docs/decisiones/adr/ADR-008-persistencia-resumen-comercial.md).

## Verificaciones de esta sesión

- Backend: `./mvnw.cmd verify '-Dit.test=CommercialSummaryIT,OpenApiExportIT' '-Dtest=*Summary*Test'`:
  16 unitarias y 7 IT correctos. PostgreSQL 16 efímero con V1–V6 desde base vacía.
  Se desactivó `DEBUG` en el proceso de prueba: el entorno heredado contenía
  `DEBUG=release`, que activaba logging de fixtures y hacía fallar la comprobación
  de ausencia de texto comercial en logs. La corrida final pasó con `DEBUG=false`.
- IT: resultado guardado y reutilizado entre roles sin proveedor disponible,
  invalidación por notas, actividad sin incremento de versión y renombre de etapa;
  una fila por tenant/oportunidad y conservación del último éxito ante fallos.
  Se mantienen las pruebas de alcance, tenant, anonimato y cambios durante generación.
- OpenAPI exportado desde runtime; tipos regenerados con `npm run generar:api`.
- `npm test -- ResumenComercial.test.tsx`: 17 pruebas correctas.
- `npm run build`: correcto. El build para E2E usó
  `VITE_API_URL=http://localhost:18080/api/v1` antes de iniciar Playwright.
- `npm run test:e2e -- resumen-comercial.spec.ts`: 2 recorridos correctos con API,
  PostgreSQL y proveedor HTTP ficticio. Recarga, retorno, cambio de rol, actividad,
  errores y reintento; 390/1440 px, ambos temas, consola sin errores, sin scroll
  horizontal, noindex del detalle y title/description/canonical de privacidad.
- Enlaces locales de Markdown modificado y `git diff --check`: correctos.

## Límites

No se llamó a Gemini real, no se usó `.env` para pruebas ni se modificó Supabase.
No hubo despliegue, commits ni pushes. Producción requiere publicar las versiones
compatibles; Flyway aplicará V6 al arrancar el backend actualizado.
La frescura se comprueba al consultar; no hay actualización remota en tiempo real.
La huella conserva los límites de las proyecciones existentes documentados en ADR-008.
