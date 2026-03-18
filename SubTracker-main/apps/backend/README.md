# Backend

Spring Boot backend for SubTrack.

## Local startup (MVP)

1. Start PostgreSQL:
   - `docker compose -f apps/backend/docker-compose.yml up -d`
2. Bootstrap environment files (recommended):
   - `bash scripts/setup/bootstrap-env.sh`
   - or `powershell -ExecutionPolicy Bypass -File scripts/setup/bootstrap-env.ps1`
3. Configure environment variables (use `apps/backend/.env.example` as a template).
   - Backend automatically loads `.env` from `apps/backend/.env` and repository root `.env` if files exist.
   - `SPRING_PROFILES_ACTIVE=dev`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subtrack`
   - `SPRING_DATASOURCE_USERNAME=subtrack`
   - `SPRING_DATASOURCE_PASSWORD=subtrack`
   - `JWT_SECRET` must be at least 32 bytes for HS256.
   - Login protection defaults (override in env when needed):
     - `APP_SECURITY_LOGIN_RATE_LIMIT_MAX_ATTEMPTS=20`
     - `APP_SECURITY_LOGIN_RATE_LIMIT_WINDOW=1m`
     - `APP_SECURITY_LOGIN_BRUTE_FORCE_MAX_FAILURES=5`
     - `APP_SECURITY_LOGIN_BRUTE_FORCE_COOLDOWN=15m`
     - `APP_SECURITY_LOGIN_BRUTE_FORCE_ENTRY_TTL=24h`
   - Registration password policy: 10-72 chars, at least 1 lowercase, 1 uppercase, 1 digit, 1 special char, no spaces.
   - Enable Gmail mailbox flow with `IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED=true`.
   - Keep Gmail provider enabled with `IMPORT_PROVIDERS_GMAIL_ENABLED=true`.
   - Keep `IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED=false` in `dev` profile.
   - Configure `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REDIRECT_URI`, and `GMAIL_FRONTEND_REDIRECT_URI`.
   - Configure `INTEGRATIONS_TOKEN_ENCRYPTION_SECRET` and `INTEGRATIONS_OAUTH_STATE_SECRET` (or ensure `JWT_SECRET` is set, because these values fallback to it).
   - In `dev`, backend starts even with empty Gmail OAuth client credentials, but OAuth start endpoint returns a clear configuration error until values are set.
   - Keep OAuth state cleanup enabled in production (`INTEGRATIONS_OAUTH_STATE_CLEANUP_ENABLED=true`), otherwise backend startup is blocked in `prod` profile.
4. Run backend:
   - `cd apps/backend`
   - `./gradlew bootRun` (Linux/macOS)
   - `gradlew.bat bootRun` (Windows)

Auth API behavior note:
- `POST /api/v1/auth/login` can return `429 Too Many Requests` with `Retry-After` when IP rate-limit or brute-force lock is triggered.

Support email draft flow:
- `GET /api/v1/subscriptions/{id}/support-email-draft?action=CANCEL|PAUSE`
- `POST /api/v1/subscriptions/{id}/support-email-events`
- New endpoints are backward-compatible and do not mutate subscription status automatically.

## Gmail mailbox flow quick validation

- Feature flag check:
  - `bash scripts/utils/check-gmail-import.sh http://localhost:8080`
- Real OAuth check (requires real Google OAuth credentials):
  - authenticate in app;
  - call `POST /api/v1/integrations/GMAIL/oauth/start`;
  - complete Google consent;
  - verify `GET /api/v1/integrations/GMAIL` is not `NOT_CONNECTED`.

## Stop local PostgreSQL

- `docker compose -f apps/backend/docker-compose.yml down`
