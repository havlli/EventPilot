#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
COMMAND_DIR="${ROOT_DIR}/src/main/resources/commands"
FAILURES=0

required_keys=(
  "DISCORD_BOT_TOKEN"
  "JWT_SECRET"
  "POSTGRES_DB"
  "POSTGRES_USER"
  "POSTGRES_PASSWORD"
)

optional_keys=(
  "DISCORD_SCHEDULER_INTERVAL_SECONDS"
  "DISCORD_REMINDER_LEAD_MINUTES"
)

pass() {
  printf '[ok] %s\n' "$1"
}

warn() {
  printf '[warn] %s\n' "$1"
}

fail() {
  printf '[fail] %s\n' "$1"
  FAILURES=$((FAILURES + 1))
}

env_line_for() {
  local key="$1"

  if [ ! -f "$ENV_FILE" ]; then
    return 1
  fi

  grep -E "^[[:space:]]*(export[[:space:]]+)?${key}[[:space:]]*=" "$ENV_FILE" | tail -n 1
}

has_non_empty_env_value() {
  local key="$1"
  local line

  line="$(env_line_for "$key" || true)"
  if [ -z "$line" ]; then
    return 1
  fi

  printf '%s' "$line" | grep -Eq '=[[:space:]]*[^[:space:]#]'
}

check_docker() {
  if command -v docker >/dev/null 2>&1; then
    pass "Docker CLI is installed"
  else
    fail "Docker CLI is not installed"
    return
  fi

  if docker info >/dev/null 2>&1; then
    pass "Docker daemon is reachable"
  else
    fail "Docker daemon is not reachable; start Docker Desktop"
  fi
}

check_env_file() {
  if [ -f "$ENV_FILE" ]; then
    pass ".env exists"
  else
    fail ".env is missing at ${ENV_FILE}"
  fi

  if git -C "$ROOT_DIR" check-ignore -q .env; then
    pass ".env is ignored by git"
  else
    warn ".env is not ignored by git"
  fi

  if git -C "$ROOT_DIR" ls-files --error-unmatch .env >/dev/null 2>&1; then
    fail ".env is tracked by git"
  else
    pass ".env is not tracked by git"
  fi
}

check_env_keys() {
  local key

  for key in "${required_keys[@]}"; do
    if has_non_empty_env_value "$key"; then
      pass "required key is set: ${key}"
    else
      fail "required key is missing or empty: ${key}"
    fi
  done

  for key in "${optional_keys[@]}"; do
    if has_non_empty_env_value "$key"; then
      pass "optional key is set: ${key}"
    else
      warn "optional fast-smoke key is not set: ${key}"
    fi
  done
}

check_command_resources() {
  local command_count
  local command_file

  if [ ! -d "$COMMAND_DIR" ]; then
    fail "command resource directory is missing: ${COMMAND_DIR}"
    return
  fi

  command_count=0
  for command_file in "$COMMAND_DIR"/*.json; do
    if [ -f "$command_file" ]; then
      command_count=$((command_count + 1))
    fi
  done

  if [ "$command_count" -ge 9 ]; then
    pass "slash command JSON resources found: ${command_count}"
  else
    fail "expected at least 9 slash command JSON resources, found: ${command_count}"
  fi
}

printf 'EventPilot live smoke preflight\n'
printf 'Checking local readiness without printing secret values.\n\n'

check_docker
check_env_file
check_env_keys
check_command_resources

printf '\n'
if [ "$FAILURES" -eq 0 ]; then
  pass "preflight passed; continue with docs/live-smoke-test.md"
else
  fail "preflight failed with ${FAILURES} blocking issue(s)"
  exit 1
fi
