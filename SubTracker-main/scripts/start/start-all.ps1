$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir "..\\setup\\bootstrap-env.ps1")
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\\..")

Write-Host "Starting SubTrack stack..."
docker compose -f (Join-Path $repoRoot "infra\\docker-compose.yml") up -d
