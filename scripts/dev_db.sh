#!/usr/bin/env bash
set -euo pipefail

# Simple helper to start Postgres (with pgvector extension) via docker compose
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

cd "$ROOT_DIR"

echo "Starting Postgres containers..."
docker compose up -d postgres postgres_vector

echo "Waiting for health..."
for i in {1..30}; do
  if docker inspect -f '{{.State.Health.Status}}' irlquest-postgres 2>/dev/null | grep -q healthy && \
     docker inspect -f '{{.State.Health.Status}}' irlquest-postgres-vector 2>/dev/null | grep -q healthy; then
    echo "Databases are healthy."
    break
  fi
  sleep 2
  echo -n "."
done

echo "DATABASE_URLs:"
echo "  Normal:  postgresql://postgres:password@localhost:5432/irl_quest"
echo "  Vector:  postgresql://postgres:password@localhost:5433/irl_quest_vec"
