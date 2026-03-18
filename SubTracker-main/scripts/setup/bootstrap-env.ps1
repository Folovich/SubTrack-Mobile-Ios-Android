$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\\..")

function Copy-IfMissing {
    param(
        [Parameter(Mandatory = $true)][string]$SourceFile,
        [Parameter(Mandatory = $true)][string]$TargetFile
    )

    if (Test-Path $TargetFile) {
        Write-Host "[ok] $TargetFile already exists"
        return
    }

    Copy-Item -Path $SourceFile -Destination $TargetFile
    Write-Host "[created] $TargetFile from $SourceFile"
}

function Set-KeyValue {
    param(
        [Parameter(Mandatory = $true)][string]$EnvFile,
        [Parameter(Mandatory = $true)][string]$Key,
        [Parameter(Mandatory = $true)][string]$Value
    )

    if (-not (Test-Path $EnvFile)) {
        New-Item -ItemType File -Path $EnvFile -Force | Out-Null
    }

    $lines = @(Get-Content $EnvFile -ErrorAction SilentlyContinue)
    $match = "^" + [regex]::Escape($Key) + "="
    $updated = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $match) {
            $lines[$i] = "$Key=$Value"
            $updated = $true
        }
    }

    if (-not $updated) {
        $lines += "$Key=$Value"
    }

    Set-Content -Path $EnvFile -Value $lines
}

function Get-EnvValue {
    param(
        [Parameter(Mandatory = $true)][string]$Key,
        [Parameter(Mandatory = $true)][string[]]$Files
    )

    foreach ($file in $Files) {
        if (-not (Test-Path $file)) {
            continue
        }

        $line = Get-Content $file | Where-Object { $_ -like "$Key=*" } | Select-Object -Last 1
        if ($null -ne $line -and $line.Length -gt ($Key.Length + 1)) {
            return $line.Substring($Key.Length + 1)
        }
        if ($line -eq "$Key=") {
            return ""
        }
    }

    return ""
}

$rootEnv = Join-Path $repoRoot ".env"
$backendEnv = Join-Path $repoRoot "apps\\backend\\.env"
$webEnv = Join-Path $repoRoot "apps\\web\\.env"
$mobileEnv = Join-Path $repoRoot "apps\\mobile\\.env"

Copy-IfMissing (Join-Path $repoRoot ".env.example") $rootEnv
Copy-IfMissing (Join-Path $repoRoot "apps\\backend\\.env.example") $backendEnv
Copy-IfMissing (Join-Path $repoRoot "apps\\web\\.env.example") $webEnv
Copy-IfMissing (Join-Path $repoRoot "apps\\mobile\\.env.example") $mobileEnv

foreach ($file in @($rootEnv, $backendEnv)) {
    Set-KeyValue -EnvFile $file -Key "IMPORT_PROVIDERS_GMAIL_ENABLED" -Value "true"
    Set-KeyValue -EnvFile $file -Key "IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED" -Value "true"
    Set-KeyValue -EnvFile $file -Key "IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED" -Value "false"
}

Write-Host "----- Gmail mailbox defaults -----"
Select-String -Path $rootEnv, $backendEnv -Pattern "^(IMPORT_PROVIDERS_GMAIL_ENABLED|IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED|IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED)=" | ForEach-Object {
    Write-Host $_.Line
}

$lookupFiles = @($backendEnv, $rootEnv)
$clientId = Get-EnvValue -Key "GMAIL_CLIENT_ID" -Files $lookupFiles
$clientSecret = Get-EnvValue -Key "GMAIL_CLIENT_SECRET" -Files $lookupFiles
Write-Host "----- Gmail OAuth required secrets -----"
Write-Host ("GMAIL_CLIENT_ID={0}" -f ($(if ([string]::IsNullOrWhiteSpace($clientId)) { "EMPTY" } else { "SET" })))
Write-Host ("GMAIL_CLIENT_SECRET={0}" -f ($(if ([string]::IsNullOrWhiteSpace($clientSecret)) { "EMPTY" } else { "SET" })))
Write-Host "If OAuth secrets are EMPTY, set them in apps/backend/.env (or root .env) before real Gmail sync."
