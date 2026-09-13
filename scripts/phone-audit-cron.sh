#!/bin/bash
# ============================================================================
# phone-audit-cron.sh — Weekly duplicate-phone audit with alerting.
#
# Runs diag-phone-audit.js, parses its verdict, and alerts if ANY of:
#   • two rows share phone_number (impossible: UNIQUE index — would mean
#     the schema changed)
#   • two accounts share a core number in different formats (07…/+250…)
#     — i.e. the duplicate-account problem has REAPPEARED
#   • one stored string maps to multiple accounts (shared variant)
#
# Output:
#   • always appends one summary line to logs/phone-audit.log
#   • on failure, appends the full audit to the log AND writes every line
#     to stderr (so cron mail / systemd mail / a log shipper picks it up)
#
# Install (as fidele on the VPS):
#   crontab -e
#   0 4 * * 1  /home/project3/scripts/phone-audit-cron.sh >> /home/project3/logs/phone-audit-cron.log 2>&1
#
# Test manually:
#   bash scripts/phone-audit-cron.sh
# ============================================================================
set -uo pipefail
cd "$(dirname "$0")/.."

mkdir -p logs
LOG="logs/phone-audit.log"
STAMP="$(date '+%Y-%m-%d %H:%M:%S %Z')"
AUDIT="$(node diag-phone-audit.js 2>&1)"
STATUS=$?

# The audit prints ✅ lines for each clean check and ❌ sections when dirty.
# Treat ANY ❌ line or a non-zero exit as an alert condition.
if [ $STATUS -ne 0 ] || printf '%s\n' "$AUDIT" | grep -q '❌'; then
  {
    echo "[$STAMP] ALERT: duplicate-phone problem detected (exit=$STATUS)"
    printf '%s\n' "$AUDIT" | sed 's/^/    /'
  } >> "$LOG"
  {
    echo "[phone-audit] ALERT at $STAMP — duplicate-phone problem on $(hostname)"
    echo "[phone-audit] Run: bash scripts/dedupe-duplicates.sh --report"
    printf '%s\n' "$AUDIT"
  } >&2
  exit 1
fi

USERS="$(printf '%s\n' "$AUDIT" | grep -oE 'Total users: [0-9]+' | grep -oE '[0-9]+')"
echo "[$STAMP] OK: no duplicate phones, no variant collisions (users=$USERS)" >> "$LOG"
echo "[phone-audit] OK at $STAMP (users=$USERS)"
exit 0
