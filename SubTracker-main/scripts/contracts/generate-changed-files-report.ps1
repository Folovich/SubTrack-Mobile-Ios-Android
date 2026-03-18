param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

function Normalize-Lines([string[]]$Lines) {
    if ($null -eq $Lines) {
        return @()
    }
    return $Lines |
        ForEach-Object { $_.TrimEnd() } |
        Where-Object { $_ -and $_.Trim().Length -gt 0 }
}

$statusLines = Normalize-Lines (& git -C $repoRoot status --short)
$trackedDiffFiles = Normalize-Lines (& git -C $repoRoot diff --name-only)
$untrackedFiles = Normalize-Lines (& git -C $repoRoot ls-files --others --exclude-standard)

$fileMap = @{}

foreach ($line in $statusLines) {
    $statusCode = if ($line.Length -ge 2) { $line.Substring(0, 2) } else { "??" }
    $rawPath = if ($line.Length -ge 4) { $line.Substring(3).Trim() } else { "" }
    if ($rawPath -match " -> ") {
        $rawPath = ($rawPath.Split(" -> ")[-1]).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($rawPath)) {
        continue
    }
    if ($rawPath.EndsWith("/")) {
        # Untracked directories are expanded by `git ls-files --others --exclude-standard`.
        continue
    }

    if (-not $fileMap.ContainsKey($rawPath)) {
        $fileMap[$rawPath] = [ordered]@{
            path = $rawPath
            statusCodes = @()
            sources = @()
            exists = $false
        }
    }
    $fileMap[$rawPath].statusCodes += $statusCode
    if (-not ($fileMap[$rawPath].sources -contains "status")) {
        $fileMap[$rawPath].sources += "status"
    }
}

foreach ($path in $trackedDiffFiles) {
    if (-not $fileMap.ContainsKey($path)) {
        $fileMap[$path] = [ordered]@{
            path = $path
            statusCodes = @()
            sources = @()
            exists = $false
        }
    }
    if (-not ($fileMap[$path].sources -contains "diff")) {
        $fileMap[$path].sources += "diff"
    }
}

foreach ($path in $untrackedFiles) {
    if (-not $fileMap.ContainsKey($path)) {
        $fileMap[$path] = [ordered]@{
            path = $path
            statusCodes = @("??")
            sources = @()
            exists = $false
        }
    }
    if (-not ($fileMap[$path].sources -contains "untracked")) {
        $fileMap[$path].sources += "untracked"
    }
}

$invalidReferences = @()
$reportedFiles = @()

foreach ($key in ($fileMap.Keys | Sort-Object)) {
    $entry = $fileMap[$key]
    $absPath = Join-Path $repoRoot $entry.path
    $exists = Test-Path $absPath
    $entry.exists = $exists
    $reportedFiles += $entry

    $isDeleted = $false
    foreach ($statusCode in $entry.statusCodes) {
        if ($statusCode -match "D") {
            $isDeleted = $true
            break
        }
    }

    if (-not $exists -and -not $isDeleted) {
        $invalidReferences += $entry.path
    }
}

if ($invalidReferences.Count -gt 0) {
    [Console]::Error.WriteLine("Invalid file references found (path does not exist and file is not marked deleted):")
    foreach ($path in $invalidReferences) {
        [Console]::Error.WriteLine(" - $path")
    }
    exit 1
}

$report = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    repositoryRoot = $repoRoot
    referenceFormat = "path-only (no line references)"
    counts = [ordered]@{
        statusShort = $statusLines.Count
        trackedDiff = $trackedDiffFiles.Count
        untracked = $untrackedFiles.Count
        reportedFiles = $reportedFiles.Count
    }
    commands = [ordered]@{
        statusShort = $statusLines
        diffNameOnly = $trackedDiffFiles
        untracked = $untrackedFiles
    }
    reportedFiles = $reportedFiles
}

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputFullPath = if ([System.IO.Path]::IsPathRooted($OutputPath)) {
        $OutputPath
    } else {
        Join-Path $repoRoot $OutputPath
    }
    $outputDir = Split-Path -Parent $outputFullPath
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    }
    ($report | ConvertTo-Json -Depth 8) | Set-Content -Path $outputFullPath -Encoding UTF8
    Write-Host "Changed files report saved to $outputFullPath"
}

$report | ConvertTo-Json -Depth 8
