#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "${SCRIPT_DIR}/../.." && pwd)"
bash "${SCRIPT_DIR}/../setup/bootstrap-env.sh"

echo "Starting SubTrack stack..."
docker compose -f "${REPO_ROOT}/infra/docker-compose.yml" up -d
