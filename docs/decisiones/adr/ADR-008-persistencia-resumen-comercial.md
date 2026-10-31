# ADR-008 — Persistencia del resumen comercial

- Estado: Aceptada por pedido del usuario
- Fecha: 31/10/2026
- Reemplaza la decisión de resumen efímero de ADR-007.

## Decisión

`assistance` conserva en PostgreSQL el último resultado validado por tenant y
oportunidad, con su huella de contexto, versión de prompt y metadatos originales.
Una nueva migración Flyway incorpora la tabla. No se modifica historia comercial.

Cada consulta autoriza y reconstruye el contexto antes de buscar el resultado.
La misma huella permite reutilizarlo sin TTL, incluso después de recargar o reiniciar.
Sólo un cambio de los datos del contexto o de la versión del prompt requiere generar
y reemplazar el resultado. Los fallos no reemplazan el último éxito; un resumen
desactualizado nunca se entrega como vigente. La función deshabilitada sigue dando 503.

El navegador consulta al volver al detalle para detectar también cambios remotos.
La llamada al proveedor sigue fuera de transacciones, con revalidación de permiso y
huella antes de persistir. No se cambia el cuerpo ni la forma del contrato HTTP.

## Consecuencias

El resultado y sus fuentes pasan a ser datos persistidos sujetos al mismo aislamiento
y conservación que la oportunidad. Se conserva una sola versión por oportunidad.
La detección usa las proyecciones acotadas existentes (últimos 50 hechos por tipo,
totales y datos actuales); no implica seguimiento en tiempo real ni cambios en datos
que no participan del contexto. La admisión continúa siendo por proceso.
