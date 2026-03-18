# Demo Checklist

Prepare test user and JWT token.
Run backend, web and mobile.
Run import smoke script: `docs/demo-script/import-smoke.md`.
Verify all import outcomes:
- `COMPLETED` (success payload)
- `COMPLETED_WITH_ERRORS` (partial payload)
- `FAILED` (fail payload)
- repeated success payload is skipped (idempotent replay)
