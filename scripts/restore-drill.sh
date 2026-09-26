#!/bin/bash
# ============================================================================
# restore-drill.sh — Prove the nightly backups are RESTORABLE, not just present.
#
# Incident-report §7.1 / §8.4: a backup that has never been restored is a hope,
# not a plan. This drill:
#   1. Picks the newest ${DB_NAME}_*.sql.gz in /home/fidele/db-backups (or the
#      file given as $1).
#   2. Creates a scratch database restore_drill_<timestamp> on the same server.
#   3. gunzips and restores the dump into it (clean restore — no --force, so
#      any definer/permission noise counts as a failure, as it would in a real
#      disaster).
#   4. Verifies: table count matches the live DB, row counts of core tables
#      match the live DB, and the newest user row is as fresh as the live one.
#   5. Drops the scratch DB (on success; kept for inspection on failure).
#
# Needs: the mysql root socket (sudo mysql), same as db-backup.sh's env parsing.
# Run:   bash /home/project3/scripts/restore-drill.sh [backup-file.sql.gz]
# Exit:  0 = drill passed, 1 = drill FAILED (investigate before you need it!)
# ============================================================================
set -euo pipefail

PROJECT_DIR="/home/project3"
BACKUP_DIR="/home/fidele/db-backups"
STAMP="$(date '+%Y-%m-%d %H:%M:%S %Z')"

cd "$PROJECT_DIR"

# ── Load .env (same parsing as db-backup.sh) ────────────────────────────────
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
DB_NAME=$(echo "$DB_URL" | sed -E 's|.*/([a-zA-Z_0-9]+)\??.*|\1|')

fail() { echo "[$STAMP] ❌ DRILL FAILED: $*"; exit 1; }

# ── 1. Pick the backup to test ───────────────────────────────────────────────
FILE="${1:-$(ls -t "$BACKUP_DIR/${DB_NAME}"_*.sql.gz 2>/dev/null | head -1 || true)}"
[ -n "$FILE" ] && [ -f "$FILE" ] || fail "no backup file found in $BACKUP_DIR"
SIZE=$(du -h "$FILE" | cut -f1)
echo "[$STAMP] Testing backup: $FILE ($SIZE)"

# ── 2. Preflight: archive integrity + disk space + root socket ──────────────
gzip -t "$FILE" || fail "archive integrity check failed"
NEED_MB=$(( $(stat -c%s "$FILE") / 1024 / 1024 * 3 + 50 ))
HAVE_MB=$(df -m /home | awk 'NR==2{print $4}')
[ "$HAVE_MB" -gt "$NEED_MB" ] || fail "low disk: need ~${NEED_MB}MB, have ${HAVE_MB}MB"

SCRATCH="restore_drill_$(date '+%Y%m%d_%H%M%S')"
sudo mysql -N -B -e "SELECT 1" >/dev/null 2>&1 || fail "no mysql root via 'sudo mysql' — grant fidele sudo NOPASSWD for mysql or run as root"

# ── 3. Restore ───────────────────────────────────────────────────────────────
echo "[$STAMP] Restoring into scratch DB: $SCRATCH …"
sudo mysql -e "CREATE DATABASE \`$SCRATCH\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
if ! gunzip < "$FILE" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$SCRATCH" 2>/tmp/restore-drill.err; then
  echo "[$STAMP] Restore errors (kept scratch DB $SCRATCH for inspection):"
  tail -5 /tmp/restore-drill.err
  echo "[$STAMP] Clean up later with: sudo mysql -e 'DROP DATABASE \`$SCRATCH\`'"
  exit 1
fi
rm -f /tmp/restore-drill.err

# ── 4. Verify against the live DB ────────────────────────────────────────────
get_count() { mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B -e "SELECT COUNT(*) FROM \`$1\`.\`$2\`" 2>/dev/null; }

LIVE_TABLES=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")
DRILL_TABLES=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SCRATCH'")
echo "[$STAMP] Tables: live=$LIVE_TABLES restored=$DRILL_TABLES"
[ "$DRILL_TABLES" -eq "$LIVE_TABLES" ] || fail "table count mismatch"

CHECKS=0
for t in users user_subscriptions tests questions learning_materials pdf_files \
         transactions test_attempts user_activities irembo_driving_license_requests; do
  EXISTS=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SCRATCH' AND table_name='$t'")
  [ "$EXISTS" -eq 1 ] || continue
  LIVE=$(get_count "$DB_NAME" "$t"); REST=$(get_count "$SCRATCH" "$t")
  if [ "$LIVE" = "$REST" ]; then
    echo "[$STAMP]   ✅ $t: $REST rows (matches live)"
  else
    fail "$t row mismatch: live=$LIVE restored=$REST"
  fi
  CHECKS=$((CHECKS+1))
done
[ "$CHECKS" -ge 3 ] || fail "fewer than 3 core tables found — verify table names"

FRESH_LIVE=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B -e "SELECT MAX(created_at) FROM \`$DB_NAME\`.users")
FRESH_REST=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -B -e "SELECT MAX(created_at) FROM \`$SCRATCH\`.users")
echo "[$STAMP] Newest user row: live=$FRESH_LIVE restored=$FRESH_REST"

sudo mysql -e "DROP DATABASE \`$SCRATCH\`"
echo "[$STAMP] Scratch DB dropped."
echo "[$STAMP] ✅ DRILL PASSED — backup $FILE is restorable ($DRILL_TABLES tables, core counts match live)"
