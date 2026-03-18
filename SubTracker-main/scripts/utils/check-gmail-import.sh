#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

health_url="${BASE_URL}/v3/api-docs"
callback_url="${BASE_URL}/api/v1/integrations/GMAIL/oauth/callback?error=access_denied"

echo "[1/2] Checking backend health at ${health_url}"
curl -fsS "${health_url}" >/dev/null

echo "[2/2] Checking Gmail mailbox flow flag via callback endpoint"
headers_file="$(mktemp)"
body_file="$(mktemp)"
trap 'rm -f "$headers_file" "$body_file"' EXIT

status="$(curl -sS -D "${headers_file}" -o "${body_file}" -w "%{http_code}" "${callback_url}")"

if grep -qi "mailbox import flow is disabled by feature flag" "${body_file}"; then
  echo "FAIL: Gmail mailbox flow is disabled by feature flag."
  sed -n '1,40p' "${body_file}"
  exit 1
fi

case "${status}" in
  302|303|307|308)
    location_header="$(grep -i '^location:' "${headers_file}" | tail -n1 | tr -d '\r' | sed -E 's/^[Ll]ocation:[[:space:]]*//')"
    echo "OK: Gmail mailbox flow flag is enabled (HTTP ${status})."
    if [[ -n "${location_header}" ]]; then
      echo "Redirect target: ${location_header}"
    fi
    ;;
  *)
    echo "FAIL: Unexpected callback response HTTP ${status}."
    sed -n '1,40p' "${body_file}"
    exit 1
    ;;
esac
