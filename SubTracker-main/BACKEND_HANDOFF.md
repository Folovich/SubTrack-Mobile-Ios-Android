# BACKEND_HANDOFF

## 1) Backend Scope & Ownership
- Scope: `apps/backend` (Spring Boot API, security, import/consent flow, persistence, OpenAPI contract in `docs/api-notes/subtrack-openapi.yaml`).
- Backend owner responsibility in handoff mode: keep API contract, tests, and docs consistent so web/mobile can self-serve.
- Out of scope for MVP handoff: external OAuth provider onboarding and non-GMAIL import providers.

## 2) Stable API Surface
- Source of truth: `docs/api-notes/subtrack-openapi.yaml`.
- Key endpoint groups:
  - Auth: `/api/v1/auth/*`
  - Users/Dashboard/Analytics: `/api/v1/users/me`, `/api/v1/dashboard`, `/api/v1/analytics/*`
  - Subscriptions/Categories/Notifications: `/api/v1/subscriptions*`, `/api/v1/categories`, `/api/v1/notifications`
  - Import/Consent: `/api/v1/consents/imports/*`, `/api/v1/imports*`
  - Integrations/Recommendations: `/api/v1/integrations`, `/api/v1/recommendations`
  - Forecast compatibility alias: `/api/v1/forecast` (deprecated)

## 3) Auth Flow
- Register: `POST /api/v1/auth/register`
  - Request: `{ "email": "<email>", "password": "<min 6>" }`
  - Response `201`: `{ "token": "<jwt>", "user": { "id", "email", "createdAt" } }`
- Login: `POST /api/v1/auth/login`
  - Request: `{ "email": "<email>", "password": "<password>" }`
  - Response `200`: same shape as register.
- Token usage:
  - Header: `Authorization: Bearer <jwt>`
  - Required for all `/api/v1/**` except `/api/v1/auth/**`.
- Typical errors:
  - `400` validation/business errors (`message`, optional `errors` map)
  - `401` invalid credentials (`{ "message": "Invalid credentials" }`)

## 4) Import + Consent Flow
- Consent endpoints (stable v1 contract supports only `GMAIL`):
  - `GET /api/v1/consents/imports/{provider}` -> status (`GRANTED|REVOKED|NOT_GRANTED`)
  - `POST /api/v1/consents/imports/{provider}/grant`
  - `POST /api/v1/consents/imports/{provider}/revoke`
- Provider availability is controlled by feature flags:
  - `import.providers.gmail.enabled`
  - `import.providers.yandex.enabled`
  - `import.providers.mail-ru.enabled`
  - `import.providers.bank-api.enabled`
- Default flag matrix: `gmail=true`, `yandex=false`, `mail-ru=false`, `bank-api=false`.
- Import start:
  - `POST /api/v1/imports/start` with `{ provider, messages[] }`
  - Requires active consent for provider.
- Error contract for missing consent:
  - `403` with stable payload:
  - `{ "code": "IMPORT_CONSENT_REQUIRED", "message": "...", "provider": "GMAIL" }`
- Idempotency:
  - Duplicate `externalId` is skipped (no duplicate subscription creation).

## 5) Deprecated/Compatibility Endpoints
- `GET /api/v1/forecast` is compatibility alias for analytics forecast and marked deprecated.
- Query param `months` on `/api/v1/forecast` is deprecated and ignored.

## 6) MVP Limitations
- Default runtime behavior keeps MVP compatibility: only `GMAIL` is enabled out of the box.
- Non-GMAIL providers are scaffolded behind flags for internal rollout; stable v1 OpenAPI contract remains GMAIL-only.
- Parser supports deterministic templates for Netflix, Spotify, YouTube Premium.
- Import is synchronous MVP flow (`POST /imports/start` returns final result in same request).
- No external OAuth flow in backend MVP (consent persisted internally).

## 7) Local Runbook
1. Start PostgreSQL:
   - `docker compose -f infra/docker-compose.yml up -d postgres`
2. Run migrations:
   - `powershell -ExecutionPolicy Bypass -File scripts/db/run-migrations.ps1`
3. Start backend:
   - `cd apps/backend`
   - `.\gradlew.bat bootRun`
4. Health check:
   - `powershell -ExecutionPolicy Bypass -File scripts/utils/health-check.ps1`
5. Auth smoke:
   - `POST /api/v1/auth/register`, then `POST /api/v1/auth/login`
6. Import smoke:
   - follow `docs/demo-script/import-smoke.md` exactly (includes consent grant/revoke checks).

## 8) Contract Change Policy
- Any API behavior/path/schema change must update `docs/api-notes/subtrack-openapi.yaml` in same PR.
- Same PR must update/extend backend tests for changed behavior.
- Same PR must update demo docs (`docs/demo-script/*`, `docs/api-notes/endpoints.md`) when external behavior changes.
- Silent breaking changes are prohibited; if breaking change is unavoidable, add deprecation window + migration note.
- Breaking-check is enforced by `scripts/contracts/check-openapi-breaking.ps1` against `docs/api-notes/baseline/subtrack-openapi.baseline.yaml`.
- Baseline updates are allowed only via explicit command with confirmation flag:
  - `powershell -ExecutionPolicy Bypass -File scripts/contracts/update-openapi-baseline.ps1 -ConfirmContractChange`
- Full local contract guard command:
  - `powershell -ExecutionPolicy Bypass -File scripts/contracts/run-contract-checks.ps1`
- Detailed rules: `docs/api-notes/contract-guard.md`.

## 9) Definition of Done for API changes
- Code + OpenAPI + notes/docs updated and consistent.
- Required tests added/updated and passing (`cd apps/backend && ./gradlew.bat test --rerun-tasks`).
- Smoke scenario still executable from docs without author assistance.
- Backward compatibility/deprecation status explicitly documented.

## 10) Bus-Factor Checklist
- New developer can run DB, migrations, backend, and health-check using documented commands only.
- New developer can execute auth and import smoke flow from docs without asking backend author.
- New developer can find stable contract and error codes in OpenAPI and handoff doc.
- New developer can implement small API change following policy and DoD without hidden tribal knowledge.
