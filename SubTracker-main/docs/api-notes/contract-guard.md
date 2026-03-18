# API Contract Guard

OpenAPI contract baseline:
- Current spec: `docs/api-notes/subtrack-openapi.yaml`
- Baseline snapshot: `docs/api-notes/baseline/subtrack-openapi.baseline.yaml`

## Local checks

Run full contract guard (changed-files source-of-truth + OpenAPI checks + backend contract tests):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/run-contract-checks.ps1
```

Run only OpenAPI breaking diff:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/check-openapi-breaking.ps1
```

Generate machine-readable changed-files report (single source of truth for audit/reporting):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/generate-changed-files-report.ps1
```

Optional output file:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/generate-changed-files-report.ps1 -OutputPath .\tmp\changed-files-report.json
```

Check OpenAPI baseline sync only:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/check-openapi-baseline-sync.ps1
```

## Baseline update rule

Update baseline only when contract change is intentional and documented:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/contracts/update-openapi-baseline.ps1 -ConfirmContractChange
```

Required process for baseline update:
1. Update `docs/api-notes/subtrack-openapi.yaml` in the same PR.
2. Update/add contract tests for changed behavior.
3. Update external API docs/notes for consumers.
4. Commit baseline file change explicitly.

## Reporting evidence policy

- Changed files and counts in reports must be taken from `generate-changed-files-report.ps1` output only.
- Report references must be `path-only` (no manual `:line` references).
- The changed-files script validates that every reported path exists (except files explicitly marked as deleted in git status), which prevents broken file references.

## What is considered breaking (blocked by check)

- Removing existing path or HTTP method.
- Removing response status code for existing operation.
- Tightening request/response schema in a backward-incompatible way (per `openapi-diff` incompatible state).

Compatible additions (for example, additive fields/endpoints) are allowed and do not fail the breaking check.
