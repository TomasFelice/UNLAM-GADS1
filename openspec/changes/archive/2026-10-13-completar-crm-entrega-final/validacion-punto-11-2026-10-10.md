# Validación integrada de §11 — 10/10/2026

Continuación autorizada hasta §11. El usuario reservó **11.8 para ejecución manual**
y pidió no publicar todavía. No se despliega ni se certifica el ambiente publicado.
La matriz siguiente corresponde a ejecuciones locales de esta sesión; las evidencias
de §1–§10 siguen siendo antecedentes separados.

## Entorno y resultados

- Windows/PowerShell, Temurin 25.0.4.1, Docker Desktop 29.0.1, PostgreSQL 16 efímero.
- `backend/`: `./mvnw.cmd clean verify`: **5 unitarias + 108 IT**, cero
  fallos/errores/omitidos, BUILD SUCCESS. Después de ampliar el fixture de migración:
  `./mvnw.cmd verify '-Dit.test=PipelineMigrationIT'`: **5 unitarias + 3 IT** correctas.
- Corrida backend final `./mvnw.cmd verify`, después de ampliar la comprobación
  de contraseña: **5 unitarias + 109 IT**, cero fallos/errores/omitidos, BUILD SUCCESS.
  `createdAndResetPasswordsUseSaltedBcryptWithoutExposingCredentials` comprueba hashes
  BCrypt distintos para la misma clave, validación del hash y reset sin clave/hash
  en respuestas. Esta corrida también incluye el fixture V1→V5 ampliado.
- `frontend/`: `npm test`: **45 pruebas en 14 archivos**; `npm run lint` y
  `npm run build` correctos. Tipos regenerados con `npm run generar:api`, hash idéntico.
- Copia limpia de fuentes sin `node_modules`/`dist`: `npm ci`, 45 pruebas y build
  correctos. Oxlint inicialmente omitió la copia por estar dentro de `backend/target`
  (ignorado por Git); la invocación explícita sobre fuentes/configuración pasó.
- `docker build -t ztech-crm:validacion-local-11 .`: correcto. Compose validado y
  arrancado con una copia de su configuración, puerto 18081 y PostgreSQL efímero:
  health UP, login semilla correcto con `mustChangePassword=true`, V1–V5 y GiST.
  Se cerraron contenedores/red de esta comprobación. No se usó Supabase remoto.
- Primera regresión E2E: **21 correctas y 3 fallidas**. El test nuevo de login
  exigía una etiqueta exacta sin el asterisco obligatorio; la revisión de rutas
  exigía h1 también al presentar acceso denegado (que usa h3). Se corrigieron
  ambas expectativas. Newman detectó defectos reales de la colección: origen
  todavía tratado como array, IDs fijos y parámetros de query ausentes del objeto
  URL importado. Se corrigieron los ejemplos, sin modificar el contrato.
- Reejecución focalizada sobre base nueva: **3/3 E2E**, cero omitidos/reintentos.
  Incluye recorrido integral, Newman y **576 combinaciones** de 48 rutas × tres
  roles × 390/1440 px × temas claro/oscuro. Newman importó colección/environment
  y ejecutó **99 solicitudes y 109 aserciones**, cero errores.
- Corrida completa final: **24/24 E2E** en 7 minutos, cero fallos, omitidos o
  reintentos. Incluye nuevamente las 576 combinaciones y Newman completo.
  Se revisaron capturas de móvil/escritorio y el menú móvil operado con teclado.
  Al terminar se cerraron API/preview y se restauró `npm run build` habitual.

Evidencia conservada sin tokens ni contraseñas:

- [Resumen backend: 5 unitarias + 109 IT](evidencias/regresion-backend-punto-11.json).
- [Resumen de los 24 E2E](evidencias/regresion-e2e-punto-11.json).
- [576 combinaciones de rutas](evidencias/rutas-verificadas-punto-11.json).
- [Newman: 99 solicitudes y 109 aserciones](evidencias/newman-punto-11.json).
- Capturas revisadas: [móvil](evidencias/admin-movil-oscuro.png) y
  [escritorio](evidencias/admin-escritorio-oscuro.png).

## Evidencia por punto EF

Todos los casos referencian suites ejecutadas en esta sesión. Los nombres de archivo
de integración corresponden a `backend/src/test/java/com/ztech/crm/`; los E2E a
`frontend/e2e/`.

| Caso | Comportamiento verificado | Evidencia |
|---|---|---|
| EF-01 | Alta/edición/baja/reset, clave temporal, revocación y último admin | `access/UserAdministrationIT`, `PasswordChangeIT`, `usuarios.spec.ts` |
| EF-02 | ADMIN, SALES_MANAGER y SELLER operan con permisos distintos | `UserAdministrationIT`, `SellerScopeIT`, `rutas-entrega.spec.ts` |
| EF-03 | 403 administrativo, 404 por ID ajeno y exclusión por listado | `UserAdministrationIT`, `OfferingAdministrationIT`, `CatalogAdministrationIT`, `PipelineAdministrationIT`, `ActivityRegistrationIT` |
| EF-04 | Clientes completos, CUIT canónico/verificador, origen, herencia y bajas | `CompanyControllerIT`, `ContactControllerIT`, `CustomerManagementIT`, `clientes.spec.ts` |
| EF-05 | Oferta administrable, paginada y con baja lógica/referencias históricas | `OfferingAdministrationIT`, `oferta.spec.ts` |
| EF-06 | Responsables heredados, reemplazados y reasignados con actor/fecha | `CustomerManagementIT`, `OpportunityCreationIT`, `OpportunityLifecycleIT`, `ciclo-oportunidades.spec.ts` |
| EF-07 | Alta/edición/detalle/listado y ciclo íntegro | `OpportunityControllerIT`, `OpportunityLifecycleIT`, `entrega-integrada.spec.ts` |
| EF-08 | Dos embudos, plantillas, predeterminado, orden atómico e inactividad | `PipelineAdministrationIT`, `PipelineMigrationIT`, `embudos.spec.ts` |
| EF-09 | Etapa inicial, cambio, cierre y reapertura append-only | `OpportunityBoardAndStageIT`, `OpportunityLifecycleIT`, `ciclo-oportunidades.spec.ts` |
| EF-10 | Actividad realizada contextual, actor servidor, asociaciones derivadas | `ActivityRegistrationIT`, `actividades.spec.ts`, `entrega-integrada.spec.ts` |
| EF-11 | Historias completas con páginas/empates, bajas y autores inactivos | `ActivityRegistrationIT`, `actividades.spec.ts` |
| EF-12 | Ganar/perder atómicos, valor/motivo, nota y recuperación ante 409 | `OpportunityLifecycleIT`, `ciclo-oportunidades.spec.ts` |
| EF-13 | Motivo configurable, activo obligatorio y snapshot del nombre original | `CatalogAdministrationIT`, `OpportunityLifecycleIT`, `catalogos.spec.ts`, `ciclo-oportunidades.spec.ts` |
| EF-14 | Cuatro catálogos y etapas configurables con rol/tenant | `CatalogAdministrationIT`, `PipelineAdministrationIT`, `catalogos.spec.ts`, `embudos.spec.ts` |
| EF-15 | Filtros, URL/páginas, búsqueda posterior a 20 y cancelación tardía | `OpportunityBoardAndStageIT`, `OfferingAdministrationIT`, `alta-oportunidades.spec.ts`, Vitest selectores/carga |
| EF-16 | Horarios Buenos Aires/UTC, capacidad real y reserva GiST entre embudos | `OpportunityCreationIT`, `OpportunityLifecycleIT`, `alta-oportunidades.spec.ts` |

## Evidencia por mejora del PDF

| Caso | Resultado y prueba |
|---|---|
| PDF-01 | Carga acotada y caché: `OpportunityLoadIT`, `carga.spec.ts`, 45 Vitest. La comparación de tiempos sigue en [§10](evidencias/comparacion-carga-punto-10.md); no se inventa otra medición en §11. |
| PDF-02 | Reasignación de oportunidad: `assignsWithActorDateAndChangesSellerScopeWithoutLosingActivities`, `ciclo-oportunidades.spec.ts`. |
| PDF-03 | Ganar/perder separados y formularios recuperables: `ciclo-oportunidades.spec.ts`, revisión de rutas/temas. |
| PDF-04 | Servicios accesibles y fechas sin detalle de almacenamiento: `alta-oportunidades.spec.ts`, revisión de rutas; sin cambios de semántica horaria. |
| PDF-05 | Registro desde los tres detalles: `actividades.spec.ts`, `ActivityRegistrationIT`. |
| PDF-06 | ABM de salones: `oferta.spec.ts`, `OfferingAdministrationIT`. |
| PDF-07 | Usuarios y roles: `usuarios.spec.ts`, `UserAdministrationIT`, `PasswordChangeIT`. |
| PDF-08 | Alta de empresa sin mención de backend: aserción en `clientes.spec.ts`. |
| PDF-09 | Panel con texto comercial y listas independientes: `carga.spec.ts`. |
| PDF-10 | Contactos por empresa, limpieza e incompatibles rechazados: `alta-oportunidades.spec.ts`, `OpportunityCreationIT`, Vitest selección de clientes. |
| PDF-11 | Alta breve, opcionales conservados y cancelación sin escritura: `alta-oportunidades.spec.ts`. |
| PDF-12 | Alta desde contacto corporativo/individual posterior a página 0: `alta-oportunidades.spec.ts`. |
| PDF-13 | Embudos independientes configurables y selección en URL: `embudos.spec.ts`, `PipelineAdministrationIT`. |

## Las 15 reglas generales

| Regla | Evidencia del resultado |
|---|---|
| 1 Responsable obligatorio | `OpportunityCreationIT`: herencia DP-02, reemplazo autorizado y asignación propia de vendedor. |
| 2 Empresa o contacto obligatorio | `OpportunityControllerIT`, `OpportunityCreationIT`: alta sin cliente rechazada; individual admitido. |
| 3 Etapa actual obligatoria | `OpportunityControllerIT`, `PipelineAdministrationIT`: etapa inicial y embudo coherentes; IDs ajenos rechazados. |
| 4 Abierta sólo en OPEN | `OpportunityBoardAndStageIT`, `OpportunityLifecycleIT`: destino incompatible rechazado sin historial parcial. |
| 5 Ganada con fecha/valor | `winReopenLoseReconstructsBothClosuresAndReleasesReservation`: reloj servidor, valor y snapshot. |
| 6 Perdida con fecha/motivo | Mismo caso y `validatesRequiredDataKindsAndForeignIdsForAllRolesWithoutPartialWrites`. |
| 7 Todo cambio con historial | `OpportunityBoardAndStageIT` y `historyInsertFailureRollsBackClosureAndVersionThroughRealHttp`. |
| 8 Actividad con actor/fecha | `recordsRealFactsOnOwnOpenAndClosedOpportunityWithServerActorAndDerivedParties`; fecha del hecho distinta de registro. |
| 9 Actividad asociada | `rejectsFutureMissingContradictoryAndInactiveDataWithoutWriting`; derivaciones y contradicciones. |
| 10 Baja sin borrar historia | `CustomerManagementIT`, `OfferingAdministrationIT`, `paginationTiesInactiveAuthorsAndLogicalCustomerDeactivationKeepAllFacts`. |
| 11 Alcance vendedor | `SellerScopeIT`, `sharedCustomerNeverExposesActivitiesOfAnotherSellersOpportunity`. |
| 12 Permisos backend | Negativos HTTP directos en acceso, oferta, catálogos, embudos, oportunidades y actividades. |
| 13 Hash seguro | `UserAdministrationIT.createdAndResetPasswordsUseSaltedBcryptWithoutExposingCredentials`: BCrypt con salt distinto, reset y respuestas sin contraseña/hash. |
| 14 Cerrada sin edición libre | `genericEditRequiresReadVersionAndNeverOverwritesALaterWriteOrAClosedOpportunity`; reapertura elevada y nota. |
| 15 Cambios identificables | `assignsWithActorDateAndChangesSellerScopeWithoutLosingActivities`, históricos con autor y fecha, `ObservabilityIT`. |

Capacidad: creación/edición/cierre revalidan; 60 en salón de 80 se admite con aviso,
81 se rechaza. Concurrencia: la prueba con barrera después de ambos prechecks se
repite **tres veces**; cada vez hay una ganada, un 409 y un único historial de cierre.
Otro caso verifica dos escrituras de la misma versión y una petición tardía sin
sobrescritura. IA queda explícitamente fuera por ser opcional.

## Migraciones, contrato y límites

`PipelineMigrationIT` instala desde vacío, actualiza V1→V5 y reejecuta sin cambios.
El segundo tenant ahora tiene usuario, clientes dados de baja, salón, catálogo,
oportunidades abiertas/ganadas/perdidas, servicios, actividades y etapas históricas.
Se comparan IDs, asociaciones, cierres, versiones y filas de hechos antes/después;
una reserva duplicada posterior falla por GiST. V1 no se modifica y no se reinicia
ninguna base existente. `HistorySnapshotMigrationIT` conserva legacy sin inventar
snapshots; `CuitMigrationIT` comprueba normalización y colisiones.

La colección corrige origen paginado, elimina IDs fijos de responsable/tipo de
evento/servicios y prepara una etapa abierta de avance. Los objetos URL incluyen
sus parámetros de query efectivos para la importación (no sólo el campo raw).
Incluye usuarios y cambio de clave
con un usuario propio de prueba; login parametrizado y credenciales vacías en el
environment. Newman verifica códigos HTTP y captura referencias/versiones;
el E2E comprueba además que las **69 operaciones runtime** están presentes en la colección
y que el YAML coincide salvo el puerto local. Los tipos TypeScript permanecen
idénticos tras regenerarlos. Los cinco diagramas se revisaron contra las clases
actuales; se corrigió la descripción del rechazo de JWT en el propio filtro.

La comprobación de Compose sustituyó únicamente conexión/puerto por recursos
efímeros locales. No acredita conexión a Supabase ni despliegue en Render/Vercel.
El SEO comprobado corresponde al DOM renderizado; el prerenderizado DF-03 sigue
fuera del alcance. La demo publicada 11.8 queda pendiente por instrucción expresa.

## Cierre documental y del alcance

Se reconciliaron AGENTS/CLAUDE, contexto, arquitectura, tareas backend, README,
diagramas y artefactos OpenSpec con la implementación actual. Se conservaron las
validaciones históricas con sus fechas y los checks manuales que no se ejecutaron.
OpenSpec estricto, enlaces locales y `git diff --check` correctos; estado Git
revisado sólo en lectura, preservando cambios previos. Sin commits, staging,
pushes, tags, despliegue, archive ni sync.

Estado de salida: **71/72 tareas**; §1–§10 y **11.1–11.7** completos.
**11.8 queda pendiente para el usuario**; no corresponde archivar el cambio.
