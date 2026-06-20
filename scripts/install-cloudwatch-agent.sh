#!/usr/bin/env bash
set -euo pipefail

CONFIG_SOURCE="${CONFIG_SOURCE:-ops/cloudwatch-agent-config.json}"
CONFIG_TARGET="/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json"

if ! command -v amazon-cloudwatch-agent-ctl >/dev/null 2>&1; then
  tmp_deb="$(mktemp)"
  trap 'rm -f "$tmp_deb"' EXIT
  curl -fsSL \
    https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb \
    -o "$tmp_deb"
  sudo dpkg -i "$tmp_deb"
fi

if [ ! -f "$CONFIG_SOURCE" ]; then
  echo "FAIL cloudwatch-agent: config not found: $CONFIG_SOURCE" >&2
  exit 1
fi

sudo mkdir -p "$(dirname "$CONFIG_TARGET")"
sudo cp "$CONFIG_SOURCE" "$CONFIG_TARGET"

sudo amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c "file:$CONFIG_TARGET" \
  -s

sudo systemctl status amazon-cloudwatch-agent --no-pager
