# Proposal

Propuesta inicial del 09/10/2026. El usuario autorizó posteriormente el apply y su
continuación hasta §11; [tasks.md](tasks.md) y la
[validación integrada](validacion-punto-11-2026-10-10.md) registran el estado actual.
No se publica aún: el usuario reservó 11.8 para realizarlo manualmente.

## Why

zTech CRM ya permite operar clientes, oportunidades y etapas, pero todavía no cubre el recorrido integral de la entrega final del 12/11/2026. Es necesario completar los pendientes de `docs/specs-backend/tasks.md`, las mejoras del PDF y el checklist enviado por el usuario, conservando lo implementado y sus reglas de seguridad.

## What Changes

- Completar la gestión de usuarios desde la interfaz y las guardas por rol, reutilizando el backend existente.
- Completar datos y validaciones de clientes, asignación de responsables y conservación de su historial al dar de baja.
- Implementar ABM de salones, servicios adicionales y catálogos, incluidos tipos de evento, con baja lógica y permisos de `ADMIN`.
- **BREAKING**: incorporar varios embudos por tenant, con ABM, etapas propias, orden y selección de embudo en oportunidades y tablero. Migrar las etapas y oportunidades actuales a un embudo inicial sin perder IDs ni historial.
- **BREAKING**: paginar los listados de oferta y catálogos actualmente devueltos como arrays, adaptando sus consumidores y tipos generados en el mismo corte.
- Ofrecer un embudo corporativo inicial y plantillas opcionales para nuevos embudos; las plantillas se copian y luego se configuran libremente.
- Completar reasignación, cierre ganado/perdido y reapertura autorizada, con historial atómico, capacidad y protección contra reservas concurrentes.
- Registrar actividades realizadas desde empresas, contactos y oportunidades; hacer navegable el historial completo, también después de bajas y reaperturas.
- Incorporar alta rápida con cliente preseleccionado y sólo campos obligatorios; filtrar contactos por empresa y permitir seleccionar clientes más allá de los primeros 20.
- Mejorar selector de servicios, separación de acciones, mensajes y estados de carga. Retirar las referencias técnicas señaladas en el PDF.
- Medir y optimizar carga inicial, consultas y volumen transferido; completar regresión por rol y tenant, contrato generado y preparación de la demostración.

## Capabilities

### New Capabilities

No existen specs principales en `openspec/specs/`. Estas capacidades formalizan los comportamientos a completar; no implican reconstruir lo que ya funciona.

- `administracion-acceso`: gestión de usuarios desde el CRM y acceso por rol.
- `gestion-clientes`: datos completos, responsables, validaciones y baja lógica de empresas/contactos.
- `oferta-comercial`: administración de salones y servicios y referencias históricas.
- `catalogos-comerciales`: administración de orígenes, motivos de pérdida y tipos de actividad/evento.
- `embudos-comerciales`: múltiples embudos, etapas propias, plantillas y tablero configurable.
- `ciclo-oportunidades`: reasignación, cierres, reapertura, concurrencia y auditoría.
- `historial-comercial`: alta de actividades realizadas y consulta íntegra de actividades y etapas.
- `creacion-oportunidades`: alta rápida/contextual, selección coherente de clientes y formulario accesible.
- `carga-crm`: carga acotada por ruta, consultas eficientes y recuperación de errores.

### Modified Capabilities

Ninguna spec principal de OpenSpec existente. Las specs aprobadas de `docs/specs-backend/` siguen siendo referencia y se actualizarán durante la implementación donde cambie el contrato.

## Impact

Backend: `access`, `customers`, `offerings`, `catalogs`, `opportunities`, `activities`, errores compartidos, migraciones nuevas desde V2 y pruebas HTTP/Testcontainers. Frontend: navegación, guardas, pantallas administrativas y comerciales, TanStack Query, formularios y estilos con tokens existentes.

Cambian el contrato OpenAPI y los tipos TypeScript derivados. El nuevo modelo de embudos requiere un ADR antes de implementarse y una migración incremental compatible con los datos de V1. No se propone cambiar stack ni incorporar dependencias de producción por defecto.

Quedan fuera IA, agenda, tareas, notificaciones, facturación, estadísticas nuevas, integraciones y cambios de SEO público ajenos al pedido. La validación del ambiente publicado depende de su disponibilidad y acceso. Este cambio autoriza sólo artefactos de planificación; no implementa código ni ejecuta escrituras de Git, incluidos commits, ramas, tags, staging y pushes.
