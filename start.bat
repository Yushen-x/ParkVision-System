@echo off
setlocal
cd /d "%~dp0"

echo Starting ParkVision...
echo.

where mvn >nul 2>nul
if errorlevel 1 (
  echo [WARN] Maven was not found. Backend will be skipped.
  echo [WARN] Frontend will use mock fallback data.
) else (
  start "ParkVision Backend" cmd /k ""%~dp0scripts\run-backend.bat""
)

start "ParkVision Frontend" cmd /k ""%~dp0scripts\run-frontend-preview.bat""

timeout /t 3 >nul
start "" "http://localhost:5173"

echo.
echo Frontend window opened. If the browser shows nothing, wait a few seconds and refresh:
echo http://localhost:5173
echo.
endlocal
