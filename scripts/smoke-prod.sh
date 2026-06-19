#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-https://api.chaekdojang.com}"
WEB_BASE="${WEB_BASE:-https://www.chaekdojang.com}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

check_json_status() {
  local name="$1"
  local url="$2"
  local file="$tmp_dir/$name.json"
  local code

  code="$(curl -sS -o "$file" -w "%{http_code}" "$url")"
  if [ "$code" != "200" ]; then
    echo "FAIL $name: expected 200, got $code"
    cat "$file" || true
    exit 1
  fi
}

check_json_status "api-health" "$API_BASE/actuator/health"
python3 - "$tmp_dir/api-health.json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as file:
    body = json.load(file)

if body.get("status") != "UP":
    print("FAIL api-health: status is not UP", file=sys.stderr)
    sys.exit(1)
PY

check_json_status "web-reviews" "$WEB_BASE/api/reviews?page=0&size=5&sort=recent"
python3 - "$tmp_dir/web-reviews.json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as file:
    body = json.load(file)

content = body.get("data", {}).get("content")
if not isinstance(content, list) or len(content) < 1:
    print("FAIL web-reviews: expected at least one review", file=sys.stderr)
    sys.exit(1)

first_id = content[0].get("id") if isinstance(content[0], dict) else None
print(f"OK web-reviews: {len(content)} reviews, first id {first_id}")
PY

home_code="$(curl -sS -o "$tmp_dir/home.html" -w "%{http_code}" "$WEB_BASE")"
if [ "$home_code" != "200" ]; then
  echo "FAIL web-home: expected 200, got $home_code"
  exit 1
fi

echo "OK api-health"
echo "OK web-home"
