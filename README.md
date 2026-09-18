# UNLAM-GADS1

CRM especializado en la gestión comercial de **salones de eventos corporativos**, desarrollado como trabajo práctico de Gestión Aplicada al Desarrollo de Software II, Ingeniería en Informática, Universidad Nacional de La Matanza.

## Estado

| Entrega | Fecha | Estado |
|---|---|---|
| Integración frontend/backend | 24/09/2026 | Implementada; validación manual desplegada pendiente |
| Entrega 1 | 24/09/2026 | Backend y frontend integrados; recorrido desplegado pendiente |
| Entrega final | 12/11/2026 | Pendiente |

## Estructura del repositorio

```text
UNLAM-GADS1/
├── frontend/   React + TypeScript + Vite; autenticación y pantallas comerciales integradas
├── backend/    API Java 25 + Spring Boot + PostgreSQL/Flyway
└── docs/       Arquitectura, planificación, decisiones y archivos de marca
```

## Cómo levantar el frontend

```bash
cd frontend
npm install
npm run dev
```

La API debe estar disponible en `VITE_API_URL`; en desarrollo se usa
`http://localhost:8080/api/v1` si la variable no está definida.

Detalle en [`frontend/README.md`](frontend/README.md).

## Documentación

- [Arquitectura, planificación y decisiones técnicas](docs/README.md)
- [Instrucciones compartidas para asistentes](AGENTS.md)
- [Cómo usar el contexto con Claude, Codex y ChatGPT/API](docs/asistentes.md)
- [Plan de la maqueta de frontend](docs/planificacion/plan-frontend-maqueta.md)
- [Decisiones abiertas del frontend](docs/decisiones/decisiones-frontend.md)
