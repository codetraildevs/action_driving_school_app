#!/bin/bash
# ============================================================================
# db-backup.sh — Nightly MySQL backup with retention for the driving-school DB.
#
# Designed to run from fidele's crontab on the VPS:
#   30 2 * * * /home/project3/scripts/db-backup.sh >> /home/fidele/db-backups/backup.log 2>&1
#
# Reads credentials from /home/project3/.env (same parsing as dedupe-duplicates.sh).
# Uses --single-transaction so InnoDB dumps are consistent without locking.
# Retention: keeps the newest $RETENTION_DAYS daily backups, deletes older ones.
#
# Manual run:  bash /home/project3/scripts/db-backup.sh
# Restore:     gunzip < FILE.sql.gz | mysql -h HOST -u USER -p DBNAME
# ============================================================================
set -euo pipefail

PROJECT_DIR="/home/project3"
BACKUP_DIR="/home/fidele/db-backups"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
STAMP="$(date '+%Y-%m-%d %H:%M:%S %Z')"

cd "$PROJECT_DIR"

# ── Load .env (strip optional surrounding quotes) ───────────────────────────
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

mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/${DB_NAME}_$(date '+%Y%m%d_%H%M%S').sql.gz"

# ── Dump + compress (single transaction → consistent snapshot, no locks) ───
if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
    --single-transaction --quick --routines --triggers --events --hex-blob \
    "$DB_NAME" 2>/dev/null | gzip > "$FILE"; then
  :
else
  echo "[$STAMP] ERROR: mysqldump failed — removing partial file"
  rm -f "$FILE"
  exit 1
fi

# ── Verify the archive is complete and non-empty ────────────────────────────
if ! gzip -t "$FILE" 2>/dev/null || [ ! -s "$FILE" ]; then
  echo "[$STAMP] ERROR: backup file failed integrity check: $FILE"
  rm -f "$FILE"
  exit 1
fi

SIZE=$(du -h "$FILE" | cut -f1)

# ── Off-site copy (rclone → B2) ─────────────────────────────────────────
# Non-fatal by design: the local backup stays the source of truth; a failed
# upload must never stop the nightly dump. Requires a configured rclone
# remote named "b2" (see docs/incident-report-2026-09.md §8.5).
if command -v rclone >/dev/null 2>&1 && rclone listremotes 2>/dev/null | grep -qi '^b2:$'; then
  if rclone copy "$FILE" "b2:${B2_BUCKET:-driving-school-backups}/mysql" --transfers 2 --checkers 4; then
    echo "[$STAMP] off-site copy uploaded to b2:${B2_BUCKET:-driving-school-backups}"
  else
    echo "[$STAMP] WARN: off-site upload FAILED — local backup intact, check rclone/b2"
  fi
else
  echo "[$STAMP] note: rclone remote 'b2' not configured — skipping off-site copy"
fi

# ── Retention: delete daily backups older than $RETENTION_DAYS ─────────────
DELETED=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -print -delete 2>/dev/null | wc -l)

KEPT=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" | wc -l)
echo "[$STAMP] OK: $FILE ($SIZE) — kept=$KEPT, expired=$DELETED"
