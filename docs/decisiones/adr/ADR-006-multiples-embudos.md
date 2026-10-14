# ADR-006 — Múltiples embudos por tenant y etapas propias

- Estado: Aceptada
- Fecha: 09/10/2026
- Participantes: usuario del proyecto; Codex (registro técnico)
- Fuente: confirmación de varios embudos con ABM documentada en el
  [diseño del cambio](../../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/design.md),
  cuya implementación fue solicitada mediante `openspec-apply-change`.
- Amplía: DP-06 y el modelo de etapas único por tenant. Mantiene ADR-001 y ADR-003.
- Requisitos afectados: BE-CAT-01/02/07, BE-OPP-01/02/03/05/06/07/08/09/12.

## Contexto

V1 define etapas con orden único por tenant y oportunidades que referencian una
etapa. El usuario confirmó varios procesos comerciales configurables por organización.
Se necesita conservar los IDs y el historial existentes y evitar etapas de otro
proceso o tenant. El corte §6 implementa este modelo con V3, API y pantallas. Cierres
y reapertura se siguen en §8, sin considerarlos implementados por este ADR.

## Decisión

1. `Pipeline` pertenece a `catalogs/domain`, junto con `Stage`. Su configuración y
   consultas se exponen por `catalogs/service`; `opportunities` consume records de
   esa capa. El uso comercial de una etapa se consulta mediante un puerto definido
   en `catalogs/service` e implementado por `opportunities`, sin invertir la dirección
   de dependencias de ADR-001 ni acceder a repositorios ajenos.
2. Cada embudo tiene `tenant_id`, nombre único por tenant, actividad, marca de
   predeterminado y auditoría. Existe un único predeterminado activo por tenant,
   protegido por índice único parcial y serialización de la configuración del tenant.
   Sustituirlo es atómico; desactivarlo exige designar antes otro activo.
3. Cada etapa pertenece a un embudo fijo. Su posición es única dentro de
   `(tenant_id, pipeline_id)`. Reordenar requiere el conjunto completo de IDs,
   bloqueo del embudo y unicidad diferible para permitir intercambios atómicos.
   El tipo de una etapa con uso comercial es inmutable.
4. Activar un embudo exige al menos una etapa activa de cada clase `OPEN`, `WON`
   y `LOST`. No se puede desactivar la última etapa activa de una clase requerida
   en un embudo activo. Los tipos, nunca los nombres, gobiernan las transiciones.
5. La oportunidad tiene `pipeline_id` obligatorio y fijo tras el alta. Las FKs
   compuestas y el servicio garantizan coherencia de tenant, embudo y etapa.
   Una edición que intenta transferirla a otro embudo se rechaza explícitamente.
6. La baja es lógica y conserva consultas históricas. Un embudo inactivo no admite
   altas ni reaperturas; sus oportunidades abiertas pueden seguir operándose con
   etapas activas existentes. El tablero conserva las columnas históricas necesarias.
7. Las siete etapas de DP-06 son el embudo corporativo inicial. Las plantillas
   opcionales «Eventos corporativos» y «Contratación breve» son definiciones de sólo
   lectura que crean copias con IDs propios en una transacción. Un embudo vacío
   nace inactivo; no se crean procesos adicionales automáticamente.
8. La reserva sigue perteneciendo al salón: la exclusión GiST conserva tenant,
   salón y rango, sin incluir el embudo. Dos procesos no pueden reservar a la vez
   el mismo recurso físico.

## Migración y compatibilidad

Aplicar una migración nueva posterior a V1, sin modificarla ni reiniciar bases.
Crear un embudo corporativo activo y predeterminado por tenant; vincular sus etapas
actuales y derivar el embudo de cada oportunidad de su etapa. Validar pertenencia
antes de imponer `NOT NULL`, FKs compuestas, índices y la nueva unicidad del orden.
Conservar IDs de etapas, oportunidades y filas de `stage_history`. V3 inicializa
las siete etapas de DP-06 sólo si el tenant no tenía ninguna. Si una configuración
parcial existente carece de alguna clase activa requerida, aborta con diagnóstico
y rollback; corregir ese dato concreto antes de volver a aplicar. No agrega
procesos adicionales ni fusiona datos.

El uso comercial toma bloqueos compartidos de embudo/etapa dentro de la transacción.
La configuración toma tenant, embudo y etapa en ese orden, con bloqueo exclusivo
para editar y consultar el uso. Esto conserva tipo/actividad frente a asociaciones
concurrentes sin acceder a repositorios de otro módulo.

Actualizar backend, export OpenAPI y tipos/consumidores frontend en el mismo corte.
El tablero usa el predeterminado si se omite `pipelineId`; listados permiten filtrar
por embudo y consultar inactivos. Probar instalación vacía y actualización desde V1
con varios tenants, datos comerciales e historial, incluida reejecución sin cambios.

La operación real requiere respaldo y ventana coordinada. Si falla una migración
transaccional, conservar la versión previa. Después de escribir datos de varios
embudos, corregir hacia adelante; volver a una API que ignora esa dimensión no es
compatible. Restaurar un respaldo completo exige decisión explícita del operador.

## Alternativas consideradas

- Mantener un embudo por tenant: no satisface la ampliación confirmada.
- Crear un módulo superior de embudos: obliga a mover etapas o introduce ciclos
  sin beneficio suficiente para este monolito.
- Compartir etapas mutables entre embudos o tenants: mezcla configuración e historia.
- Permitir transferencias de oportunidades: requiere mapeo de etapas e historial;
  queda fuera del alcance aprobado de esta entrega.

## Consecuencias

Se amplían persistencia, autorización, filtros y contrato. La configuración requiere
transacciones y pruebas concurrentes además de negativos de rol/tenant. Cada corte
debe distinguir modelo aceptado de funcionalidad implementada. Las referencias
históricas a etapas únicas por tenant describen V1, no el modelo objetivo.
