param(
    [string]$BaselinePath = "docs/api-notes/baseline/subtrack-openapi.baseline.yaml",
    [string]$CurrentPath = "docs/api-notes/subtrack-openapi.yaml"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$baselineFullPath = Join-Path $repoRoot $BaselinePath
$currentFullPath = Join-Path $repoRoot $CurrentPath

if (-not (Test-Path $baselineFullPath)) {
    [Console]::Error.WriteLine("Baseline file is missing: $baselineFullPath")
    exit 2
}

if (-not (Test-Path $currentFullPath)) {
    [Console]::Error.WriteLine("Current OpenAPI file is missing: $currentFullPath")
    exit 2
}

$baselineHash = (Get-FileHash -Path $baselineFullPath -Algorithm SHA256).Hash.ToLowerInvariant()
$currentHash = (Get-FileHash -Path $currentFullPath -Algorithm SHA256).Hash.ToLowerInvariant()

if ($baselineHash -ne $currentHash) {
    [Console]::Error.WriteLine("OpenAPI spec and baseline are out of sync.")
    [Console]::Error.WriteLine("Run scripts/contracts/update-openapi-baseline.ps1 -ConfirmContractChange if the contract change is intentional.")
    exit 1
}

Write-Host "OpenAPI baseline is in sync with current spec."
