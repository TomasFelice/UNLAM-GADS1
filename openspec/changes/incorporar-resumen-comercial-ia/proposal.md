# Proposal

## Why

Retomar una negociación exige releer actividades, observaciones y cambios de etapa. Un resumen comercial verificable permite al vendedor y a la supervisión reconstruir necesidades, acuerdos, objeciones y asuntos por confirmar, aprovechando el texto libre ya registrado.

El usuario eligió incorporar esta función y usar exclusivamente Gemini API gratuita. La consigna permite IA después de completar y probar el núcleo; las validaciones locales históricas no sustituyen la demostración autenticada remota todavía pendiente.

## What Changes

- Generar automáticamente el resumen comercial al entrar al detalle de una oportunidad abierta o cerrada, después de confirmar sus permisos de lectura y sin bloquear las demás secciones.
- Mostrar necesidad del cliente, situación actual, acuerdos/objeciones e información pendiente/contradictoria, con fuentes revisables y cobertura explícita del contexto analizado.
- Incorporar un endpoint backend que obtiene contexto autorizado, invoca Gemini y valida la estructura y las referencias antes de entregar el resultado. No acepta contexto, tenant, instrucciones ni modelo desde el cliente.
- Usar inicialmente `gemini-3.1-flash-lite`; alternativa configurable `gemini-3.5-flash-lite`, condicionada a disponibilidad y cuotas reales del proyecto. Excluir Flash 3.8, aliases `latest`, versiones preview y servicios pagos.
- Contener fallos con plazo total, reintentos acotados, límites locales y estados recuperables; el CRM sigue funcionando cuando la IA no responde. No se promete menor carga ni disponibilidad por popularidad del modelo.
- Persistir y reutilizar el último resultado por tenant/oportunidad mientras coincidan la huella del contexto y versión de prompt, deduplicar solicitudes en curso e invalidar ante cambios comerciales. No generar por foco, reconexión o rerender; ante un fallo ofrecer «Reintentar» sin bucles automáticos.
- Guardar el resumen mediante V6 (ADR-008), sin escribir actividades, etapas ni propuestas. No requiere entrenamiento, búsqueda web, base vectorial o agente con herramientas.
- Documentar configuración, tratamiento de datos del nivel gratuito, pruebas reproducibles y límites de disponibilidad. La credencial compartida en la conversación no se reutiliza ni se copia; se requiere una nueva para la prueba manual.

## Capabilities

### New Capabilities

- `resumen-comercial-ia`: generación automática al abrir una oportunidad, reutilización temporal por sesión, contexto comercial autorizado y acotado, síntesis con fuentes, revisión humana y operación resiliente con Gemini gratuito.

### Modified Capabilities

Ninguna. Se conservan los requisitos de `historial-comercial` y `carga-crm`; la nueva capacidad los complementa sin cambiar la lectura íntegra paginada del historial. La generación automática se limita al detalle activo y autorizado, con carga independiente y reutilización acotada; no se precarga desde listados, tablero ni otras rutas.

## Impact

- Backend: nuevo módulo `assistance`, servicios de proyección en `opportunities` y `activities`, configuración del cliente HTTP y errores compatibles con `ProblemDetail`.
- Arquitectura: registrar antes del código un ADR que extienda el grafo con `assistance → {activities, opportunities}`; los módulos existentes no dependen del asistente. Mantener los contratos entre módulos en sus servicios, sin exponer JPA ni consumir DTOs HTTP internos.
- API: `POST /api/v1/opportunities/{id}/commercial-summary`; actualizar OpenAPI y tipos frontend generados.
- Frontend: panel en `PaginaOportunidadDetalle`, fuentes expandibles y hook de generación integrado con sesión e invalidaciones existentes; CSS Modules y tokens.
- Operación: variables sólo backend, feature flag apagado por defecto, sin alta de facturación ni despliegue implícito. Pruebas automáticas con proveedor simulado y PostgreSQL efímero; evaluación manual optativa contra Gemini con datos ficticios.
