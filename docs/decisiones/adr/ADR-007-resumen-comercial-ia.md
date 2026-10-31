# ADR-007 — Resumen comercial efímero con Gemini

- Estado: Aceptada; persistencia revisada por [ADR-008](ADR-008-persistencia-resumen-comercial.md)
- Fecha: 31/10/2026
- Alcance: DP-12 y cambio `incorporar-resumen-comercial-ia`.

## Decisión

El módulo coordinador `assistance` consume exclusivamente servicios públicos de
`activities` y `opportunities`. Los módulos existentes no dependen de él. El grafo
queda `assistance → {activities, opportunities}`, seguido de
`activities → opportunities → {customers, offerings, catalogs} → access → tenancy`.
No introduce ciclos ni expone entidades o DTOs HTTP como contratos del coordinador.

La síntesis usa REST JDK contra Gemini `generateContent`, sin SDK de agentes,
herramientas ni búsqueda. Se obtiene un snapshot autorizado en una transacción
breve de lectura; la llamada externa ocurre fuera de ella y se revalida alcance
y huella antes de entregar el resultado.

El resumen es efímero, no modifica negocio ni requiere migraciones. El cliente
genera al abrir el detalle autorizado y reutiliza durante cinco minutos en memoria
de la sesión, con invalidación por cambios comerciales conocidos.

Se utiliza exclusivamente Free Tier verificado operativamente, modelos explícitos,
clave nueva sólo backend y función apagada por defecto. El fallback permanece vacío
hasta su evaluación real. El código no puede acreditar la facturación de una clave.

## Consecuencias

Se descartan la dependencia inversa oportunidades→actividades (ciclo), el contexto
armado por el cliente (manipulable) y la persistencia de síntesis (fuera de alcance).
La validación de referencias y estructura no demuestra fidelidad semántica: requiere
revisión humana y corpus ficticio. La disponibilidad, cuotas y tratamiento de datos
del proveedor son condiciones externas; las pruebas simuladas no las certifican.
La implementación y su evidencia se registran en las tareas del cambio; este ADR
registra la decisión, sin dar por completadas sus pruebas.
