# Docker Setup

Use `infra/docker-compose.yml`.
Adjust env values in `infra/env`.
Start services with `scripts/start`.

Backend + Postgres quick start:
- `docker compose -f infra/docker-compose.yml build backend`
- `docker compose -f infra/docker-compose.yml up -d postgres backend`
- `powershell -ExecutionPolicy Bypass -File scripts/utils/health-check.ps1 -BaseUrl http://localhost:8080 -ProbePath /v3/api-docs -TimeoutSec 60`
