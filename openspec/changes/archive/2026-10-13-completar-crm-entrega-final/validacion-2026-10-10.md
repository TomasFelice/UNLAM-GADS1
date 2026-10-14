# Validación propia — §9, 10/10/2026

Continuación solicitada hasta completar §9; §10 y §11 quedan fuera de esta sesión.
Se preservaron los cambios existentes. Sin commits, staging, ramas, pushes, tags,
deploy, archive ni sync. No se usaron `.env` ni bases remotas.

## Implementación

- Contexto autorizado de oportunidad mediante `OpportunityContext`, record de service;
  activities no consume entidades/DTOs internos ajenos. `Role` conserva la excepción
  transversal de las convenciones. Clientes validan escrituras por asignación directa.
- POST append-only: tipo activo, fecha real no futura, descripción/resultado obligatorios,
  tenant/actor/registro del servidor. Deriva clientes de oportunidad y empresa de contacto;
  rechaza contradicciones. Oportunidades cerradas admiten hechos sin modificar su estado.
- Con oportunidad, SELLER sólo lee sus hechos; compartir cliente no expone oportunidades
  ajenas. Hechos generales usan el alcance de cliente. Referencias ajenas devuelven 404.
- Alta en los tres detalles, conservación de campos ante error, cancelación sin escritura,
  bloqueo de envío pendiente, invalidación de vistas y foco devuelto al botón de alta.
- Actividades, oportunidades de ambos clientes y contactos de empresa navegables por
  páginas. Desempates por ID; V5 indexa cronología por asociación. Se distinguen hecho
  y registro. Bajas lógicas, tipos desactivados y autores inactivos conservan historia.
- Historial de etapas con reintento, etapa inicial/transiciones/notas/autores y datos
  originales de ambos cierres. Formato de hora explícito de Buenos Aires.
- Contrato exportado desde runtime por `OpenApiExportIT`, tipos regenerados, colección
  Postman, README, requisitos/diseño y diagrama de componentes actualizados.

## Validaciones ejecutadas

- JDK 25; Docker Desktop iniciado para PostgreSQL 16 efímero de Testcontainers.
  Primera corrida falló antes de los IT porque Docker estaba apagado; las cinco
  unitarias pasaron. Se resolvió el entorno y se ejecutó la suite completa.
- `./mvnw.cmd clean verify`: **5 unitarias + 106 IT**, cero fallos, errores u omitidos.
  Incluye instalación V1–V5, migraciones incrementales, reservas concurrentes y siete
  pruebas de `ActivityRegistrationIT`: asociaciones/fechas/tipos, alcance de tres roles
  y dos tenants, cliente compartido, paginación con empates, autores inactivos y bajas.
- Después de agregar el negativo explícito de lectura de oportunidad del segundo tenant:
  `./mvnw.cmd verify -Dit.test=ActivityRegistrationIT`: **5 unitarias + 7 IT** correctas.
- `npm test`: **36 pruebas** en 13 archivos correctas. Cuatro nuevas verifican páginas,
  validación/recuperación de formulario, asociaciones, invalidación, ID inválido e
  historial con reintento y dos cierres. `npm run lint`: sin advertencias.
- Build integrado con `VITE_API_URL=http://localhost:18080/api/v1` correcto; el aviso
  de bundle superior a 500 kB permanece para el corte de rendimiento (§10).

- `npm run test:e2e`: **20/20 correctos**. Incluye alta de actividades por los tres
  roles desde los tres detalles, oportunidad cerrada, misma interacción sin duplicarse
  en sus historias, 30 hechos, 26 oportunidades y 26 contactos del mismo cliente con
  páginas posteriores y bajas. Verifica 390/1440 px, ambos temas, teclado, ausencia de
  errores de consola/scroll horizontal y noindex. La regresión de páginas públicas
  conserva title/description/canonical. Ciclo alta→cambio→ganada→reapertura→perdida,
  con autores, notas y los dos cierres; colección Postman completa compatible.
- Primer intento E2E coincidió con recompilación de clases de test y no encontró el
  launcher. Se repitió después de finalizar Maven. Primera regresión ejecutada:
  19 correctos y un error en Postman por sort=id no admitido. Se corrigió a createdAt
  y la repetición completa pasó 20/20. No se omitieron pruebas para cerrar el corte.
- Revisión visual de capturas de formulario en escritorio y móvil oscuro. La captura
  móvil espera que termine la transición del menú tras redimensionar; repetición de
  los **dos E2E de actividades** correcta tras ese ajuste de verificación.
- `npm run build` habitual restaurado después de cerrar E2E; `npm run lint` sin avisos.
  `openspec validate completar-crm-entrega-final --strict` correcto. Enlaces locales,
  `git diff --check` y `git status --short` revisados; §10/§11 permanecen pendientes.

Estado final de la solicitud: §9.1–§9.7 completados, **58/72 tareas**. La validación
es local con infraestructura efímera; no demuestra deploy ni operación en ambiente
publicado. El próximo corte es §10, sin iniciarlo en esta sesión.
