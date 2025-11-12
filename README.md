# emp_fullstack (Dockerized)

This archive contains:
- `backend/` — Spring Boot (Maven) backend (already dockerized)
- `frontend/` — Node-based frontend (Angular/React/Vue) added and Dockerfile created
- `docker-compose.yml` — to build and run backend + frontend together

## How to run

1. Build and start both services with Docker Compose:

```bash
docker compose up --build
```

This will:
- build backend from `backend/`
- build frontend from `frontend/` (production build + nginx)
- expose frontend on port 80 and backend on port 8080

## Notes & Next steps

- The frontend Dockerfile assumes the production build output is placed under `dist/`. 
  If your frontend build output is named differently (e.g., `dist/app-name`), update the Dockerfile's `COPY --from=build` path.
- `nginx.conf` proxies `/api/` requests to `http://emp-backend:8080`. Ensure your frontend calls backend with `/api/...` prefix or adjust as needed.
- Do NOT commit sensitive files (logs, uploads, .env with secrets) to git. Use environment variables or GitHub Secrets.

## MySQL (added)

This docker-compose adds a `mysql` service (MySQL 8.0).
- Database: `alok`
- Root user: `root` with password `system` (set by compose). 

The backend service is configured via environment variables to use the container `mysql`:
```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/alok?useSSL=false&serverTimezone=Asia/Kolkata
DB_USER=root
DB_PASSWORD=system
```

An init SQL (`backend/docker/mysql-init/init.sql`) creates a sample `employees` table with one row.

Run with:
```
docker compose up --build
```

