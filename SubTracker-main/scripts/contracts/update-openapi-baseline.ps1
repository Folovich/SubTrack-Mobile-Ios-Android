param(
    [switch]$ConfirmContractChange
)

$ErrorActionPreference = "Stop"

if (-not $ConfirmContractChange) {
    [Console]::Error.WriteLine("Baseline update is blocked. Re-run with -ConfirmContractChange after reviewing contract changes.")
    exit 2
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$currentSpec = Join-Path $repoRoot "docs/api-notes/subtrack-openapi.yaml"
$baselineSpec = Join-Path $repoRoot "docs/api-notes/baseline/subtrack-openapi.baseline.yaml"

if (-not (Test-Path $currentSpec)) {
    [Console]::Error.WriteLine("Current OpenAPI file is missing: $currentSpec")
    exit 2
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $baselineSpec) | Out-Null
Copy-Item -Path $currentSpec -Destination $baselineSpec -Force

Write-Host "OpenAPI baseline updated:"
Write-Host "  source: $currentSpec"
Write-Host "  target: $baselineSpec"
Write-Host "Commit this change only when contract update is intentional."
