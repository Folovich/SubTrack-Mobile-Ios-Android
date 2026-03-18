$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$changedFilesReportScript = Join-Path $PSScriptRoot "generate-changed-files-report.ps1"
$openApiBaselineSyncCheck = Join-Path $PSScriptRoot "check-openapi-baseline-sync.ps1"
$openApiCheck = Join-Path $PSScriptRoot "check-openapi-breaking.ps1"
$changedFilesReportOutput = Join-Path ([System.IO.Path]::GetTempPath()) "subtrack-changed-files-report.json"

Write-Host "Step 1/4: Changed files source-of-truth report"
& $changedFilesReportScript -OutputPath $changedFilesReportOutput | Out-Null
if ($LASTEXITCODE -ne 0) {
    exit 1
}
Write-Host "Changed files report path: $changedFilesReportOutput"

Write-Host "Step 2/4: OpenAPI baseline sync check"
& $openApiBaselineSyncCheck
if (-not $?) {
    exit 1
}

Write-Host "Step 3/4: OpenAPI breaking diff check"
& $openApiCheck
if ($LASTEXITCODE -ne 0) {
    exit 1
}

Write-Host "Step 4/4: Backend API contract tests"
Push-Location (Join-Path $repoRoot "apps/backend")
try {
    $gradleCommand = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "./gradlew" }
    & $gradleCommand test --tests "*ContractTest" --rerun-tasks
    $gradleExit = $LASTEXITCODE
} finally {
    Pop-Location
}

exit $gradleExit
