param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")
$healthUrl = "$base/v3/api-docs"
$callbackUrl = "$base/api/v1/integrations/GMAIL/oauth/callback?error=access_denied"

Write-Host "[1/2] Checking backend health at $healthUrl"
curl.exe --silent --show-error --fail $healthUrl | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "FAIL: backend health check failed at $healthUrl."
    exit 1
}

Write-Host "[2/2] Checking Gmail mailbox flow flag via callback endpoint"
$headersFile = [System.IO.Path]::GetTempFileName()
$bodyFile = [System.IO.Path]::GetTempFileName()

try {
    $statusCode = curl.exe --silent --show-error --output $bodyFile --dump-header $headersFile --write-out "%{http_code}" $callbackUrl
    if ($LASTEXITCODE -ne 0) {
        Write-Error "FAIL: callback probe failed for $callbackUrl."
        exit 1
    }
    $body = Get-Content $bodyFile -Raw

    if ($body -match "mailbox import flow is disabled by feature flag") {
        Write-Error "FAIL: Gmail mailbox flow is disabled by feature flag."
        Write-Host $body
        exit 1
    }

    if ($statusCode -in @("302", "303", "307", "308")) {
        Write-Host "OK: Gmail mailbox flow flag is enabled (HTTP $statusCode)."
        $location = Get-Content $headersFile | Where-Object { $_ -match "^[Ll]ocation:" } | Select-Object -Last 1
        if ($location) {
            $locationValue = ($location -replace "^[Ll]ocation:\\s*", "").Trim()
            Write-Host "Redirect target: $locationValue"
        }
        exit 0
    }

    Write-Error "FAIL: Unexpected callback response HTTP $statusCode."
    Write-Host $body
    exit 1
}
finally {
    Remove-Item -ErrorAction SilentlyContinue $headersFile, $bodyFile
}
