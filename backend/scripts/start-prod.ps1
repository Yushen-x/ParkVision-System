# ParkVision - run the packaged backend jar against MySQL in production-ish mode.
# Usage:  powershell -ExecutionPolicy Bypass -File "backend\scripts\start-prod.ps1"
#
# Reads (with sensible local defaults) the following environment variables:
#   PARKVISION_DB_USER / PARKVISION_DB_PASSWORD   - MySQL application credentials
#   PARKVISION_DATASOURCE_URL                     - JDBC URL (optional override)
#   PARKVISION_JWT_SECRET                         - JWT signing key (auto-generated if unset)

$ErrorActionPreference = 'Stop'
$backendDir = Split-Path -Parent $PSScriptRoot
Set-Location $backendDir

$jar = Get-ChildItem (Join-Path $backendDir 'target') -Filter 'parkvision-backend-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*plain*' } |
    Select-Object -First 1
if (-not $jar) {
    throw 'No packaged jar found. Run package.ps1 first.'
}

$env:SPRING_PROFILES_ACTIVE = 'mysql'
if (-not $env:PARKVISION_DB_USER) { $env:PARKVISION_DB_USER = 'parkvision' }
if (-not $env:PARKVISION_DB_PASSWORD) { $env:PARKVISION_DB_PASSWORD = 'ParkVision@2026' }
if (-not $env:PARKVISION_JWT_SECRET) {
    # Generate a strong random secret for this run so the insecure default is never used.
    $bytes = New-Object 'System.Byte[]' 48
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $env:PARKVISION_JWT_SECRET = [Convert]::ToBase64String($bytes)
    Write-Host 'Generated a one-off JWT secret for this run (set PARKVISION_JWT_SECRET to persist sessions across restarts).'
}

Write-Host ("Starting {0} on MySQL ..." -f $jar.Name) -ForegroundColor Cyan
& java -jar $jar.FullName
