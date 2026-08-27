#!/bin/bash
# healthcheck-cron.sh — Auto-rebuild when .next is missing or corrupt
#
# Run from cPanel → Cron Jobs (every 5 minutes):
#   bash /home/sxlvhdzo/project3/healthcheck-cron.sh
#
# What it does:
#   1. Checks if .next/BUILD_ID exists and is readable
#   2. If missing/corrupt → runs deploy.js to rebuild
#   3. Logs everything to healthcheck.log
#   4. Won't overlap with an existing deploy

set -e

APP_DIR="/home/sxlvhdzo/project3"
NODE_BIN="/home/sxlvhdzo/nodevenv/project3/22/bin"
LOG="$APP_DIR/healthcheck.log"
LOCK="$APP_DIR/.deploy.lock"
BUILD_ID_FILE="$APP_DIR/.next/BUILD_ID"

# Rotate log if it gets too big (>1MB)
if [ -f "$LOG" ] && [ $(stat -c%s "$LOG" 2>/dev/null || echo 0) -gt 1048576 ]; then
  mv "$LOG" "$LOG.old"
fi

exec >> "$LOG" 2>&1

log() {
  echo "[$(date '+%F %T')] $1"
}

# --- Check 1: Is .next/BUILD_ID present and non-empty? ---
if [ -f "$BUILD_ID_FILE" ] && [ -s "$BUILD_ID_FILE" ]; then
  BUILD_ID=$(cat "$BUILD_ID_FILE")
  log "OK — .next exists (BUILD_ID: $BUILD_ID)"
  exit 0
fi

# --- Check 2: Is a deploy already running? ---
if [ -f "$LOCK" ]; then
  LOCK_PID=$(cat "$LOCK" 2>/dev/null)
  if [ -n "$LOCK_PID" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
    log "SKIP — deploy already running (PID: $LOCK_PID)"
    exit 0
  else
    log "STALE LOCK found — removing"
    rm -f "$LOCK"
  fi
fi

# --- Check 3: .next is missing or corrupt → auto-rebuild ---
log "⚠ .next/BUILD_ID is MISSING or empty — triggering auto-rebuild..."
log "  Build ID file: $BUILD_ID_FILE"

cd "$APP_DIR"

# Run deploy.js
log "Running deploy.js..."
"$NODE_BIN/node" "$APP_DIR/deploy.js"
DEPLOY_EXIT=$?

if [ $DEPLOY_EXIT -eq 0 ]; then
  log "✅ Auto-rebuild completed successfully"
else
  log "❌ Auto-rebuild FAILED (exit code: $DEPLOY_EXIT)"
  log "  Check deploy.log for details"
fi

exit $DEPLOY_EXIT
