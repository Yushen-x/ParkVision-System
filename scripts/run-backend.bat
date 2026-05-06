@echo off
setlocal
cd /d "%~dp0..\backend"

echo ParkVision Backend
echo Project: %cd%
echo.

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Backend cannot start in this window.
  echo The frontend can still run with mock fallback data.
  pause
  exit /b 1
)

mvn spring-boot:run
pause
