# Backend — Tareas

Registro compartido para Claude, Codex y otros asistentes. Las verificaciones fechadas
son antecedentes de esas sesiones; no reemplazan ejecutar los checks del cambio actual.

Sigue los hitos de `docs/planificacion/plan-de-desarrollo.md` y la estrategia de corte
vertical (migración → dominio → service → controller → tests). Cada tarea referencia el
requisito (`requirements.md`) y la sección de diseño (`design.md`) que cubre.

> ## 📍 Punto de retomada (última actualización: 17/09/2026)
>
> **Fases 0-4 completas y estabilización post-E1 en curso.** El esquema incluye contraseña
> temporal/revocación, responsables obligatorios, datos ampliados de empresas y salones,
> tipos de evento, rango horario, servicios N:N y exclusión GiST. El frontend ya tiene
> cliente HTTP, tipos OpenAPI, sesión, guardas, login, cambio de clave y empresas reales.
> BE-SEC-06 ya aplica alcance de vendedor. V1 incluye un escenario comercial compacto.
> Tras el reinicio de la base, V1–V4 se consolidaron en una sola V1 (17/09/2026).
> Se adelantaron las lecturas de actividades e historial y los filtros de DP-10 para
> integrar todas las rutas privadas del frontend con la API.
>
> **Validación histórica del 14/09/2026 (antes de consolidar):** `./mvnw.cmd clean verify` pasó 38/38 pruebas
> (5 unitarias + 33 IT) con PostgreSQL 16. Incluye Flyway V1–V3 desde base vacía,
> `MigrationV3IT` para el salto V2→V3 con registros legacy y `SellerScopeIT` para
> alcance comercial y acceso cruzado. Frontend: 6/6 pruebas, lint y build en verde.
>
> **Sin commits ni pushes**: el usuario pidió explícitamente no hacer ninguno en este
> proyecto salvo que lo pida de nuevo.
>
> Cada fase completa tiene notas de bugs reales encontrados (y su fix) y de alcance
> dejado afuera a propósito, con la razón — leerlas antes de tocar código relacionado.

> **BE-DOC-01 es continuo**: todo controller nuevo lleva sus anotaciones Swagger
> (`@Tag`/`@Operation`/`@ApiResponse`) en el mismo PR que lo crea. No aparece como tarea
> repetida en cada fase de abajo para no inflar la lista — se aplica siempre.

---

## Consolidación de migraciones — 17/09/2026

- [x] Por pedido del usuario tras reiniciar la base, unificar V1–V4 en
      `V1__initial_schema.sql`: esquema final, restricciones y todos los datos semilla.
- [x] Sustituir `MigrationV3IT` (backfill V2→V3) por `MigrationV1IT`: instalación desde
      cero, versión única, nueva ejecución sin cambios, seed y exclusión GiST.
- Validación de esta sesión: `./mvnw.cmd clean verify` pasó 45/45 pruebas (5 unitarias
  y 40 de integración) con PostgreSQL 16 de Testcontainers. `git diff --check` y
  enlaces locales de la documentación revisados. No se ejecutó contra Supabase.
- Las referencias a V2/V3/V4 en los registros fechados siguientes describen el trabajo
  histórico. Los cambios futuros se incorporan en nuevas migraciones desde V2.

## Fase 0 — Base técnica

- [x] Scaffold Maven (`backend/pom.xml`, Java 25, Spring Boot 4.1.1) — design §1
- [x] Estructura de paquetes (`shared/`) — design §2
- [x] `application.yml` + perfiles `local`/`prod` (variables: `DB_URL`, `DB_USERNAME`,
      `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `PORT`)
- [x] `backend/Dockerfile` (build multietapa) — design §1 — build real verificado con Docker
- [x] Conexión a Supabase probada en local (12/09): conexión **directa** funciona desde
      esta red (tiene IPv6 real) — Flyway aplicó V1+V2 contra el proyecto real, login
      end-to-end verificado con curl. El pooler Supavisor session sigue siendo
      obligatorio para **Render** (su salida no tiene IPv6) — pendiente de ese connection
      string cuando se configure el deploy
- [x] `V1__initial_schema.sql` con las 13 tablas — design §3, §4 — corrido contra
      Postgres real (constraints probados: `ck_opportunities_party`, `ux_companies_tenant_cuit`)
- [x] `V2__seed_catalogs.sql`: tenant semilla, `ADMIN` semilla, 7 etapas (DP-06), venues,
      event_services, origins, loss_reasons, activity_types — design §4 — conteos verificados
- [x] Flyway corre limpio contra Testcontainers **y contra Supabase real** (vía Spring
      Boot en ambos casos, no sólo SQL crudo) — V1 y V2 aplicadas, 34s de arranque total
      (latencia real de red)
- [x] `shared/audit`: `AuditableEntity`, `TenantOwnedEntity`, `AuditorAwareImpl` — design §3
- [x] `shared/exception`: jerarquía + `GlobalExceptionHandler` — design §7 — con fix real
      encontrado por test (`ErrorResponse` de Spring MVC no se aplastaba a 500)
- [x] `shared/dto`: `PageResponse<T>` — design §8
- [x] Actuator: `/actuator/health` responde `{"status":"UP"}` contra Supabase real
- [ ] Deploy inicial en Render (aunque sea un esqueleto) — **pendiente**: requiere que el
      usuario conecte el repo en su cuenta de Render (no es algo que se pueda hacer sin
      acceso a esa cuenta) — usuario lo está gestionando
- [x] `springdoc-openapi` activo (3.1.1, con esquema Bearer JWT) — design §10 — `/v3/api-docs`
      y `/swagger-ui.html` verificados en runtime contra Supabase real

## Fase 1 — Acceso mínimo (BE-SEC-01/02/03/07/08/10, BE-ACC-01)

- [x] Entidad `Tenant` (`tenancy/`) + `TenantRepository`
- [x] Entidad `User` + enum `Role` (`access/domain/`)
- [x] `shared/security`: `JwtService`, `JwtAuthenticationFilter`,
      `CustomUserDetailsService`, `AuthenticatedUser`, `TenantContext`,
      `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` — design §6
- [x] `SecurityConfig`: cadena de filtros, rutas públicas vs. protegidas — design §6
- [x] `AuthController` + `AuthService`: `POST /api/v1/auth/login` — design §10, con
      anotaciones Swagger (BE-DOC-01)
- [x] Test: login válido devuelve JWT; login inválido (password o email incorrectos)
      devuelve `401` sin distinguir causa — `AuthControllerIT`, contra Testcontainers
- [x] Test: endpoint protegido sin token devuelve `401`; con token válido atraviesa la
      seguridad (verificado contra un 404 real de Spring MVC, no un 401)
- [x] Login verificado manualmente con curl contra Supabase real (local): token válido,
      datos del admin semilla correctos — [ ] falta repetir esto contra Render una vez
      desplegado

### Nota de diseño resuelta durante la implementación

`users.email` pasó a ser único **a nivel sistema**, no por tenant (`UNIQUE (tenant_id,
email)` → `UNIQUE (email)`): el login de BE-SEC-01 no tiene selector de organización y,
en la práctica, sólo existe un tenant. Actualizado en `V1__initial_schema.sql`,
`design.md` y `decisiones-pendientes.md` (DP-03).

### Gotchas de Spring Boot 4.1.1 encontrados y resueltos (dejar como referencia)

1. **Flyway no se activa solo con `flyway-core`** — Spring Boot 4 modularizó la
   auto-configuración; hace falta `spring-boot-starter-flyway` además de
   `flyway-database-postgresql`.
2. **`TestRestTemplate` se movió** de `spring-boot-test` a un artefacto nuevo,
   `spring-boot-resttestclient`, en el paquete `org.springframework.boot.resttestclient`
   — y ese artefacto no arrastra `spring-boot-restclient` (bug conocido de Spring Boot
   4.0.x/4.1.x), hace falta agregarlo aparte para que `RestTemplateBuilder` esté
   disponible. Además requiere `@AutoConfigureTestRestTemplate` explícito en el test.
3. **Spring Boot 4 usa Jackson 3** (`tools.jackson.*`), no Jackson 2. `jjwt-jackson`
   sigue usando Jackson 2 internamente sin conflicto (namespaces distintos), pero por eso
   los handlers de seguridad (`RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`)
   arman el JSON de error a mano en vez de depender de un `ObjectMapper` inyectado.
4. **Testcontainers 2.x movió `PostgreSQLContainer`** de `org.testcontainers.containers`
   a `org.testcontainers.postgresql`, y dejó de ser genérico.
5. Excepciones de Spring MVC como `NoResourceFoundException` implementan la interfaz
   `ErrorResponse` (no extienden `ErrorResponseException`) — un `@ExceptionHandler`
   genérico de `Exception.class` las aplasta a 500 si no se las detecta con
   `instanceof ErrorResponse` explícitamente.
6. `HttpStatus.UNPROCESSABLE_ENTITY` (422) se renombró a `HttpStatus.UNPROCESSABLE_CONTENT`
   (RFC 9110 cambió la frase oficial de "Unprocessable Entity" a "Unprocessable
   Content"). El código sigue siendo 422 en ambos casos, pero son constantes de enum
   distintas — comparar contra la vieja hace fallar un `assertEquals` aunque la
   respuesta HTTP sea correcta. Usar `UNPROCESSABLE_CONTENT` en código nuevo.

## Fase 2 — Clientes (BE-CUS-01..07) ✅ completa

- [x] Entidades `Company`, `Contact` (`customers/domain/`) — design §3.3, §3.4.
      `salesRepId`/`originId` son `Long` planos (no `@ManyToOne`), porque cruzan módulo
      (`access`/`catalogs`) y ADR-001 prohíbe una relación JPA directa entre módulos —
      `Contact.company` sí es `@ManyToOne` real porque `Contact` y `Company` viven en el
      mismo módulo
- [x] DTOs request/response + `CompanyMapper`, `ContactMapper` — un solo `*Request` por
      entidad reutilizado en alta y edición (mismos campos en ambos casos)
- [x] `CompanyRepository`, `ContactRepository` (`*AndTenantId`)
- [x] `CompanyService`, `ContactService`: alta, edición, detalle, listado paginado
- [x] `CompanyController`, `ContactController` — design §10, con anotaciones Swagger
- [x] Validación: `cuit`/`document` únicos por tenant cuando presentes — check
      proactivo en el service (`409` con mensaje claro) + `@ValidCuit` (formato de
      estructura únicamente; el dígito verificador sigue pendiente de DP-03, no se
      resolvió por iniciativa propia)
- [x] Relacionar contacto con empresa (`companyId` opcional) al crear/editar, incluida
      la desvinculación (pasar a cliente individual)
- [x] Test de servicio/controller: `CompanyControllerIT` (7 tests), `ContactControllerIT`
      (5 tests) — contra Testcontainers, cubren alta, edición, detalle con contactos
      anidados, duplicados → `409`, formato inválido → `400`, no encontrado → `404`,
      empresa relacionada inexistente → `404`, paginación
- [ ] Test de tenant: acceso cruzado por listado y por id directo → `404` — **pendiente**,
      requiere un segundo tenant de prueba; se agrega cuando haya un caso de uso real de
      alta de tenant para no fabricar datos de test ad-hoc
- [x] Demo verificable: crear empresa + contacto relacionado, consultar detalle — cubierto
      por `detailIncludesRelatedContacts`

### Bug real encontrado y corregido en esta fase

Los tests `*IT` (convención Failsafe) nunca corrían con `mvn test`/`mvn verify` tal como
estaba el `pom.xml`: sólo tenía configurado `spring-boot-maven-plugin`, sin
`maven-failsafe-plugin`. Surefire (fase `test`) no incluye `*IT.java` por defecto — el
`clean verify` reportado como exitoso en la Fase 1 corrió **cero tests**, en silencio
(`BUILD SUCCESS` con 2 segundos de duración era la señal de alarma que no miré con
suficiente atención). Se agregó `maven-failsafe-plugin` al `pom.xml`; confirmado con
`mvn clean verify`: **17/17 tests** corren de verdad (5 auth + 7 companies + 5 contacts).
**Lección para las fases siguientes: mirar siempre el conteo real de `Tests run`, nunca
sólo `BUILD SUCCESS`.**

## Fase 3 — Oportunidades (BE-OFF-01/03, BE-CAT-01, BE-OPP-01/02/03) ✅ completa

- [x] Entidades `Venue`, `EventService` (`offerings/domain/`) — design §3.5, §3.6
- [x] `VenueController`/`Service`, `EventServiceController`/`Service` (sólo lectura en
      E1); además `getActiveOrThrow` en `VenueService` para uso cross-módulo (BE-OFF-05)
- [x] Entidad `Stage` (`catalogs/domain/`) + `StageController` (sólo lectura en E1) —
      no extiende `TenantOwnedEntity` (la tabla no tiene auditoría completa);
      `assertOpenAndActive`/`getSummary` en `StageService` para uso cross-módulo
- [x] Entidad `Opportunity` (`opportunities/domain/`) — design §3.9. Todo FK a otro
      módulo (`companyId`, `contactId`, `salesRepId`, `venueId`, `stageId`, `originId`)
      es un `Long` plano, nunca `@ManyToOne` cruzando módulo (ADR-001); `@Version`
      mapeado desde ya para BE-OPP-11 (Fase 8)
- [x] `OpportunityRepository`, `OpportunityService`: alta, edición, detalle, listado
- [x] Regla: `companyId` o `contactId`, al menos uno — `BusinessException` → `422`
- [x] `OpportunityController` — design §10, con anotaciones Swagger
- [x] `OpportunityMapper` con datos anidados (empresa/contacto, salón, etapa) — el
      mapper es puro (sin dependencias de otros `service`); `OpportunityService`
      resuelve los datos cross-módulo y se los pasa armados. `salesRepId` queda como id
      plano: `access` no tiene `UserService` de propósito general todavía (Fase 6)
- [x] `CreateOpportunityRequest` (con `stageId`) vs. `UpdateOpportunityRequest` (sin
      `stageId` ni `salesRepId`) separados a propósito — cambiar etapa o responsable no
      es una edición genérica (docs/arquitectura/02-modulos-y-capas.md)
- [x] Test de servicio: alta con empresa, alta con contacto (implícito en los distintos
      tests), alta sin ninguno → `422` — `OpportunityControllerIT` (6 tests), de paso
      ejercita `GET /venues` y `GET /stages` contra el seed real
- [x] Test: etapa inicial no abierta → `422`; salón/etapa/empresa inexistentes → `404`
- [ ] Test de tenant — mismo pendiente que Fase 2, misma razón
- [x] Demo verificable: seleccionar salón y etapa precargados, crear oportunidad —
      cubierto por `createsOpportunityWithCompanyAndFetchesDetail`

### Alcance no cubierto a propósito en esta fase

- `VenueController`/`EventServiceController`: sin tests de integración dedicados (se
  ejercitan indirectamente via `OpportunityControllerIT`); su lógica es un `getById`/
  `list` de solo lectura idéntico en patrón a lo ya probado en Fase 2.
- `salesRepId`, `originId`, `lossReasonId` en `Opportunity`: sin validación de
  existencia en el service (mismo patrón ya aceptado en Fase 2 para `originId` de
  Company/Contact) — la FK de la base los protege igual, con un mensaje de error menos
  específico hasta que exista `UserService`/`catalogs` completo (Fase 6).
- BE-OPP-10 (capacidad del salón) y BE-OPP-11 (reserva sin solapamiento): explícitamente
  Fase 8, dependen de DP-07/DP-08 sin resolver.

## Fase 4 — Embudo E1 (BE-OPP-05/06) ✅ completa

- [x] Entidad `StageHistory` (append-only) — design §3.10. Append-only por
      construcción, no sólo por convención: sin setters; nada permite mutarla una vez
      creada
- [x] `StageHistoryRepository`: sólo `save` + `findAllByTenantIdAndOpportunityId...`;
      `deleteById`/`delete` sobreescritos con `UnsupportedOperationException` (no
      exhaustivo — cubre los casos obvios, no cada variante de `CrudRepository`)
- [x] `ChangeStageService` transaccional — design §9.1, pasos 1-4 (sin ramas `win`/`lose`
      todavía: pedir una etapa `WON`/`LOST` da el mismo `422` que cualquier etapa no
      abierta, reservado para `/win`/`/lose` en la Fase 7). Casos extra cubiertos:
      misma etapa → `422`, oportunidad cerrada → `409` (ya aplicado aunque hoy ninguna
      oportunidad puede cerrarse todavía)
- [x] `Opportunity.changeStage(Long)`: mutador separado de `updateDetails`, a propósito
- [x] `POST /api/v1/opportunities/{id}/stage`
- [x] `GET /api/v1/opportunities/board` agrupado por etapa — las 7 columnas siempre
      presentes (incluidas WON/LOST), aunque estén vacías; sin paginar (igual criterio
      que venues/stages: dato acotado, consumo tipo tablero completo)
- [x] Test: cambio de etapa persiste `Opportunity` y `StageHistory` atómicamente —
      `ChangeStageServiceTest` (unitario, Mockito puro — primer test que corre bajo
      Surefire/`mvn test`, no sólo Failsafe) + `OpportunityBoardAndStageIT` (E2E real)
- [x] Test: si falla el insert de historial, no queda la etapa cambiada (rollback) —
      cubierto por `propagatesExceptionWhenHistoryInsertFailsSoTransactionCanRollBack`:
      prueba que la excepción se propaga sin `catch` (lo que activa el rollback
      declarativo de `@Transactional`, garantizado por Spring — no hace falta
      re-verificarlo con Testcontainers forzando un fallo real de base)
- [ ] Despliegue estable verificado contra el ambiente publicado — pendiente de Render
      (usuario lo está gestionando)

**33/33 tests pasando** (5 unitarios + 28 de integración) tras esta fase.

Con esto se cierra el alcance funcional de **Entrega 1** (24/09): login, empresas y
contactos, oportunidades con empresa/contacto/responsable/salón, embudo agrupado por
etapa con cambio persistido.

## Documentación de Entrega 1 (adelantada a pedido del usuario, 12/09)

El usuario pidió que los artefactos de documentación (originalmente Fase 10, planeados
para el final) existan también al cierre de Entrega 1, con alcance acotado a lo que
existe hoy — se completan/actualizan de nuevo antes de la entrega final (Fase 10).

- [x] `backend/README.md` — stack, variables de entorno, cómo levantar (con/sin
      Docker), cómo testear, credencial semilla, estructura de paquetes, y una sección
      "Estado y alcance" explícita sobre qué es E1 y qué falta
- [x] `backend/docs/openapi.yaml` — exportado real desde `/v3/api-docs.yaml` contra la
      app corriendo (no a mano)
- [x] `backend/docs/diagrams/component-diagram.md` — módulos reales de esta entrega
      (sin `activities`, que todavía no existe)
- [x] `backend/docs/diagrams/sequence-login.md`
- [x] `backend/docs/diagrams/sequence-change-stage.md`
- [ ] `sequence-win-lose-opportunity.md` / `sequence-concurrent-reservation.md` — a
      propósito NO se hicieron todavía: documentarían un flujo que no existe en el
      código hasta la Fase 7/8; se agregan cuando esas funcionalidades se implementen
- [x] `backend/docs/postman/zTech-CRM.postman_collection.json` — los endpoints de E1
      (Auth, Companies, Contacts, Venues, EventServices, Stages, Opportunities incluido
      cambio de etapa y tablero), con auto-seteo de `accessToken`/`companyId`/etc.
      encadenando los requests
- [x] `backend/docs/postman/zTech-CRM-Local.postman_environment.json`
- [x] `backend/docs/docker.md`
- [x] `backend/docker-compose.yml`, `backend/.env.example` — ya existían desde la Fase 0

### Bug real encontrado armando esta documentación

Al bajar el `openapi.yaml` real, `/v3/api-docs.yaml` devolvía `401` — el patrón
`/v3/api-docs/**` en `SecurityConfig` no cubre `/v3/api-docs.yaml` (es un sufijo sobre
el mismo segmento, no un sub-path; tampoco lo cubre para `/v3/api-docs` a secas en todas
las versiones). Nada de la suite de tests lo detectaba porque ninguno pegaba contra esas
rutas. Corregido agregando las tres variantes explícitas
(`/v3/api-docs`, `/v3/api-docs.yaml`, `/v3/api-docs/**`) y un test de regresión
(`apiDocsAndSwaggerUiArePublicWithoutToken` en `AuthControllerIT`) para que no vuelva a
pasar desapercibido. **34/34 tests** tras el fix.

## Auditoría de `docs/arquitectura/` contra la implementación real (12/09)

El usuario preguntó si los documentos de arquitectura de la raíz (`docs/arquitectura/`,
no `backend/docs/`) seguían al día con lo implementado — se habían actualizado antes de
programar, pero no se revisaron de nuevo después. Se releyeron los 3 documentos línea
por línea contra el código real y se corrigieron los desvíos encontrados:

- `03-datos-seguridad-y-concurrencia.md`: la baja lógica decía `active` **o**
  `deleted_at`; no hay `deleted_at` en ningún lado del esquema. `Company`/`Contact`
  usan su propio `status` (no un booleano aparte); sólo los catálogos usan `active`.
- `03-datos-seguridad-y-concurrencia.md`: `@Version` en `Opportunity` estaba descripto
  como algo que "puede incorporarse" — ya está mapeado desde la Fase 3, sólo falta el
  flujo que lo ejerza (Fase 8).
- `02-modulos-y-capas.md`: ejemplo de migración con nombre incorrecto
  (`V2__initial_catalogs.sql` → `V2__seed_catalogs.sql`, el archivo real).
- `02-modulos-y-capas.md`: decía que `CreateOpportunity` debía ser una clase de
  `service` propia, igual que `ChangeStage`. En la implementación real sólo
  `ChangeStage` lo es (coordina dos escrituras atómicas); crear/editar oportunidad son
  métodos de `OpportunityService` con validación real pero una sola escritura. Se
  corrigió el texto para describir el criterio aplicado, no el original.
- `02-modulos-y-capas.md`: la sección "Pruebas por límite" describía 4 capas separadas
  (dominio/aplicación/infraestructura/web); lo que se hizo en la práctica son 2
  estrategias reales — unitarios Mockito sólo para servicios con lógica genuina
  (`ChangeStageServiceTest`) más `*IT` de punta a punta contra Testcontainers, que
  cubren aplicación+infraestructura+web de una sola vez. Reescrita para reflejar esto.
- `02-modulos-y-capas.md`: el árbol de paquetes listaba `activities/` como si ya
  existiera. Se anotó cada módulo con su fase real (implementado vs. pendiente).
- **Bug de código encontrado en la auditoría** (no sólo de documentación):
  `01-arquitectura-general.md` decía "los logs incluyen requestId, userId y tenantId",
  pero `JwtAuthenticationFilter` nunca poblaba `tenantId`/`userId` en el MDC — sólo
  `requestId` (vía `RequestIdFilter`) se veía de verdad en los logs; el resto del
  patrón de logging quedaba siempre vacío. Corregido: el filtro ahora agrega ambos al
  MDC tras autenticar y los limpia en un `finally` (Tomcat reutiliza hilos entre
  requests). Verificado en vivo contra Supabase real, no sólo por lectura de código:
  `tenant=1 user=1 req=...` aparece poblado en el log de una consulta autenticada.
  **34/34 tests siguen pasando** tras el fix.

Lo que **no** se tocó por ser genuinamente prospectivo, no una afirmación incorrecta
sobre el presente: la sección de reservas concurrentes/GiST (Fase 8, todavía no
implementada), el alcance del `SELLER` (BE-SEC-06, Fase 5/6), y los ejemplos de
contrato HTTP que incluyen `/win` (todavía no existe, pero se presenta como ejemplo de
convención de nombres, no como afirmación de que ya está implementado).

## "¿Está listo para que alguien lo levante en local siguiendo sólo el README?" (12/09)

El usuario preguntó esto explícitamente. La respuesta honesta era "no" hasta este punto
— el README documentaba pasos que en la práctica no alcanzaban. Se verificó cada paso
ejecutándolo de verdad (no sólo releyendo el texto) y se corrigieron 3 problemas reales:

1. **`.env` no se carga solo.** Spring Boot no lee `.env` por su cuenta — sólo
   `docker-compose.yml` lo hace, vía `env_file`. Un `./mvnw spring-boot:run` a secas
   arranca sin ninguna variable seteada y falla al crear el `DataSource`. Se agregaron
   `backend/run.ps1` y `backend/run.sh`, que cargan `.env` al proceso antes de invocar
   Maven. Probados de punta a punta contra Supabase real (health `UP`, Flyway aplicado).
2. **JDK equivocado activo da un error críptico.** En una máquina con más de un JDK
   instalado (esta misma, de hecho — JDK 17 seguía siendo el default en una terminal
   nueva), correr con el JDK equivocado tira
   `UnsupportedClassVersionError: ... class file version 69.0 ... up to 61.0`, que no
   dice "instalá JDK 25". Los scripts ahora detectan esto primero y cortan con un
   mensaje claro. Además usan `$JAVA_HOME/bin/java` directo (igual que hace `mvnw`
   internamente) en vez de armar un `PATH` nuevo — en Git Bash, meter un `JAVA_HOME`
   tipo `C:/...` dentro de `$PATH` rompe el `PATH` porque `:` es su separador y choca
   con los dos puntos de la letra de unidad de Windows. Costó un debugging real
   encontrarlo — quedó documentado en el comentario del script para no repetirlo.
3. **El perfil `local` no era automático.** El README decía que `local` corría por
   default; en los hechos, sin `SPRING_PROFILES_ACTIVE` seteado Spring cae al perfil
   `default` (visto literalmente en el log: "No active profile set"). Corregido en el
   texto, y los scripts nuevos sí fuerzan `local` como fallback.

**Verificado en vivo, no sólo leído:** `run.ps1` y `run.sh` corren completos contra
Supabase real (Flyway migra, health `UP`, login funciona) y ambos detectan
correctamente el JDK equivocado y el `.env` faltante con mensajes claros, probado en
los tres casos.

**Docker Compose, verificado en ambos sentidos (12/09, el usuario consiguió el
connection string del Session pooler):**
- Con la conexión **directa**: falla exactamente como está documentado
  (`UnknownHostException` — la red de Docker es IPv4-only).
- Con el **Session pooler** (`aws-0-us-west-2.pooler.supabase.com:5432`, usuario
  `postgres.<project-ref>`): funciona completo — Flyway migra, health `UP`, login
  responde con un JWT válido, todo dentro del contenedor. **DT-07 queda confirmado
  empíricamente**, no sólo como decisión de diseño.

Con esto, alguien que clone el repo, instale JDK 25 y Docker Desktop, complete `.env`
con credenciales de Supabase (propias o del equipo, usando el pooler si van a usar
Docker) y corra `run.ps1`/`run.sh` o `docker compose up --build` **sí** llega a una API
funcionando y testeable — verificado por los dos caminos, no asumido.

## ✅ Entrega 1 — 24/09

- [ ] Recorrido completo probado manualmente en el ambiente desplegado: login → alta
      empresa → alta contacto → alta oportunidad → verla en el embudo → cambiar de etapa
      → refrescar y comprobar persistencia — pendiente de Render
- [ ] Tag de versión en git — **no aplica por ahora**: el usuario pidió no hacer commits
      ni pushes; queda a su cargo cuando decida
- [ ] Checklist de "criterio de terminado" de `plan-de-desarrollo.md` repasado

---

## Fase 5 — Estabilización (post-E1, hacia 02/10)

- [x] Cerrar DP-01, DP-02, DP-03 (resto), DP-04, DP-08, DP-10 con el usuario
- [ ] Corregir defectos detectados en la demo de E1
- [x] BE-CUS-08: baja lógica por cambio de estado; falta ampliar el test histórico
- [x] BE-SEC-06: alcance del `SELLER` según DP-02. Listados e ids directos de empresas,
      contactos, oportunidades y tablero respetan asignación; los clientes relacionados
      por una oportunidad propia son de sólo lectura. `CustomerVisibilityPort` invierte
      la dependencia y evita acoplar `customers` a tablas de `opportunities`.
- [x] `SellerScopeIT`: cubre asignaciones directas, relaciones por oportunidad,
      exclusión de contactos hermanos, `404` en lectura/escritura fuera de alcance,
      oportunidades ajenas y acceso directo a otro tenant.

## Fase 6 — Seguridad y maestros EF (hacia 13/10)

- [x] BE-ACC-02/03/04: alta, listado, detalle, edición, baja lógica y reset de usuarios
      sólo `ADMIN`; cubierto por `UserControllerIT`
- [x] BE-SEC-05: autorización por métodos activada y aplicada al módulo de usuarios
- [x] BE-ACC-05: cambio obligatorio de contraseña y revocación por `auth_version`,
      cubierto por `PasswordChangeIT`
- [ ] BE-OFF-02/04: ABM completo de `Venue` y `EventService`
- [ ] BE-CAT-02/03/04/05: ABM de los cuatro catálogos configurables
- [ ] Tests negativos de rol para cada endpoint administrativo nuevo

## Fase 7 — Flujo comercial EF (hacia 25/10)

- [x] Entidad `Activity` (`activities/domain/`) — design §3.11
- [ ] `ActivityController`/`Service`: BE-ACT-01
- [x] Lectura paginada de actividades y endpoints cronológicos por empresa/contacto/oportunidad (BE-ACT-02). El alta BE-ACT-01 sigue pendiente.
- [x] `GET /api/v1/opportunities/{id}/stage-history`: BE-ACT-03; el alta de oportunidad registra su etapa inicial.
- [ ] `WinOpportunityService`, `LoseOpportunityService` — design §9.2 (BE-OPP-07/08)
- [ ] BE-OPP-09: bloqueo de edición sobre oportunidad cerrada + caso de uso de reapertura
      (según resolución de DP-01)
- [ ] BE-OPP-04: asignar/reasignar responsable
- [ ] Tests de las 15 reglas de negocio de la consigna aplicadas a este flujo

## Fase 8 — Especialización completa (hacia 04/11)

- [x] Resolver DP-07 (semántica temporal)
- [x] Restricción de exclusión GiST (originalmente V3, consolidada en V1) sobre `opportunities` — design §3.9
- [x] Validación histórica `MigrationV3IT`: backfill V2→V3 sin pérdida de datos.
      Reemplazada por `MigrationV1IT` tras el reinicio de la base del 17/09.
- [x] BE-OPP-10: validación de capacidad del salón; advertencia UI separada para >50
- [ ] BE-OPP-11: validación de aplicación + traducción de violación GiST a `409` — design §9.3
- [ ] Test de concurrencia con Testcontainers: dos confirmaciones simultáneas, una sola
      tiene éxito
- [ ] `@Version` (optimistic locking) en `Opportunity` activo y probado

## Fase 9 — Candidato final (hacia 09/11)

- [x] BE-CUS-09, BE-OPP-12: filtros y búsqueda con `Specification` — design §8
- [x] Paginación consistente, máximo 100 y orden validado en listados comerciales
- [ ] Revisión de accesibilidad básica de las respuestas de error (mensajes usables)
- [ ] Observabilidad: logs con `requestId`/`userId`/`tenantId`, sin datos sensibles
- [ ] Regresión completa de los 17 casos de uso mínimos de la consigna
- [x] `backend/docs/openapi.yaml` actualizado y cliente TypeScript regenerado

## Fase 10 — Documentación y artefactos de entrega (BE-DOC-02..07)

Se ejecuta con la implementación ya terminada y estable. Detalle completo del contenido
de cada artefacto en `design.md` §12.

- [ ] `backend/.env.example` con todas las variables de entorno usadas, sin valores reales
- [ ] `backend/docker-compose.yml`: levanta la imagen del `Dockerfile` contra Supabase
      vía `.env` — design §12.5
- [ ] `backend/docs/docker.md`: build, run, variables obligatorias, nota sobre
      Testcontainers — design §12.5
- [ ] `backend/docs/openapi.yaml`: export estático desde `/v3/api-docs.yaml`, verificado
      contra el Swagger UI en runtime — design §12.2
- [ ] `backend/docs/diagrams/component-diagram.md` (Mermaid) — design §12.3
- [ ] `backend/docs/diagrams/sequence-login.md` (Mermaid) — design §12.3
- [ ] `backend/docs/diagrams/sequence-change-stage.md` (Mermaid) — design §12.3
- [ ] `backend/docs/diagrams/sequence-win-lose-opportunity.md` (Mermaid) — design §12.3
- [ ] `backend/docs/diagrams/sequence-concurrent-reservation.md` (Mermaid) — design §12.3
- [ ] `backend/docs/postman/zTech-CRM.postman_collection.json`: todos los endpoints,
      organizados por módulo, con auto-seteo de `{{accessToken}}` tras login — design §12.4
- [ ] `backend/docs/postman/zTech-CRM-Local.postman_environment.json` — design §12.4
- [ ] Prueba manual: importar la colección + environment en Postman limpio y ejecutar el
      recorrido completo (login → alta → cambio de etapa → cierre) sin tocar nada a mano
- [ ] `backend/README.md` completo según la estructura de design §12.6
- [ ] Revisión cruzada: todo lo que el README promete (comandos, endpoints, credenciales)
      funciona ejecutado desde cero en una máquina limpia

## ✅ Entrega final — 12/11

- [ ] Demostración integral end-to-end sobre el ambiente desplegado
- [ ] Documentación (`docs/`, `AGENTS.md`, `CLAUDE.md`, `backend/docs/`,
      `backend/README.md`) al día con lo implementado
- [ ] Tag de versión en git

---

## IA (opcional, después del núcleo)

No se planifica en este documento hasta que el núcleo esté aprobado y estable — según la
consigna, entra sólo después de completar y probar las funcionalidades principales.
