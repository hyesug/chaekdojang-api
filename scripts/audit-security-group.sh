#!/usr/bin/env bash
set -euo pipefail

SG_ID="${1:?'usage: ./scripts/audit-security-group.sh sg-xxxxxxxx'}"

if ! command -v aws >/dev/null 2>&1; then
  echo "FAIL security-group-audit: aws cli is required" >&2
  exit 1
fi

echo "== Security group inbound rules: $SG_ID =="
aws ec2 describe-security-groups \
  --group-ids "$SG_ID" \
  --query 'SecurityGroups[0].IpPermissions[*].{
    Protocol:IpProtocol,
    From:FromPort,
    To:ToPort,
    IPv4:IpRanges[*].CidrIp,
    IPv6:Ipv6Ranges[*].CidrIpv6
  }' \
  --output table

echo
echo "== Public exposure check =="
aws ec2 describe-security-groups \
  --group-ids "$SG_ID" \
  --query 'SecurityGroups[0].IpPermissions[?contains(IpRanges[].CidrIp, `0.0.0.0/0`) || contains(Ipv6Ranges[].CidrIpv6, `::/0`)].{
    Protocol:IpProtocol,
    From:FromPort,
    To:ToPort,
    IPv4:IpRanges[*].CidrIp,
    IPv6:Ipv6Ranges[*].CidrIpv6
  }' \
  --output table

echo
echo "Expected public inbound rules:"
echo "- 80/tcp from 0.0.0.0/0 and ::/0"
echo "- 443/tcp from 0.0.0.0/0 and ::/0"
echo
echo "Review immediately if these are public:"
echo "- 22/tcp SSH: should be your current IP only"
echo "- 5432/tcp PostgreSQL: should not be public"
echo "- 6379/tcp Redis: should not be public"
echo "- 8080/tcp API: should not be public; compose binds it to 127.0.0.1"
