# Architecture Decision Records

Decisiones que cambian arquitectura, límites modulares, contratos o tecnología. Una
decisión aceptada no se edita: si cambia, se escribe un ADR nuevo que la reemplaza y el
anterior pasa a estado `Reemplazada` con el enlace correspondiente.

| ADR | Título | Estado | Fecha |
|---|---|---|---|
| [001](ADR-001-estructura-de-paquetes.md) | Estructura de paquetes del backend | Aceptada | 12/09/2026 |
| [002](ADR-002-plataforma-supabase-render.md) | Supabase y Render como plataforma, Java 25 y Spring Boot 4 | Aceptada | 12/09/2026 |
| [003](ADR-003-multi-tenancy.md) | Mantener multi-tenancy pese a estar fuera del alcance de la consigna | Aceptada | 12/09/2026 |
| [004](ADR-004-producto-o-servicio.md) | Modelado de "producto o servicio": Venue y EventService | Aceptada | 12/09/2026 |
| [005](ADR-005-frontend-en-vercel.md) | Vercel como hosting definitivo del frontend | Aceptada | 12/09/2026 |

## Plantilla

```markdown
# ADR-XXX — Título

- Estado: Propuesta | Aceptada | Reemplazada por ADR-YYY
- Fecha:
- Participantes:

## Contexto
## Decisión
## Alternativas consideradas
## Consecuencias
```
