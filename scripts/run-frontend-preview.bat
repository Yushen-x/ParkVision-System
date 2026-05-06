@echo off
setlocal
cd /d "%~dp0.."

echo ParkVision Frontend Preview
echo Project: %cd%
echo.

if not exist "frontend\node_modules" (
  echo Installing frontend dependencies...
  cd /d "%~dp0..\frontend"
  call npm install
  if errorlevel 1 (
    echo npm install failed.
    pause
    exit /b 1
  )
  cd /d "%~dp0.."
)

if not exist "frontend\dist\index.html" (
  echo Building frontend...
  cd /d "%~dp0..\frontend"
  call npm run build
  if errorlevel 1 (
    echo npm run build failed.
    pause
    exit /b 1
  )
  cd /d "%~dp0.."
)

echo Starting static preview server...
echo URL: http://localhost:5173
echo Keep this window open.
echo.
node scripts\serve-frontend.mjs

pause
