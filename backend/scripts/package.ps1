# ParkVision - build a deployable package (backend jar + frontend dist).
# Usage:  powershell -ExecutionPolicy Bypass -File "backend\scripts\package.ps1"
#
# Output:
#   backend\target\parkvision-backend-*.jar   (runnable Spring Boot jar)
#   frontend\dist\                             (static frontend, serve via any web server)

$ErrorActionPreference = 'Stop'
$backendDir = Split-Path -Parent $PSScriptRoot
$rootDir = Split-Path -Parent $backendDir
$frontendDir = Join-Path $rootDir 'frontend'

Write-Host '== [1/2] Building backend (tests included) ==' -ForegroundColor Cyan
Set-Location $backendDir
& .\mvnw.cmd clean package
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }

Write-Host '== [2/2] Building frontend ==' -ForegroundColor Cyan
Set-Location $frontendDir
if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Write-Host 'Installing frontend dependencies...'
    & npm install
    if ($LASTEXITCODE -ne 0) { throw 'npm install failed.' }
}
& npm run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }

$jar = Get-ChildItem (Join-Path $backendDir 'target') -Filter 'parkvision-backend-*.jar' |
    Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*plain*' } |
    Select-Object -First 1

Write-Host ''
Write-Host 'Package complete.' -ForegroundColor Green
Write-Host ("  Backend jar : {0}" -f $jar.FullName)
Write-Host ("  Frontend    : {0}" -f (Join-Path $frontendDir 'dist'))
Write-Host ''
Write-Host 'Run the backend with:  powershell -ExecutionPolicy Bypass -File "backend\scripts\start-prod.ps1"'
