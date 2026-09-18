# Documentación técnica de zTech CRM

Este directorio traduce las definiciones funcionales y arquitectónicas del proyecto a guías ejecutables para diseño, implementación y entrega.

## Documentos

1. [Arquitectura general](arquitectura/01-arquitectura-general.md): contexto, stack, contenedores, despliegue y restricciones.
2. [Módulos y capas](arquitectura/02-modulos-y-capas.md): límites del monolito modular, dependencias y organización de frontend y backend.
3. [Datos, seguridad y concurrencia](arquitectura/03-datos-seguridad-y-concurrencia.md): multi-tenancy, autorización, persistencia, auditoría y reservas.
4. [Plan de desarrollo](planificacion/plan-de-desarrollo.md): hitos, alcance por entrega, secuencia, pruebas y criterios de salida.
5. [Registro de decisiones](decisiones/decisiones-pendientes.md): decisiones funcionales resueltas y temas aún abiertos.
6. [ADR](decisiones/adr/): decisiones que cambian arquitectura, límites modulares, contratos o tecnología.
7. [Contexto del proyecto](context/00-proyecto.md): resumen compartido, reglas y estado registrado.
8. [Especificación del backend](specs-backend/requirements.md): requisitos, [diseño](specs-backend/design.md) y [tareas](specs-backend/tasks.md).
9. [Guía de asistentes](asistentes.md): uso del mismo contexto con Claude, Codex y modelos sin acceso automático a archivos.
10. [Plan de la maqueta de frontend](planificacion/plan-frontend-maqueta.md): alcance, rutas, SEO y lenguaje visual de la Entrega 1.
11. [Bitácora del frontend](planificacion/bitacora-frontend.md): registro breve de lo ejecutado en la rama `feature/frontend`.
12. [Registro de decisiones](decisiones/decisiones-pendientes.md): temas que deben acordarse antes de cerrar diseño o aceptación.
13. [Decisiones del frontend](decisiones/decisiones-frontend.md): temas abiertos que surgieron al construir la maqueta.

## Marca

`marca/` contiene los archivos aprobados del logo en sus variantes de color, monocromo y escala de grises. Los derivados para la web (favicons, iconos de aplicación e imagen Open Graph) se generan a partir de ellos y viven en `frontend/public/`.

## Fuentes y precedencia

- `consignas/*.pdf` definen alcance, entidades mínimas, reglas de negocio, casos de uso, entregas y orden obligatorio de desarrollo. **Es la fuente de máxima autoridad.**
- `ERS - CRM Eventos Corporativos.docx` consolida y detalla esos requisitos.
- `Arquitectura General — zTech CRM.docx` define arquitectura y tecnologías.
- `TP_CRM_relevamiento.docx` aporta el análisis original de la consigna y el orden de desarrollo.

Los `.docx` anteriores se citan como antecedentes: no están incluidos en este checkout.
No asumir que están disponibles ni inventar sus requisitos. Las resoluciones vigentes
se documentan en los ADR, el registro de decisiones y las [decisiones técnicas](context/03-decisiones-tecnicas.md).

Ante una diferencia: la consigna gobierna el alcance, la ERS gobierna el comportamiento del producto y el documento de arquitectura gobierna las decisiones técnicas, siempre que no contradiga a las anteriores. Las decisiones aún abiertas se registran explícitamente; no deben resolverse de manera implícita en el código.

Cuando una decisión se aparta deliberadamente de la consigna, debe quedar registrada como ADR con el motivo — es el caso de [ADR-003](decisiones/adr/ADR-003-multi-tenancy.md), que mantiene la multi-tenancy aunque la consigna la excluya del alcance.

## Contexto operativo para agentes

[`AGENTS.md`](../AGENTS.md) contiene las instrucciones comunes para todos los asistentes.
[`CLAUDE.md`](../CLAUDE.md) las importa para Claude Code; Codex usa `AGENTS.md` como
entrada. El conocimiento compartido vive en `docs/context/` y `docs/specs-backend/`.
Las rutas antiguas de `.claude/` sólo redirigen allí. Ver [guía de uso](asistentes.md).
Si cambia una decisión, actualizar el contexto y las specs afectados en el mismo cambio.

## Mantenimiento

Actualizar estos documentos en el mismo pull request que cambie un límite modular, contrato, tecnología, regla crítica o hito. Las decisiones de arquitectura irreversibles o costosas deben registrarse como ADR antes de implementarse.
