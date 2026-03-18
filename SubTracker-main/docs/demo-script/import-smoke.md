# Import Smoke Script (MVP)

## Preconditions
- Backend is running on `http://localhost:8080`
- Test user exists or can be created via `/api/v1/auth/register`
- Provider flags are default (`GMAIL=true`, `YANDEX=false`, `MAIL_RU=false`, `BANK_API=false`), so smoke flow uses `GMAIL`.

## 1) Login and get JWT
```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@subtrack.app","password":"Passw0rd!"}'
```

Copy `token` from response.

## 2) Grant consent for import provider
```bash
curl -X POST "http://localhost:8080/api/v1/consents/imports/GMAIL/grant" \
  -H "Authorization: Bearer <TOKEN>"
```

Optional negative check (import without consent):
- call `POST /api/v1/consents/imports/GMAIL/revoke`
- call `POST /api/v1/imports/start` and expect `403` with code `IMPORT_CONSENT_REQUIRED`.

## 3) Run success payload
```bash
curl -X POST "http://localhost:8080/api/v1/imports/start" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "GMAIL",
    "messages": [
      {
        "externalId": "gmail-msg-success-001",
        "from": "billing@netflix.com",
        "subject": "Netflix renewal notice",
        "body": "Your Netflix renewal is scheduled for 2026-03-20. Amount: 9.99 USD",
        "receivedAt": "2026-03-08T10:00:00Z"
      }
    ]
  }'
```

Expect:
- `status = COMPLETED`
- `created > 0`
- `errors = 0`

## 4) Run partial payload
```bash
curl -X POST "http://localhost:8080/api/v1/imports/start" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "GMAIL",
    "messages": [
      {
        "externalId": "gmail-msg-partial-001",
        "from": "billing@netflix.com",
        "subject": "Netflix renewal notice",
        "body": "Your Netflix renewal is scheduled for 2026-03-22. Amount: 9.99 USD",
        "receivedAt": "2026-03-08T11:00:00Z"
      },
      {
        "externalId": "gmail-msg-partial-002",
        "from": "billing@netflix.com",
        "subject": "Netflix renewal notice",
        "body": "Amount: 9.99 USD",
        "receivedAt": "2026-03-08T11:01:00Z"
      }
    ]
  }'
```

Expect:
- `status = COMPLETED_WITH_ERRORS`
- `created > 0`
- `errors > 0`

## 5) Run fail payload
```bash
curl -X POST "http://localhost:8080/api/v1/imports/start" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "GMAIL",
    "messages": [
      {
        "externalId": "gmail-msg-fail-001",
        "from": "no-reply@example.com",
        "subject": "Your receipt",
        "body": "Amount: 9.99 USD",
        "receivedAt": "2026-03-08T12:00:00Z"
      }
    ]
  }'
```

Expect:
- `status = FAILED`
- `created = 0`
- `errors = processed`

## 6) Idempotency check (repeat same success payload)
Send the exact same request from step 3 again.

Expect:
- second run has `skipped > 0`
- no duplicate subscription creation for same message `externalId`

## 7) Verify history/details
```bash
curl -X GET "http://localhost:8080/api/v1/imports" \
  -H "Authorization: Bearer <TOKEN>"
```

Pick one `id` and check details:
```bash
curl -X GET "http://localhost:8080/api/v1/imports/<ID>" \
  -H "Authorization: Bearer <TOKEN>"
```
