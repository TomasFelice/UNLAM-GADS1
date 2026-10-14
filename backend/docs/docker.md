# Docker

## Build de la imagen

```powershell
cd backend
docker build -t crm-backend .
```

Build multietapa (`Dockerfile`): compila con `maven:3.9-eclipse-temurin-25` y empaqueta
el resultado sobre `eclipse-temurin:25-jre-alpine`. La imagen final no incluye Maven ni
el código fuente, sólo el jar y el JRE.

## Levantar con Docker Compose

1. Copiar `.env.example` a `.env` y completar los valores reales (nunca commitear `.env`).
2. `docker compose up --build`

`docker-compose.yml` levanta un único servicio (`api`) contra **Supabase** — no hay un
Postgres local en el compose, porque la base del proyecto es Supabase (ver
[ADR-002](../../docs/decisiones/adr/ADR-002-plataforma-supabase-render.md)). El mismo
`.env` sirve para sustituir variables en el propio `docker-compose.yml` (Compose lee un
archivo `.env` en el directorio automáticamente) y para inyectarlas al contenedor
(`env_file`).

## Variables de entorno obligatorias

Ver la tabla completa en [`../README.md`](../README.md#variables-de-entorno). Como
mínimo hacen falta `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` para que la
aplicación arranque — sin ellas, Spring falla al crear el `DataSource`/`JwtService` con
un error claro en el log, no un fallo silencioso.

## Conexión a Supabase: directa vs. pooler

- **Render** (donde se despliega la API): usar el **pooler Supavisor en modo session**
  — la salida de Render no tiene IPv6, y el endpoint directo de Supabase sí lo requiere
  (DT-07).
- **Local, fuera de Docker** (`./mvnw spring-boot:run`): la conexión directa funciona si
  la red del desarrollador tiene IPv6 real (verificado el 12/09/2026).
- **Local, con Docker Compose**: usar el pooler también — la red virtual de Docker
  Desktop es IPv4-only incluso si el host tiene IPv6, así que la conexión directa a
  Supabase falla dentro del contenedor (`UnknownHostException` o "Address not
  available", según el punto exacto de la resolución DNS/conexión).

**Confirmado con `docker compose up --build` real (12/09/2026)**: con `DB_URL` apuntando
al pooler (`aws-0-<región>.pooler.supabase.com:5432`, usuario `postgres.<project-ref>`),
el contenedor levanta completo — Flyway migra, `/actuator/health` responde `UP` y el
login devuelve un JWT válido. No es sólo la recomendación teórica de DT-07.

## Tests de integración no usan Docker Compose

Los tests `*IT` (Testcontainers) levantan su propio contenedor de PostgreSQL efímero
por corrida — sólo necesitan el **daemon de Docker Desktop corriendo**, nada de este
`docker-compose.yml`. Ver `../README.md` para correrlos.

## Verificación local de entrega — 10/10/2026

§11 construyó la imagen multietapa y arrancó una copia de la configuración Compose
con conexión/puerto de prueba y PostgreSQL efímero: health UP, login semilla con
cambio obligatorio, V1–V5 aplicadas y exclusión GiST presente. Los recursos se cerraron
al finalizar. Esto verifica el empaquetado y arranque; la conexión a Supabase y el
ambiente publicado siguen sujetos al ensayo manual del usuario.
Detalle en la [validación integrada](../../openspec/changes/archive/2026-10-13-completar-crm-entrega-final/validacion-punto-11-2026-10-10.md).
