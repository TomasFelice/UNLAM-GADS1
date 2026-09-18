# Trabajar con Claude y modelos de OpenAI

El proyecto mantiene un único conjunto de instrucciones, contexto y especificaciones.
La herramienta que ejecuta el modelo determina cómo se cargan los archivos.

## Entradas y fuentes compartidas

| Archivo o carpeta | Propósito |
|---|---|
| [AGENTS.md](../AGENTS.md) | Instrucciones comunes y orden de lectura |
| [CLAUDE.md](../CLAUDE.md) | Entrada de Claude Code que importa `AGENTS.md` |
| [Contexto del proyecto](context/00-proyecto.md) | Resumen, alcance y estado registrado |
| [Producto](context/01-producto-y-consignas.md) | Consignas y reglas de negocio resumidas |
| [Convenciones del backend](context/02-backend-convenciones.md) | Capas, contratos, seguridad y pruebas |
| [Decisiones técnicas](context/03-decisiones-tecnicas.md) | Acuerdos técnicos y referencias a pendientes |
| [Requisitos](specs-backend/requirements.md), [diseño](specs-backend/design.md) y [tareas](specs-backend/tasks.md) | Especificación aprobada e historial de implementación |

Las rutas antiguas de `.claude/context/` y `.claude/specs-backend/` se conservan como
enlaces de transición. El contenido se edita en `docs/`; no mantener copias por proveedor.

## Codex

Abrir el repositorio como espacio de trabajo. Codex descubre `AGENTS.md` desde la raíz
hasta el directorio de trabajo; el archivo de este proyecto indica qué contexto adicional
leer. Esto usa la configuración estándar, sin agregar un `config.toml` ni seleccionar
un modelo concreto. Ver la [documentación oficial de AGENTS.md](https://developers.openai.com/codex/guides/agents-md).

Después de cambiar las instrucciones, iniciar una sesión nueva para comprobar su carga.
Las instrucciones globales o los archivos `AGENTS.override.md` de cada entorno también
pueden afectar la sesión; no forman parte de esta migración del repositorio.

## Claude Code

Mantener `CLAUDE.md` en la raíz: importa `AGENTS.md` y remite al mismo contexto de `docs/`.
La importación `@AGENTS.md` sigue la [documentación oficial de Claude Code](https://code.claude.com/docs/en/memory#agents-md).
Si otro cliente de Claude no interpreta la importación, pedirle que lea `AGENTS.md`
explícitamente. No depender de memorias privadas de una sesión anterior para continuar.

## ChatGPT, API u otro cliente sin lectura automática

No asumir que el modelo tiene acceso al disco o carga archivos por su nombre. Adjuntar
o proporcionar el contenido de `AGENTS.md`, `docs/context/00-proyecto.md` y
`docs/context/01-producto-y-consignas.md`. Para backend, incluir además convenciones,
decisiones técnicas y las partes pertinentes de requisitos, diseño y tareas. Proporcionar
los PDF originales cuando haga falta contrastar un requisito y el cliente pueda leerlos.

Si sólo se proporcionan extractos, identificar archivo y sección, incluyendo el estado
de las decisiones relacionadas. Sin herramientas de ejecución, los cambios y pruebas
propuestos deben distinguirse de los realmente aplicados o ejecutados.

Mensaje sugerido después de proporcionar los archivos:

```text
Trabajamos en zTech CRM (UNLAM-GADS1). Lee AGENTS.md y sigue su orden de lectura.
Mi tarea es: <describir el cambio y su alcance>.
Consulta las decisiones y tareas relacionadas antes de implementar. Conserva los IDs
de requisitos y distingue lo implementado de lo pendiente. No hagas commits ni pushes.
Al terminar, indica los cambios, las validaciones ejecutadas y lo que no pudiste verificar.
```

## Comprobar y mantener el contexto

En una sesión nueva, pedir al asistente que identifique las instrucciones leídas, el
contrato OpenAPI vigente, el siguiente pendiente relacionado con la tarea y las pruebas
que corresponden. Debe localizar `backend/docs/openapi.yaml`, reconocer la restricción
de commits/pushes y distinguir `test` de `verify`.

Actualizar las reglas comunes en `AGENTS.md`, los conocimientos en `docs/context/` y
el avance en `docs/specs-backend/tasks.md`. Registrar las validaciones con su fecha y
alcance; los 34 tests documentados de E1 son un resultado histórico, no una nueva corrida.
