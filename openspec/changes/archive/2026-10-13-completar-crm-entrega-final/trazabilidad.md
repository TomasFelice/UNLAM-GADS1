# Trazabilidad de la entrega final

Fecha de análisis: 09/10/2026. **Observado** significa lectura de código/documentación, no ejecución satisfactoria. Las marcas existentes de septiembre son antecedentes; todos los criterios finales deben validarse de nuevo al implementar.

## Fuentes y precedencia

1. [Consigna general](../../../../docs/consignas/Consigna_Trabajo_Practico.pdf), [Definiciones](../../../../docs/consignas/Definiciones-Generales.pdf), [Módulos](../../../../docs/consignas/Modulos_Principales.pdf) y [Entregas](../../../../docs/consignas/Entregas-CRM.pdf): autoridad de requisitos del TP.
2. [ADR](../../../../docs/decisiones/adr/README.md), [DP aceptadas](../../../../docs/decisiones/decisiones-pendientes.md) y [DT](../../../../docs/context/03-decisiones-tecnicas.md): resoluciones vigentes. Conservar excepción de multi-tenancy de ADR-003.
3. [Checklist encontrado](../../../../docs/specs-backend/tasks.md), [requisitos BE](../../../../docs/specs-backend/requirements.md) y [diseño aprobado](../../../../docs/specs-backend/design.md): estado y compromisos técnicos del proyecto.
4. [Faltantes y mejoras](<../../../../Faltantes_Mejoras para Entrega Final.pdf>): leído en ambas páginas, con sus capturas de interfaz.
5. Checklist de 16 funcionalidades enviado por el usuario en esta conversación y confirmaciones posteriores: varios embudos por tenant con ABM, análisis de precarga/plantillas y alta breve contextual con campos obligatorios.

Los números EF/PDF de esta matriz son índices locales de las fuentes, no reemplazan ni renumeran IDs RF/RN/CA/BE/DP/DT/ADR. No se dispone de la ERS `.docx`.

## Checklist final del usuario

| Punto | Estado observado | Trabajo previsto y tareas |
|---|---|---|
| EF-01 Gestión de usuarios | API de alta, edición, consulta, baja y reset existente; sin pantallas administrativas | Interfaz, guardas y regresión: §2 |
| EF-02 Tres roles | Enum y sesión existentes | Mantener y probar matriz de permisos: §2, §11 |
| EF-03 Permisos por rol | Autorización de usuarios y alcance comercial implementados parcialmente | Ampliar a maestros, embudos y nuevos casos; negativos por rol/tenant en cada corte |
| EF-04 Empresas y contactos completos | ABM real; empresa sin origen en formulario, CUIT sin dígito y URL sin validación backend | Completar datos, validaciones, acciones autorizadas e históricos: §3, §9 |
| EF-05 Productos o servicios | Salones y servicios de lectura | ABM y baja lógica: §4 |
| EF-06 Responsables | Herencia y asignación de clientes/alta de oportunidad existentes | Reasignación explícita de oportunidad e interfaz: §3, §8 |
| EF-07 Oportunidades completas | Alta/edición/listado/detalle de abiertas existentes | Embudo, alta breve, asignación, cierres y reapertura: §6–§8 |
| EF-08 Embudo configurable | Etapas globales por tenant, sólo lectura | Varios embudos con etapas, orden y ABM confirmado por usuario: §6 |
| EF-09 Etapas con historial | Alta inicial y cambio abierto con historial existentes | Conservar y ampliar a cierres/reapertura y etapas configuradas: §6, §8, §9 |
| EF-10 Registro de actividades | Entidad y GET; sin POST ni formulario | Alta append-only autorizada y contextual: §9 |
| EF-11 Historial de clientes y oportunidades | API y líneas de tiempo existentes; frontend consulta página 0 | Navegación completa, alcance de actividad y datos de cada cierre: §8, §9 |
| EF-12 Cierres ganados/perdidos | Campos y seed existentes; sin casos de uso y botones deshabilitados | Transacciones y formularios de cierre: §8 |
| EF-13 Motivos de pérdida | Catálogo de lectura y referencias de seed | ABM y uso obligatorio al perder: §5, §8 |
| EF-14 Etapas, tipos, orígenes, motivos | Catálogos de lectura, incluido tipo de evento | ABM de los cuatro y BE-CAT-06: §5, §6 |
| EF-15 Búsqueda, filtros, paginación | Filtros DP-10 comerciales; maestros no paginados, selectores truncados | Completar listados/selección/históricos y filtro por embudo: §3–§7, §9, §10 |
| EF-16 Especialización real | Modelo evento, servicios, capacidad, GiST y `@Version` presentes | Validar reserva en cierre, traducción específica y concurrencia: §8, §11 |

IA es opcional, no cuenta como faltante obligatorio y no forma parte de las tareas. No agregar documentación por una exigencia inexistente del TP; mantener la que el proyecto ya comprometió.

## Faltantes y mejoras del PDF

| Punto | Evidencia observada | Resolución y tareas |
|---|---|---|
| PDF-01 Carga inicial lenta | Panel descarga tablero completo; imports estáticos; enriquecimiento por oportunidad. Causa runtime todavía no medida | Línea base §1; carga por ruta, lotes, panel acotado y comparación §10 |
| PDF-02 Cambio de responsable en oportunidad | Update excluye responsable; falta `/assign` | Operación explícita y UI: §8 |
| PDF-03 Cierres y margen de botones | Botones deshabilitados contiguos en detalle | Casos reales y separación con tokens: §8 |
| PDF-04 Selector de servicios y aclaración de fechas | Checkboxes sin disposición cuidada y pie con Argentina/UTC | Mejorar selección y quitar detalle de almacenamiento: §7 |
| PDF-05 Registrar actividades | GET y líneas de tiempo existentes; falta alta | API y formulario contextual: §9 |
| PDF-06 ABM de salones | Lecturas únicamente | Gestión completa: §4 |
| PDF-07 Usuarios y roles | Backend existente sin UI administrativa | Completar interfaz: §2 |
| PDF-08 Texto backend en alta de empresa | Ayuda de responsable menciona backend | Explicar asignación comercial: §3 |
| PDF-09 Texto API en panel | Descripción menciona API | Texto comercial junto con rediseño de carga: §10 |
| PDF-10 Filtrar contacto por empresa | Alta consulta contactos sin `companyId`; API ya soporta filtro | Filtro, limpieza de incompatibles y protección servidor: §7 |
| PDF-11 Oportunidad rápida | Formulario completo único | Alta breve confirmada por usuario: §7 |
| PDF-12 Crear desde contacto | Enlace con `contactoId` ya existe; empresa no se precarga por detalle | Completar coherencia y probar contacto fuera de página 0: §7 |
| PDF-13 Distintos embudos configurables | No existe `Pipeline`; orden de etapas único por tenant | ABM múltiple confirmado, migración y plantillas opcionales: §6 |

## Pendientes del checklist existente

| Origen | Tratamiento |
|---|---|
| Fases 0/1/4 y E1: Render, login y recorrido desplegado | Revalidación remota §11, condicionada a disponibilidad; no presumir deploy pendiente o exitoso sólo por notas antiguas |
| Fase 2: acceso cruzado por listado/ID | Fixtures de segundo tenant en §1 y negativos en §2–§9; no hace falta alta pública de tenant |
| Fase 3: cerrar/reabrir y datos de evento | §8, preservando modelo ya presente |
| Fase 5: defectos E1 y baja lógica con historia | Defectos concretos identificados en PDF/matriz; baja e historia §3 y §9; no inventar un listado de bugs de demo no documentado |
| Fase 6: oferta, catálogos y negativos administrativos | §4–§6, incluyendo BE-CAT-06 omitido en la lista de cuatro catálogos |
| Fase 7: actividades, ganar/perder, bloquear cerradas, reapertura, reasignar y reglas | §8–§9; el bloqueo genérico ya existe y se conserva |
| Fase 8: disponibilidad, GiST, concurrencia y versionado | §8; GiST y `@Version` existentes, falta ejercerlos y verificarlos |
| Fase 9: errores, observabilidad, regresión y OpenAPI | Errores en §8–§9, medición/logs §10 y regresión §11; contrato por corte |
| Fase 10: README, env, Docker, OpenAPI, diagramas y Postman | Mantener/actualizar en el corte que cambia cada flujo; verificación limpia e integrada §11 |
| Final: documentación y demo | §11, sin dar funcionalidades planificadas por implementadas |
| E1/Final: tags de versión | Excluidos por prohibición expresa de toda escritura de Git |
| IA posterior al núcleo | Diferida y no bloqueante |

## Evidencia principal del código

- [Usuarios](../../../../backend/src/main/java/com/ztech/crm/access/service/UserService.java) y [rutas frontend](../../../../frontend/src/app/router/rutas.tsx): backend administrable sin pantallas de usuarios/configuración.
- [Oportunidades](../../../../backend/src/main/java/com/ztech/crm/opportunities/service/OpportunityService.java), [etapas](../../../../backend/src/main/java/com/ztech/crm/opportunities/service/ChangeStageService.java) y [entidad](../../../../backend/src/main/java/com/ztech/crm/opportunities/domain/Opportunity.java): abiertos, historial inicial, relaciones activas siempre revalidadas, consultas repetidas y `@Version` existente.
- [Migración V1](../../../../backend/src/main/resources/db/migration/V1__initial_schema.sql): orden de etapas por tenant, GiST y datos semilla existentes.
- [Actividad](../../../../backend/src/main/java/com/ztech/crm/activities/service/ActivityService.java) y [visibilidad](../../../../backend/src/main/java/com/ztech/crm/activities/repository/specification/ActivitySpecifications.java): sólo lectura y OR de asociaciones que requiere cobertura de clientes compartidos.
- [CUIT](../../../../backend/src/main/java/com/ztech/crm/shared/validation/CuitValidator.java), [empresa](../../../../frontend/src/features/empresas/PaginaEmpresaFormulario.tsx) y [solicitud backend](../../../../backend/src/main/java/com/ztech/crm/customers/dto/request/CompanyRequest.java): dígito y URL pendientes; origen no incluido en formulario.
- [Panel](../../../../frontend/src/features/panel/PaginaPanel.tsx), [alta oportunidad](../../../../frontend/src/features/oportunidades/PaginaOportunidadFormulario.tsx), [detalle contacto](../../../../frontend/src/features/contactos/PaginaContactoDetalle.tsx) y [actividad frontend](../../../../frontend/src/features/actividades/apiActividades.ts): evidencia de PDF y paginación incompleta.
- [Embudo](../../../../frontend/src/features/embudo/PaginaEmbudo.tsx): clasifica cierre por nombre y oculta columnas cerradas aun al filtrar estado; debe usar tipo de etapa.
- [Pruebas frontend y scripts](../../../../frontend/package.json): existen Vitest y script/dependencia Playwright; no se encontraron configuración ni pruebas E2E en el inventario leído.

## Cierre de cobertura

Cada grupo de [tasks.md](tasks.md) incluye implementación, pruebas relevantes y documentación/contrato del corte. §11 valida el producto integrado sobre los 16 puntos EF, los 13 puntos PDF y las 15 reglas generales. Marcar una tarea sólo después de su evidencia; completar archivos de planificación no equivale a completar el CRM.
