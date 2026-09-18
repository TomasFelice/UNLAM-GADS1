# Instrucciones compartidas — zTech CRM

Aplican a todo el repositorio para Claude Code, Codex y otros asistentes.
TP de Gestión Aplicada al Desarrollo de Software II (UNLaM): CRM especializado en
salones de eventos corporativos, monorepo y monolito modular.

## Inicio de una tarea

1. Revisar el pedido del usuario y `git status --short`; respetar cambios existentes.
2. Leer [Contexto del proyecto](docs/context/00-proyecto.md) y
   [Producto y consignas](docs/context/01-producto-y-consignas.md).
3. Para backend, leer [Convenciones](docs/context/02-backend-convenciones.md),
   [Decisiones técnicas](docs/context/03-decisiones-tecnicas.md) y
   [Tareas y punto de retomada](docs/specs-backend/tasks.md). Consultar los
   [Requisitos](docs/specs-backend/requirements.md) y el
   [Diseño](docs/specs-backend/design.md) del cambio concreto.
4. Para frontend, leer `frontend/README.md` y las decisiones o planes de frontend
   pertinentes antes de modificarlo.
5. Consultar arquitectura, decisiones funcionales y PDFs según la tarea. Distinguir
   alcance planificado, implementación y validaciones históricas.

Las specs del backend ya están aprobadas y la implementación empezó. Continuar el
trabajo autorizado sin volver a pedir aprobación del plan completo. Si una decisión
funcional pendiente afecta la tarea, usar sólo un supuesto ya documentado o consultar
esa decisión concreta; avanzar con el trabajo independiente.

## Alcance y fuentes

- El usuario determina si la sesión trabaja en backend, frontend o documentación.
  No asumir una asignación por ser Claude o Codex. Si hay sesiones paralelas,
  respetar las carpetas asignadas y coordinar el contrato compartido.
- Los PDF de `docs/consignas/` son la máxima autoridad de requisitos del TP.
  Las excepciones deliberadas aprobadas se conservan explícitamente: por ejemplo,
  [ADR-003](docs/decisiones/adr/ADR-003-multi-tenancy.md) mantiene la multi-tenancy.
- Para decisiones técnicas, aplicar los ADR aceptados y las resoluciones vigentes
  de [DT](docs/context/03-decisiones-tecnicas.md) y
  [DP](docs/decisiones/decisiones-pendientes.md) antes que propuestas anteriores.
  Los resúmenes no reemplazan esas resoluciones ni resuelven decisiones abiertas.
- La ERS y otros `.docx` mencionados como antecedentes no están en este checkout.
  No inventar su contenido ni afirmar haberlos leído.
- Las reglas comunes se mantienen en este archivo. `CLAUDE.md` es la entrada para
  Claude; `docs/context/` y `docs/specs-backend/` son el contexto compartido.
  Guía de uso sin acceso automático al repositorio: [Asistentes](docs/asistentes.md).

## Estructura y contrato

- `backend/`: API REST Java 25 + Spring Boot 4, Maven, PostgreSQL en Supabase,
  Flyway, Spring Security + JWT propio y despliegue Docker en Render.
- `frontend/`: aplicación React 19 + TypeScript + Vite. Actualmente es una maqueta
  navegable con datos de demostración, sin autenticación, HTTP ni persistencia.
- `backend/docs/openapi.yaml`: contrato estático exportado desde
  `/v3/api-docs.yaml`; JSON de runtime en `/v3/api-docs`. Regenerarlo si cambia la API.
- La integración frontend-backend consume ese contrato; no duplicar DTOs ni reglas de
  negocio en el cliente.
- `docs/arquitectura/`, `docs/planificacion/`, `docs/decisiones/`: arquitectura,
  hitos y decisiones. ADR en `docs/decisiones/adr/`.
- Paquetes backend: `com.ztech.crm.<modulo>.{controller,service,domain,dto,repository,mapper}`.
  Lo transversal vive en `com.ztech.crm.shared`. Ver
  [ADR-001](docs/decisiones/adr/ADR-001-estructura-de-paquetes.md).

## Reglas de implementación

1. Validar reglas de negocio y permisos en el backend.
2. Controllers: HTTP ↔ DTO y delegación a service; nunca entidades JPA ni repositorios.
3. Services: transacciones, autorización y coordinación. Un módulo consume sólo el
   service de otro, nunca su repository, domain ni DTOs internos. Sin ciclos:
   `activities → opportunities → {customers, offerings, catalogs} → access → tenancy`.
4. Toda entidad de negocio lleva `tenant_id`; se resuelve del usuario autenticado,
   nunca del request. Filtrar lecturas y escrituras por tenant.
5. Historial de etapas append-only: cambio de etapa e historial en una transacción.
6. Baja lógica para registros con historia comercial; sin borrado físico.
7. Contraseñas con BCrypt/PasswordEncoder; sin texto plano ni cifrado reversible.
8. Flyway gobierna el esquema; migraciones aplicadas inmutables, correcciones en una
   versión nueva. Nunca `ddl-auto=update`.
9. Errores sin stacktraces ni datos de otro tenant; secretos por variables de entorno.
10. Cada corte vertical backend incluye migración, dominio, service, controller y
    pruebas pertinentes. No presentar funcionalidad planificada como ya implementada.
11. En frontend, archivos, componentes, props, variables y textos de interfaz se
    escriben en español, con la terminología del dominio.
12. El frontend usa CSS Modules y los tokens de
    `frontend/src/shared/styles/tokens.css`; no hardcodear colores, espaciados ni
    tipografías. Los iconos se agregan a `shared/components/Icono.tsx`.
13. Toda página pública renderiza `<Seo />`; las pantallas bajo `/app` lo renderizan
    con `noIndexar`. Consultar `frontend/README.md` para las convenciones completas.

## Comandos y validación

Desde `backend/`, con JDK 25 activo. Para levantar la API, configurar `.env` según
[README del backend](backend/README.md).

| Acción | PowerShell (Windows) | Bash |
|---|---|---|
| Unitarios (Surefire) | `./mvnw.cmd test` | `./mvnw test` |
| Build y toda la suite (incluye Failsafe) | `./mvnw.cmd clean verify` | `./mvnw clean verify` |
| API local, carga `.env` y perfil `local` | `./run.ps1` | `./run.sh` |
| API en Docker contra Supabase | `docker compose up --build` | `docker compose up --build` |

Desde `frontend/`:

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # chequeo de tipos y build de producción
npm run preview
npm run lint     # oxlint
```

`spring-boot:run` directo requiere variables y perfil configurados previamente:
Spring Boot no carga `.env` por sí solo. Testcontainers requiere Docker Desktop
corriendo y levanta su propio PostgreSQL; no depende de Compose ni de Supabase.

Estrategia vigente de backend: JUnit + Mockito para servicios con lógica que conviene
aislar; `*IT` por HTTP real contra PostgreSQL de Testcontainers, con migraciones desde
base vacía. `test` no ejecuta los `*IT`; `verify` sí. Ver
[diseño §13](docs/specs-backend/design.md#13-testing-mapea-a-02-backend-convencionesmd).
Los tests de dominio o MockMvc se agregan cuando aportan cobertura específica.

Casos negativos obligatorios al implementar el flujo correspondiente: rol insuficiente,
acceso a otro tenant por listado y por id directo, transición de etapa inválida,
reserva superpuesta y capacidad excedida. Para reservas, probar dos confirmaciones
simultáneas: exactamente una tiene éxito y la otra recibe `409`.
Los pendientes actuales están en `docs/specs-backend/tasks.md`; no darlos por cubiertos.

Para cambios de frontend, ejecutar `npm run build`; recorrer las rutas tocadas y
confirmar que no haya errores de consola, scroll horizontal a 390 px y que cada página
pública conserve su `title`, `description` y `canonical`. Al integrar la API, agregar
las pruebas pertinentes; hoy no hay suite automatizada de frontend.

Para todo cambio, ejecutar `git diff --check` y revisar `git status --short`.
Para documentación, verificar rutas, enlaces y coherencia de instrucciones.
Informar qué validaciones se ejecutaron y sus límites; no atribuirse los resultados
históricos de otra sesión.

## Estilo y mantenimiento

Código backend, clases, entidades, tablas y columnas en inglés; UI, mensajes al usuario
y documentación en español. Tablas en plural y `snake_case`, columnas en `snake_case`,
clases en `PascalCase`. Preservar terminología de dominio e IDs
(`RF-01`, `RN-01`, `CA-01`, `BE-*`, `DP-*`, `DT-*`, `ADR-*`); no renumerarlos.
Markdown conciso, encabezados ATX y nombres de archivo descriptivos.

Actualizar documentación, contexto y specs afectados en el mismo cambio que modifique
un límite modular, contrato, tecnología, regla crítica o hito. Registrar decisiones
arquitectónicas irreversibles o costosas en un ADR antes de implementarlas.
Actualizar este archivo si cambian instrucciones comunes; mantener `CLAUDE.md` como
entrada breve, sin duplicar reglas.

## Git y pull requests

**No hacer commits ni pushes salvo pedido explícito del usuario.** Esta preferencia
ya fue establecida para el proyecto; las convenciones siguientes no autorizan esas acciones.

Cuando el usuario lo solicite: commits chicos, imperativos y con scope, por ejemplo
`feat(opportunities): registrar historial de etapas`,
`feat(frontend): integrar login` o `docs: registrar ADR-003`.
Los PR explican el cambio y sus IDs de requisito, validación realizada, issue relacionado
y decisiones abiertas o cambios de alcance.
