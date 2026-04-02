#!/usr/bin/env sh
echo "Stopping SubTrack stack..."
docker compose -f infra/docker-compose.yml down
