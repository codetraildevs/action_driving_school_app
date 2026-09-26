#!/bin/bash
# ============================================================================
# restore-drill.sh — Prove the nightly backups are RESTORABLE and FRESH.
#
# Incident-report §7.1 / §8.4: a backup that has never been restored is a hope,
# not a plan. This drill:
#   1. Picks the newest ${DB_NAME}_*.sql.gz in /home/fidele/db-backups (or the
#      file given as $1).
#   2. Creates a scratch database restore_drill_<timestamp> on the same server.
#   3. gunzips and restores the dump into it AS ROOT (sudo mysql socket) — the
#      way a real disaster restore runs. (The app user's grants only cover the
#      live DB, so piping the dump in as the app user fails with ERROR 1044;
#      root needs no extra grants.)
#   4. Verifies with a model that works for backups of ANY age:
#        - restore completed cleanly (no --force: errors = failure)
#        - schema: restored table count vs live (fewer = migrations since
#          backup → warn; more = impossible → fail)
#        - core tables: present and non-empty; restored rows can never exceed
#          live rows (that would mean corruption)
#        - freshness: the newest users.created_at inside the restored data
#          must be ≤ live's, and the BACKUP FILE itself must be younger than
#          MAX_AGE_HOURS (default 48) — an old-but-restorable backup is still
#          an unacceptable recovery point, so staleness FAILS the drill
#   5. Drops the scratch DB (on success; kept for inspection on failure).
#
# Run:   bash /home/project3/scripts/restore-drill.sh [backup-file.sql.gz]
# Env:   MAX_AGE_HOURS=48 (fail if newest backup is older than this)
# Exit:  0 = drill passed, 1 = drill FAILED (investigate before you need it!)
# ============================================================================
set -euo pipefail

PROJECT_DIR="/home/project3"
BACKUP_DIR="/home/fidele/db-backups"
MAX_AGE_HOURS="${MAX_AGE_HOURS:-48}"
STAMP="$(date '+%Y-%m-%d %H:%M:%S %Z')"

cd "$PROJECT_DIR"

# ── Load .env (same parsing as db-backup.sh, to learn DB_NAME) ──────────────
for line in $(cat .env); do
  key="${line%%=*}"
  val="${line#*=}"
  [[ "$key" =~ ^[A-Z_]+$ ]] || continue
  val="${val%\"}"; val="${val#\"}"; val="${val%\'}"; val="${val#\'}"
  export "$key=$val"
done

DB_URL="${DATABASE_URL:?DATABASE_URL missing in .env}"
DB_NAME=$(echo "$DB_URL" | sed -E 's|.*/([a-zA-Z_0-9]+)\??.*|\1|')

fail() { echo "[$STAMP] ❌ DRILL FAILED: $*"; exit 1; }
warn() { echo "[$STAMP] ⚠️  $*"; }

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

# Staleness check BEFORE doing any work — a stale pipeline is the emergency.
AGE_HOURS=$(( ( $(date +%s) - $(stat -c%Y "$FILE") ) / 3600 ))
if [ "$AGE_HOURS" -gt "$MAX_AGE_HOURS" ]; then
  echo "[$STAMP] ❌ DRILL FAILED: newest backup is ${AGE_HOURS}h old (limit ${MAX_AGE_HOURS}h)."
  echo "[$STAMP]    The file itself may restore fine — but the BACKUP PIPELINE is stale."
  echo "[$STAMP]    Check: crontab -l | grep db-backup ; tail /home/fidele/db-backups/backup.log"
  echo "[$STAMP]    (override with MAX_AGE_HOURS=<n> bash $0 to test an old file anyway)"
  exit 1
fi
echo "[$STAMP] Backup age: ${AGE_HOURS}h (limit ${MAX_AGE_HOURS}h) OK"

SCRATCH="restore_drill_$(date '+%Y%m%d_%H%M%S')"
sudo mysql -N -B -e "SELECT 1" >/dev/null 2>&1 || fail "no mysql root via 'sudo mysql'"

# ── 3. Restore (as root over the socket — no app-user grants needed) ────────
echo "[$STAMP] Restoring into scratch DB: $SCRATCH …"
sudo mysql -e "CREATE DATABASE \`$SCRATCH\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
if ! gunzip < "$FILE" | sudo mysql "$SCRATCH" 2>/tmp/restore-drill.err; then
  echo "[$STAMP] Restore errors (kept scratch DB $SCRATCH for inspection):"
  tail -5 /tmp/restore-drill.err
  echo "[$STAMP] Clean up later with: sudo mysql -e 'DROP DATABASE \`$SCRATCH\`'"
  exit 1
fi
rm -f /tmp/restore-drill.err

# ── 4. Verify (root socket everywhere) ──────────────────────────────────────
q() { sudo mysql -N -B -e "$1" 2>/dev/null; }
get_count() { q "SELECT COUNT(*) FROM \`$1\`.\`$2\`"; }

LIVE_TABLES=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")
DRILL_TABLES=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SCRATCH'")
echo "[$STAMP] Tables: live=$LIVE_TABLES restored=$DRILL_TABLES"
if [ "$DRILL_TABLES" -gt "$LIVE_TABLES" ]; then
  fail "restored schema has MORE tables than live — impossible/corrupt dump"
elif [ "$DRILL_TABLES" -lt "$LIVE_TABLES" ]; then
  warn "restored schema has fewer tables than live (migrations ran since the backup) — expected for old backups"
fi

CHECKS=0
GAP_WORST=0
for t in users user_subscriptions tests questions learning_materials pdf_files \
         transactions test_attempts user_activities irembo_driving_license_requests; do
  EXISTS=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SCRATCH' AND table_name='$t'")
  [ "$EXISTS" -eq 1 ] || continue
  LIVE=$(get_count "$DB_NAME" "$t"); REST=$(get_count "$SCRATCH" "$t")
  if [ "$REST" -eq 0 ]; then
    fail "core table $t is EMPTY in the restored copy"
  elif [ "$REST" -gt "$LIVE" ]; then
    fail "$t restored ($REST) > live ($LIVE) — corrupt/inconsistent dump"
  fi
  GAP=$((LIVE - REST))
  [ "$GAP" -gt "$GAP_WORST" ] && GAP_WORST=$GAP
  echo "[$STAMP]   ✅ $t: $REST rows (live=$LIVE, gap=$GAP)"
  CHECKS=$((CHECKS+1))
done
[ "$CHECKS" -ge 3 ] || fail "fewer than 3 core tables found — verify table names"

FRESH_LIVE=$(q "SELECT MAX(created_at) FROM \`$DB_NAME\`.users")
FRESH_REST=$(q "SELECT MAX(created_at) FROM \`$SCRATCH\`.users")
echo "[$STAMP] Newest user row: live=$FRESH_LIVE restored=$FRESH_REST"
if [[ "$FRESH_REST" > "$FRESH_LIVE" ]]; then
  fail "restored data contains rows NEWER than the live DB — clock/corruption problem"
fi
echo "[$STAMP] Data gap vs live ≈ $GAP_WORST rows (organic growth since the backup — informational, not an error)"

sudo mysql -e "DROP DATABASE \`$SCRATCH\`"
echo "[$STAMP] Scratch DB dropped."
echo "[$STAMP] ✅ DRILL PASSED — backup is restorable (${AGE_HOURS}h old, $DRILL_TABLES tables, $CHECKS core tables verified)"
