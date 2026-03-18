param(
    [string]$BaselinePath = "docs/api-notes/baseline/subtrack-openapi.baseline.yaml",
    [string]$CurrentPath = "docs/api-notes/subtrack-openapi.yaml"
)

$ErrorActionPreference = "Stop"

function Get-Sha1([string]$Path) {
    return (Get-FileHash -Path $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

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

$baselineArg = $BaselinePath.Replace("\", "/")
$currentArg = $CurrentPath.Replace("\", "/")

$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
    [Console]::Error.WriteLine("Java runtime is required for OpenAPI diff check. Install Java 21+ and retry.")
    exit 2
}

$toolVersion = "2.1.2"
$toolFile = "openapi-diff-cli-$toolVersion-all.jar"
$toolSha1 = "e40e0f485e9bfabfa37693c3c5a2a1c5d89fda87"
$toolUrl = "https://repo1.maven.org/maven2/org/openapitools/openapidiff/openapi-diff-cli/$toolVersion/$toolFile"
$toolCacheDir = Join-Path ([System.IO.Path]::GetTempPath()) "subtrack-contract-tools"
$toolPath = Join-Path $toolCacheDir $toolFile

New-Item -ItemType Directory -Force -Path $toolCacheDir | Out-Null

$shouldDownload = $true
if (Test-Path $toolPath) {
    $existingHash = Get-Sha1 -Path $toolPath
    $shouldDownload = $existingHash -ne $toolSha1
}

if ($shouldDownload) {
    Write-Host "Downloading OpenAPI diff CLI $toolVersion ..."
    Invoke-WebRequest -UseBasicParsing -Uri $toolUrl -OutFile $toolPath
    $downloadedHash = Get-Sha1 -Path $toolPath
    if ($downloadedHash -ne $toolSha1) {
        [Console]::Error.WriteLine("Downloaded tool checksum mismatch. Expected $toolSha1, got $downloadedHash.")
        exit 2
    }
}

Write-Host "Checking OpenAPI compatibility against baseline ..."
Push-Location $repoRoot
try {
    & java -jar $toolPath --fail-on-incompatible $baselineArg $currentArg
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exitCode -ne 0) {
    [Console]::Error.WriteLine("Breaking OpenAPI changes detected. Update API with backward compatibility or add deprecation/migration plan.")
    exit $exitCode
}

Write-Host "OpenAPI compatibility check passed (no breaking changes)."
