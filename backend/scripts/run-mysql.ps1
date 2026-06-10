# ParkVision - start the backend against local MySQL.
# Usage:  powershell -ExecutionPolicy Bypass -File "backend\scripts\run-mysql.ps1"
# Override the DB password with env var PARKVISION_DB_PASSWORD if you changed it.

$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:PARKVISION_DB_USER = 'parkvision'
if (-not $env:PARKVISION_DB_PASSWORD) { $env:PARKVISION_DB_PASSWORD = 'ParkVision@2026' }

$backendDir = Split-Path -Parent $PSScriptRoot
Set-Location $backendDir
Write-Host "Starting ParkVision backend on MySQL (profile=mysql, user=parkvision)..."
& .\mvnw.cmd -DskipTests spring-boot:run
