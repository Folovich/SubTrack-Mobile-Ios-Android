# Endpoints

API endpoint notes for SubTrack.
List auth, subscriptions, categories and analytics endpoints.
Keep this file in sync with OpenAPI.

Contract guard checks and baseline policy: `docs/api-notes/contract-guard.md`.

## Auth endpoints
- `POST /api/v1/auth/register` returns `201` with JWT payload.
- `POST /api/v1/auth/login` returns `200` with JWT payload.
- Auth validation/business errors return `400`, invalid credentials return `401`.
- `POST /api/v1/auth/login` may return `429` with `Retry-After` when login rate-limit/lockout is active.
- Register password policy: 10-72 chars, lowercase+uppercase+digit+special required, spaces are forbidden.

## Forecast endpoints
- Canonical forecast endpoint: `GET /api/v1/analytics/forecast`.
- Compatibility alias: `GET /api/v1/forecast` (deprecated).
- `months` query parameter on `/api/v1/forecast` is deprecated and ignored.

## Notification endpoint
- Existing endpoint is unchanged: `GET /api/v1/notifications?days=<1..365>`.
- Response shape is unchanged (`id`, `type`, `message`, `scheduledAt`, `status`).
- Supported `type` values: `UPCOMING_CHARGE`, `PRICE_CHANGE`, `INACTIVITY`.
- `UPCOMING_CHARGE` reminder flow (1-3 days) remains active.

## Usage analytics endpoint
- `GET /api/v1/analytics/usage?period=<month|year>&subscriptionId=<optional-id>`.
- Aggregates usage signals by active subscriptions for selected period.
- `subscriptionId` must belong to current user; invalid ownership returns `400` with `{"message":"Subscription not found"}`.

## Import consent endpoints
- Check status: `GET /api/v1/consents/imports/GMAIL`.
- Grant consent: `POST /api/v1/consents/imports/GMAIL/grant`.
- Revoke consent: `POST /api/v1/consents/imports/GMAIL/revoke`.
- Import endpoint `POST /api/v1/imports/start` requires active consent and returns `403 IMPORT_CONSENT_REQUIRED` when missing.
- Runtime scaffold flags exist for future providers (`import.providers.*.enabled`), but stable v1 external contract remains GMAIL-only.

## Usage signal endpoints
- Create usage signal: `POST /api/v1/usage-signals`.
- List usage signals: `GET /api/v1/usage-signals?subscriptionId=<id>` (optional filter).
- Signals are user-scoped: subscription must exist and belong to current user, otherwise `400` with `{"message":"Subscription not found"}`.

## Support email draft endpoints
- Build quick-cancel/quick-pause support email draft:
  - `GET /api/v1/subscriptions/{id}/support-email-draft?action=CANCEL|PAUSE`
  - response shape: `subscriptionId`, `action`, `provider`, and `draft.{to,subject,body,mailtoUrl,plainTextForCopy}`
- Track quick-support UX events:
  - `POST /api/v1/subscriptions/{id}/support-email-events`
  - request: `{ "action":"CANCEL|PAUSE", "event":"DRAFT_OPENED|MAILTO_OPENED|TEXT_COPIED" }`
- Ownership and validation behavior for new support-email endpoints:
  - `404` if subscription not found
  - `403` if subscription belongs to another user
  - `400` for invalid action/event values

## Other authenticated endpoints
- Integrations: `GET /api/v1/integrations`.
- Recommendations: `GET /api/v1/recommendations?category=<category>`.
  - Returns catalog-backed alternatives from `recommendation_catalog` (seeded by backend migrations).
