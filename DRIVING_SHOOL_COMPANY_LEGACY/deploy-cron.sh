#!/bin/bash
# rwanda_app_backend — scheduled rebuild + restart for cPanel (Phusion Passenger)
# Run from cPanel -> Cron Jobs with:  bash /home/sxlvhdzo/project1/deploy-cron.sh
# All output goes to deploy-cron.log next to this script.

set -e

APP_DIR="/home/sxlvhdzo/project1"                      # app root (where package.json + .next live)
NODE_BIN="/home/sxlvhdzo/nodevenv/project1/22/bin"     # cPanel nodevenv bin (node/npm/npx)
LOG="$APP_DIR/deploy-cron.log"
LOCK="$APP_DIR/.deploy.lock"

exec >> "$LOG" 2>&1

# Avoid overlapping runs (previous build still going)
if [ -f "$LOCK" ]; then
  echo "$(date '+%F %T'): deploy already running — skipping this run"
  exit 0
fi
touch "$LOCK"
trap 'rm -f "$LOCK"' EXIT

echo "$(date '+%F %T'): ===== deploy start ====="
cd "$APP_DIR"

# Optional: pull the latest code first if you ever deploy via git
# git pull origin main

echo "$(date '+%F %T'): [1/3] npm install --legacy-peer-deps"
"$NODE_BIN/npm" install --legacy-peer-deps

echo "$(date '+%F %T'): [2/3] prisma generate"
"$NODE_BIN/npx" prisma generate

echo "$(date '+%F %T'): [3/3] next build"
"$NODE_BIN/npm" run build

echo "$(date '+%F %T'): restarting app (touch tmp/restart.txt)"
mkdir -p "$APP_DIR/tmp"
touch "$APP_DIR/tmp/restart.txt"

echo "$(date '+%F %T'): ===== deploy done ====="

# --- Auto-rebuild if .next is missing (safety net) ---
if [ ! -f "$APP_DIR/.next/BUILD_ID" ]; then
  echo "$(date '+%F %T'): ⚠ .next/BUILD_ID is MISSING after deploy — running healthcheck..."
  "$NODE_BIN/node" "$APP_DIR/healthcheck.js"
fi
