# ParkVision - MySQL root password reset + app database bootstrap
# Run this as Administrator (it stops/starts the MySQL service).
# It will:
#   1) reset root password to ParkVision@2026
#   2) create database 'parkvision' (utf8mb4)
#   3) create app account 'parkvision' / ParkVision@2026 and grant privileges

$ErrorActionPreference = 'Stop'

$mysqlBin    = 'D:\mysql-8.0.41-winx64\bin'
$serviceName = 'MySQL'
$newPassword = 'ParkVision@2026'
$initFile    = Join-Path $env:TEMP 'pv-mysql-reset.sql'

$sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$newPassword';
CREATE DATABASE IF NOT EXISTS parkvision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'parkvision'@'localhost' IDENTIFIED BY '$newPassword';
GRANT ALL PRIVILEGES ON parkvision.* TO 'parkvision'@'localhost';
FLUSH PRIVILEGES;
"@
Set-Content -Path $initFile -Value $sql -Encoding ASCII

Write-Host "[1/5] Stopping MySQL service..."
Stop-Service -Name $serviceName -Force
Start-Sleep -Seconds 2

Write-Host "[2/5] Starting temporary mysqld with --init-file..."
$proc = Start-Process -FilePath (Join-Path $mysqlBin 'mysqld.exe') -ArgumentList "--init-file=`"$initFile`"" -PassThru
Start-Sleep -Seconds 12

Write-Host "[3/5] Shutting down temporary mysqld..."
try {
    & (Join-Path $mysqlBin 'mysqladmin.exe') -u root "-p$newPassword" shutdown
} catch {
    Write-Host "  mysqladmin shutdown failed, will kill the process instead."
}
Start-Sleep -Seconds 3
if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }

Write-Host "[4/5] Restarting MySQL service..."
Start-Service -Name $serviceName
Start-Sleep -Seconds 3

Write-Host "[5/5] Verifying app account connection..."
& (Join-Path $mysqlBin 'mysql.exe') -u parkvision "-p$newPassword" -e "SELECT CURRENT_USER();" parkvision

Remove-Item $initFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "DONE. root and parkvision passwords are both: $newPassword"
Write-Host "Database 'parkvision' is ready. Go back to the chat and tell me it is done."
