#!/usr/bin/env bash
# ParkVision one-click startup for macOS / Linux.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
HYPERLPR_DIR="$PROJECT_ROOT/tools/hyperlpr"
LOGS_DIR="$PROJECT_ROOT/logs"
PID_FILE="$LOGS_DIR/parkvision.pids"

FRONTEND_PORT="${FRONTEND_PORT:-5173}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
VISION_PORT="${VISION_PORT:-8715}"
SKIP_BACKEND="${SKIP_BACKEND:-0}"
SKIP_VISION="${SKIP_VISION:-0}"
NO_INSTALL="${NO_INSTALL:-0}"
DETACHED="${DETACHED:-1}"
OPEN_BROWSER="${OPEN_BROWSER:-1}"

mkdir -p "$LOGS_DIR"
: > "$PID_FILE"

step() { printf '\n==> %s\n' "$1"; }
warn() { printf 'WARN: %s\n' "$1" >&2; }

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: $1 is required but not found." >&2
    exit 1
  fi
}

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

save_pid() {
  printf '%s=%s\n' "$1" "$2" >> "$PID_FILE"
}

wait_for_http() {
  local url="$1"
  local label="$2"
  local attempts="${3:-60}"
  local i=1
  while (( i <= attempts )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    (( i++ )) || true
  done
  warn "$label did not become ready in time: $url"
  return 1
}

pick_frontend_port() {
  local port="$FRONTEND_PORT"
  while port_in_use "$port"; do
    warn "Port $port is already in use."
    port=$((port + 1))
  done
  FRONTEND_PORT="$port"
}

start_backend() {
  if [[ "$SKIP_BACKEND" == "1" ]]; then
    warn "Backend startup skipped (SKIP_BACKEND=1)."
    return 0
  fi

  if port_in_use "$BACKEND_PORT"; then
    warn "Backend port $BACKEND_PORT is already in use. Skip backend startup."
    return 0
  fi

  local maven_cmd=""
  if [[ -x "$BACKEND_DIR/mvnw" ]]; then
    maven_cmd="$BACKEND_DIR/mvnw"
  elif command -v mvn >/dev/null 2>&1; then
    maven_cmd="mvn"
  else
    warn "Neither Maven Wrapper nor global Maven was found. Backend will not start."
    warn "Frontend will use fallback data until the backend toolchain is available."
    return 0
  fi

  step "Starting Spring Boot backend (port $BACKEND_PORT)"
  (
    cd "$BACKEND_DIR"
    exec "$maven_cmd" spring-boot:run
  ) >>"$LOGS_DIR/backend.out.log" 2>>"$LOGS_DIR/backend.err.log" &
  save_pid backend "$!"
  wait_for_http "http://127.0.0.1:$BACKEND_PORT/api/auth/login" "Backend" 120 || true
}

start_vision() {
  if [[ "$SKIP_VISION" == "1" ]]; then
    return 0
  fi

  if ! command -v python3 >/dev/null 2>&1 && ! command -v python >/dev/null 2>&1; then
    warn "Python not found. License-plate OCR will fall back to the built-in engine."
    return 0
  fi

  if port_in_use "$VISION_PORT"; then
    warn "Vision port $VISION_PORT is already in use. Skip HyperLPR startup."
    return 0
  fi

  if [[ ! -f "$HYPERLPR_DIR/server.py" ]]; then
    warn "HyperLPR server.py not found. Skip vision service."
    return 0
  fi

  local python_cmd="python3"
  if ! command -v python3 >/dev/null 2>&1; then
    python_cmd="python"
  fi

  step "Starting HyperLPR vision service (port $VISION_PORT)"
  (
    cd "$HYPERLPR_DIR"
    exec "$python_cmd" server.py
  ) >>"$LOGS_DIR/hyperlpr.out.log" 2>>"$LOGS_DIR/hyperlpr.err.log" &
  save_pid hyperlpr "$!"
}

start_frontend() {
  require_cmd node
  require_cmd npm

  if [[ "$NO_INSTALL" != "1" && ! -d "$FRONTEND_DIR/node_modules" ]]; then
    step "Installing frontend dependencies"
    (cd "$FRONTEND_DIR" && npm install)
  fi

  pick_frontend_port

  if port_in_use "$FRONTEND_PORT"; then
    warn "Frontend port $FRONTEND_PORT is already in use. Skip frontend startup."
    return 0
  fi

  step "Starting Vue frontend (port $FRONTEND_PORT)"
  if [[ "$DETACHED" == "1" ]]; then
    (
      cd "$FRONTEND_DIR"
      exec npm run dev -- --port "$FRONTEND_PORT"
    ) >>"$LOGS_DIR/frontend.out.log" 2>>"$LOGS_DIR/frontend.err.log" &
    save_pid frontend "$!"
    wait_for_http "http://127.0.0.1:$FRONTEND_PORT/" "Frontend" 30 || true
  else
    printf '\nFrontend: http://localhost:%s\n' "$FRONTEND_PORT"
    printf 'Keep this terminal open while developing.\n\n'
    cd "$FRONTEND_DIR"
    exec npm run dev -- --port "$FRONTEND_PORT"
  fi
}

printf 'ParkVision startup\nProject: %s\n' "$PROJECT_ROOT"

start_vision
start_backend
start_frontend

printf '\nParkVision is running.\n'
printf 'Frontend: http://localhost:%s\n' "$FRONTEND_PORT"
printf 'Backend:  http://localhost:%s\n' "$BACKEND_PORT"
printf 'Logs:     %s\n' "$LOGS_DIR"
printf 'Stop:     ./stop.sh\n'

if [[ "$OPEN_BROWSER" == "1" && "$DETACHED" == "1" ]]; then
  if command -v open >/dev/null 2>&1; then
    open "http://localhost:$FRONTEND_PORT"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1 || true
  fi
fi
