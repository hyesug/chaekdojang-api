#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-.env.production}"

echo "== git =="
git log -1 --oneline || true
git status --short || true

echo
echo "== disk =="
df -h /
sudo docker system df || true

echo
echo "== containers =="
sudo docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
sudo docker inspect chaekdojang-app --format 'app image: {{.Config.Image}}' || true

echo
echo "== health =="
curl -sS http://localhost:8080/actuator/health || true
echo

echo
echo "== recent backups =="
ls -lh /home/ubuntu/db_backups 2>/dev/null | tail -n 10 || true

echo
echo "== deploy history =="
cat deploy_history/latest.env 2>/dev/null || true
cat deploy_history/latest_rollback.env 2>/dev/null || true

echo
echo "== recent app logs =="
sudo docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=80 app || true
