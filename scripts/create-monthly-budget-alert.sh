#!/usr/bin/env bash
set -euo pipefail

MONTHLY_LIMIT_USD="${MONTHLY_LIMIT_USD:-20}"
ALERT_EMAIL="${1:?'usage: ALERT_EMAIL or first arg is required, e.g. ./scripts/create-monthly-budget-alert.sh you@example.com'}"
BUDGET_NAME="${BUDGET_NAME:-chaekdojang-monthly-cost}"

if ! command -v aws >/dev/null 2>&1; then
  echo "FAIL budget-alert: aws cli is required" >&2
  exit 1
fi

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

cat > "$tmp_dir/budget.json" <<JSON
{
  "BudgetName": "$BUDGET_NAME",
  "BudgetLimit": {
    "Amount": "$MONTHLY_LIMIT_USD",
    "Unit": "USD"
  },
  "TimeUnit": "MONTHLY",
  "BudgetType": "COST",
  "CostTypes": {
    "IncludeTax": true,
    "IncludeSubscription": true,
    "UseBlended": false
  }
}
JSON

cat > "$tmp_dir/notifications.json" <<JSON
[
  {
    "Notification": {
      "NotificationType": "ACTUAL",
      "ComparisonOperator": "GREATER_THAN",
      "Threshold": 80,
      "ThresholdType": "PERCENTAGE"
    },
    "Subscribers": [
      {
        "SubscriptionType": "EMAIL",
        "Address": "$ALERT_EMAIL"
      }
    ]
  },
  {
    "Notification": {
      "NotificationType": "FORECASTED",
      "ComparisonOperator": "GREATER_THAN",
      "Threshold": 100,
      "ThresholdType": "PERCENTAGE"
    },
    "Subscribers": [
      {
        "SubscriptionType": "EMAIL",
        "Address": "$ALERT_EMAIL"
      }
    ]
  }
]
JSON

aws budgets create-budget \
  --account-id "$ACCOUNT_ID" \
  --budget file://"$tmp_dir/budget.json" \
  --notifications-with-subscribers file://"$tmp_dir/notifications.json"

echo "OK budget-alert: $BUDGET_NAME ${MONTHLY_LIMIT_USD} USD, email=$ALERT_EMAIL"
echo "Confirm the AWS Budgets email subscription from your inbox."
