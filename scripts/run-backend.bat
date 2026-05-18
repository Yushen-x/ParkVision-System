@echo off
setlocal
cd /d "%~dp0..\backend"

echo ParkVision Backend
echo Project: %cd%
echo.

if exist "mvnw.cmd" (
  call mvnw.cmd spring-boot:run
) else (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo Maven Wrapper and global Maven were not found. Backend cannot start in this window.
    echo The frontend can still run with fallback data.
    pause
    exit /b 1
  )

  mvn spring-boot:run
)
pause
