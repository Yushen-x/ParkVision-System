#!/usr/bin/env bash
# ParkVision one-click shutdown for macOS / Linux.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"
PID_FILE="$LOGS_DIR/parkvision.pids"

PORTS=(5173 5174 8080 8715)

warn() { printf 'WARN: %s\n' "$1" >&2; }

stop_pid() {
  local name="$1"
  local pid="$2"

  if [[ -z "$pid" ]]; then
    return 0
  fi

  if kill -0 "$pid" >/dev/null 2>&1; then
    printf 'Stopping %s (pid %s)\n' "$name" "$pid"
    kill "$pid" >/dev/null 2>&1 || true

    local i=0
    while kill -0 "$pid" >/dev/null 2>&1 && (( i < 10 )); do
      sleep 0.5
      (( i++ )) || true
    done

    if kill -0 "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  fi
}

stop_from_pid_file() {
  if [[ ! -f "$PID_FILE" ]]; then
    return 0
  fi

  while IFS='=' read -r name pid; do
    [[ -z "${name:-}" || -z "${pid:-}" ]] && continue
    stop_pid "$name" "$pid"
  done < "$PID_FILE"

  rm -f "$PID_FILE"
}

stop_by_ports() {
  local port pid
  for port in "${PORTS[@]}"; do
    local pids
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
    [[ -z "$pids" ]] && continue

    while read -r pid; do
      [[ -z "$pid" ]] && continue
      local cmd cwd
      cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
      cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -1 || true)"

      if [[ "$cwd" == "$PROJECT_ROOT"* ]] \
        || [[ "$cmd" == *"ParkVision-System"* ]] \
        || [[ "$cmd" == *"parkvision"* ]] \
        || [[ "$cmd" == *"spring-boot:run"* && "$cwd" == "$PROJECT_ROOT/backend"* ]] \
        || [[ "$cmd" == *"vite"* && "$cwd" == "$PROJECT_ROOT/frontend"* ]] \
        || [[ "$cmd" == *"server.py"* && "$cwd" == "$PROJECT_ROOT/tools/hyperlpr"* ]]; then
        stop_pid "port-$port" "$pid"
      else
        warn "Port $port is used by another project (pid $pid). Skipped."
      fi
    done <<< "$pids"
  done
}

printf 'Stopping ParkVision...\n'
stop_from_pid_file
stop_by_ports
printf 'Done.\n'
