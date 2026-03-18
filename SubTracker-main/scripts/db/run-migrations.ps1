param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$TimeoutSec = 90,
    [switch]$KeepBackendRunning
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backendDir = Join-Path $repoRoot "apps\backend"
$composeFile = Join-Path $repoRoot "infra\docker-compose.yml"
$healthScript = Join-Path $repoRoot "scripts\utils\health-check.ps1"

if (-not (Test-Path $healthScript)) {
    Write-Error "Health check script not found: $healthScript"
    exit 2
}

function Test-BackendUp {
    param([string]$Url)
    try {
        $probe = $Url.TrimEnd("/") + "/v3/api-docs"
        $response = Invoke-WebRequest -Uri $probe -Method Get -TimeoutSec 3 -ErrorAction Stop
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300)
    } catch {
        return $false
    }
}

Write-Host "Ensuring PostgreSQL is running via infra compose..."
docker compose -f $composeFile up -d postgres | Out-Host
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start postgres container with $composeFile"
    exit 1
}

$backendWasUp = Test-BackendUp -Url $BaseUrl
$startedProcess = $null

try {
    if (-not $backendWasUp) {
        Write-Host "Backend is not running. Starting bootRun to apply Flyway migrations..."
        $startedProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "bootRun", "--no-daemon" -WorkingDirectory $backendDir -PassThru
    } else {
        Write-Host "Backend already running. Flyway migrations are applied on startup; verifying availability..."
    }

    & $healthScript -BaseUrl $BaseUrl -ProbePath "/v3/api-docs" -TimeoutSec $TimeoutSec
    if ($LASTEXITCODE -ne 0) {
        throw "Backend did not become healthy in time."
    }

    Write-Host "Flyway migrations are applied (startup verification succeeded)."
} catch {
    Write-Error $_.Exception.Message
    if ($startedProcess -and -not $startedProcess.HasExited) {
        Stop-Process -Id $startedProcess.Id -Force
    }
    exit 1
}

if ($startedProcess -and -not $KeepBackendRunning) {
    Write-Host "Stopping temporary backend process (PID $($startedProcess.Id))."
    Stop-Process -Id $startedProcess.Id -Force
    Write-Host "Done."
} elseif ($startedProcess) {
    Write-Host "Backend kept running (PID $($startedProcess.Id)) because -KeepBackendRunning was specified."
}

exit 0
