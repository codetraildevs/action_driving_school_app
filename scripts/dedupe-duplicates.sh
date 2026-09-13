#!/bin/bash
# ============================================================================
# dedupe-duplicates.sh — Backup, preview, confirm, then merge the duplicate
# 07…/+250… account pairs in production.
#
# Usage (on the VPS, from /home/project3):
#   bash sql/dedupe-duplicates.sh           # interactive: backup → preview → confirm → merge → verify
#   bash sql/dedupe-duplicates.sh --report  # read-only preview, no changes
#
# Reads DB credentials from .env (DATABASE_URL), same as the app.
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

# ── Load .env ────────────────────────────────────────────────────────────────
for line in $(cat .env); do
  key="${line%%=*}"
  val="${line#*=}"
  [[ "$key" =~ ^[A-Z_]+$ ]] || continue
  val="${val%\"}"; val="${val#\"}"; val="${val%\'}"; val="${val#\'}"
  export "$key=$val"
done

DB_URL="${DATABASE_URL:?DATABASE_URL missing in .env}"
DB_HOST=$(echo "$DB_URL" | sed -E 's|.*@([^:/]+).*|\1|')
DB_PORT=$(echo "$DB_URL" | sed -nE 's|.*:([0-9]+)@.*|\1|p'); DB_PORT=${DB_PORT:-3306}
DB_USER=$(echo "$DB_URL" | sed -E 's|.*://([^:@]+):.*|\1|')
DB_PASS=$(echo "$DB_URL" | sed -E 's|.*://[^:@]+:([^@]+)@.*|\1|')
DB_NAME=$(echo "$DB_URL" | sed -E 's|.*/([a-z_0-9]+)\??.*|\1|')

MYSQL="mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME"

# URL-decode the password if needed (%40 → @)
DB_PASS_DECODED=$(printf '%b' "${DB_PASS//%/\\x}")

echo "═══════════════════════════════════════════════════════"
echo "  Duplicate account dedupe — $(date)"
echo "  DB: $DB_NAME @ $DB_HOST:$DB_PORT (user: $DB_USER)"
echo "══════════════════════════════════════════ --"

# ── 1. Preview (always) ─────────────────────────────────────────────────────
REPORT_SQL="
SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core,
       GROUP_CONCAT(CONCAT(id, ':', phone_number, CASE WHEN is_active=0 THEN '(inactive)' ELSE '' END) SEPARATOR ' | ') AS accounts
FROM users
GROUP BY core HAVING COUNT(*) > 1
ORDER BY core;"

echo ""
echo "[1/4] Duplicate pairs preview (read-only):"
$MYSQL -e "$REPORT_SQL"

if [[ "${1:-}" == "--report" ]]; then
  echo ""
  echo "Report-only mode — no changes made."
  exit 0
fi

# ── 2. Backup ────────────────────────────────────────────────────────────────
echo ""
echo "[2/4] Backing up users + child tables…"
STAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="/root/dedupe_backup_${STAMP}.sql"
[[ -w /root ]] || BACKUP_FILE="./dedupe_backup_${STAMP}.sql"
mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --single-transaction --no-tablespaces \
  "$DB_NAME" users user_subscriptions user_test_access user_timezones reading_sessions \
  > "$BACKUP_FILE" 2>/dev/null
echo "      Backup: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# ── 3. Confirm ───────────────────────── carefully
echo ""
echo "[3/4] This will MERGE duplicate accounts (lower id survives, higher id is"
echo "      archived into users_dedupe_archive and deleted)."
read -r -p "Type MERGE to continue, anything else to abort: " ANSWER
if [[ "$ANSWER" != "MERGE" ]]; then
  echo "Aborted — nothing changed."
  exit 1
fi

# ── 4. Run the merge ─────────────────────────────────────────────────────────
echo ""
echo "[4/4] Running merge…"
$MYSQL < sql/dedupe-accounts.sql

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Done. Verify:"
echo "    node diag-phone-audit.js          # must show no variant collisions"
echo "    $MYSQL -e 'SELECT * FROM dedupe_log ORDER BY id'"
echo "═══════════════════════════════════════════════════════"
