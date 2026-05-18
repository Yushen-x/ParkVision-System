# ParkVision-System

ParkVision-System is a smart parking demo project with a Vue 3 frontend and a Spring Boot backend. The repository is now wired so the frontend prefers live APIs first, while still keeping fallback data available when the backend is offline.

## Project layout

```text
ParkVision-System/
  frontend/     Vue 3 + Vite application
  backend/      Spring Boot backend with Maven Wrapper
  docs/         architecture notes and demo material
  deploy/       Docker Compose draft
  scripts/      local startup helpers
  start.bat     Windows one-click launcher
  start.ps1     PowerShell launcher
  stop.bat      Windows stop helper
  stop.ps1      PowerShell stop helper
```

## Quick start

PowerShell:

```powershell
.\start.ps1
```

Command Prompt:

```bat
start.bat
```

Default URLs:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
```

The backend no longer depends on a globally installed Maven. The repository includes `backend/mvnw.cmd`, and the startup scripts prefer it automatically.

## What is complete now

- Frontend pages for dashboard, AI vision, dispatch, admin console, owner portal, digital twin, system gateway, pricing, and indoor navigation
- Backend APIs for slots, raw orders, admin order rows, alerts, pricing rules, access list, system nodes, dispatch queue, AGVs, pre-dispatch, VIP retrieval, and vision inference
- Fallback repository data that keeps the demo usable even if the backend is not running
- Minimal backend tests plus a Maven Wrapper so the backend can be built on a clean machine

## Manual commands

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Backend:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```
