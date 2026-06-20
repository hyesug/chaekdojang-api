#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.production}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/db_backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
BACKUP_S3_PREFIX="${BACKUP_S3_PREFIX:-db-backups}"

if [ ! -f "$ENV_FILE" ]; then
  echo "FAIL db-backup: env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

if [ -z "${SPRING_DATASOURCE_URL:-}" ] || [ -z "${SPRING_DATASOURCE_USERNAME:-}" ] || [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
  echo "FAIL db-backup: datasource variables are missing" >&2
  exit 1
fi

eval "$(
python3 - "$SPRING_DATASOURCE_URL" <<'PY'
import shlex
import sys
from urllib.parse import urlparse

jdbc_url = sys.argv[1]
if not jdbc_url.startswith("jdbc:postgresql://"):
    raise SystemExit("unsupported datasource url")

url = "postgresql://" + jdbc_url.removeprefix("jdbc:postgresql://")
parsed = urlparse(url)

host = parsed.hostname or ""
port = parsed.port or 5432
database = parsed.path.lstrip("/")

print(f"PGHOST={shlex.quote(host)}")
print(f"PGPORT={shlex.quote(str(port))}")
print(f"PGDATABASE={shlex.quote(database)}")
PY
)"

timestamp="$(date -u +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
backup_file="$BACKUP_DIR/chaekdojang_${timestamp}.dump"

PGPASSWORD="$SPRING_DATASOURCE_PASSWORD" pg_dump \
  --format=custom \
  --no-owner \
  --no-acl \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$SPRING_DATASOURCE_USERNAME" \
  --dbname="$PGDATABASE" \
  --file="$backup_file"

chmod 600 "$backup_file"
find "$BACKUP_DIR" -name 'chaekdojang_*.dump' -type f -mtime +"$RETENTION_DAYS" -delete

if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
  if ! command -v aws >/dev/null 2>&1; then
    echo "FAIL db-backup: aws cli is required when BACKUP_S3_BUCKET is set" >&2
    exit 1
  fi

  s3_uri="s3://${BACKUP_S3_BUCKET%/}/${BACKUP_S3_PREFIX%/}/$(basename "$backup_file")"
  aws s3 cp "$backup_file" "$s3_uri" --only-show-errors
  echo "OK db-backup-s3: $s3_uri"
fi

echo "OK db-backup: $backup_file"
