#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ubuntu/chaekdojang-api}"
CRON_TIME="${CRON_TIME:-17 3 * * *}"
LOG_FILE="${LOG_FILE:-/home/ubuntu/db_backups/backup.log}"

mkdir -p "$(dirname "$LOG_FILE")"

job="$CRON_TIME cd $APP_DIR && ./scripts/backup-prod-db.sh >> $LOG_FILE 2>&1"

tmp="$(mktemp)"
crontab -l 2>/dev/null | grep -v 'backup-prod-db.sh' > "$tmp" || true
printf '%s\n' "$job" >> "$tmp"
crontab "$tmp"
rm -f "$tmp"

echo "OK db-backup-cron: $job"
