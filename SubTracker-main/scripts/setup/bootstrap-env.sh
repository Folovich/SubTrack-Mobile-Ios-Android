#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

copy_if_missing() {
  local source_file="$1"
  local target_file="$2"
  if [[ -f "$target_file" ]]; then
    echo "[ok] ${target_file#"$REPO_ROOT"/} already exists"
    return
  fi
  cp "$source_file" "$target_file"
  echo "[created] ${target_file#"$REPO_ROOT"/} from ${source_file#"$REPO_ROOT"/}"
}

upsert_key() {
  local env_file="$1"
  local key="$2"
  local value="$3"
  if grep -q "^${key}=" "$env_file"; then
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "$env_file"
    rm -f "${env_file}.bak"
  else
    printf "%s=%s\n" "$key" "$value" >> "$env_file"
  fi
}

read_env_value() {
  local key="$1"
  local env_file
  for env_file in "$REPO_ROOT/apps/backend/.env" "$REPO_ROOT/.env"; do
    [[ -f "$env_file" ]] || continue
    local line
    line="$(grep -E "^${key}=" "$env_file" | tail -n1 || true)"
    if [[ -n "$line" ]]; then
      echo "${line#*=}"
      return
    fi
  done
  echo ""
}

print_secret_state() {
  local key="$1"
  local value
  value="$(read_env_value "$key")"
  if [[ -n "${value// }" ]]; then
    echo "${key}=SET"
  else
    echo "${key}=EMPTY"
  fi
}

copy_if_missing "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
copy_if_missing "$REPO_ROOT/apps/backend/.env.example" "$REPO_ROOT/apps/backend/.env"
copy_if_missing "$REPO_ROOT/apps/web/.env.example" "$REPO_ROOT/apps/web/.env"
copy_if_missing "$REPO_ROOT/apps/mobile/.env.example" "$REPO_ROOT/apps/mobile/.env"

for env_file in "$REPO_ROOT/.env" "$REPO_ROOT/apps/backend/.env"; do
  upsert_key "$env_file" "IMPORT_PROVIDERS_GMAIL_ENABLED" "true"
  upsert_key "$env_file" "IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED" "true"
  upsert_key "$env_file" "IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED" "false"
done

echo "----- Gmail mailbox defaults -----"
grep -nE "^(IMPORT_PROVIDERS_GMAIL_ENABLED|IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED|IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED)=" "$REPO_ROOT/.env" "$REPO_ROOT/apps/backend/.env"
echo "----- Gmail OAuth required secrets -----"
print_secret_state "GMAIL_CLIENT_ID"
print_secret_state "GMAIL_CLIENT_SECRET"
echo "If OAuth secrets are EMPTY, set them in apps/backend/.env (or root .env) before real Gmail sync."
