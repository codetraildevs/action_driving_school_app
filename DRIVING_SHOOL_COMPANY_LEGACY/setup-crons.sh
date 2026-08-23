#!/bin/bash
# setup-crons.sh — Configure cron jobs for auto-deploy and health check
#
# Run once:  bash /home/sxlvhdzo/project3/setup-crons.sh
#
# This sets up:
#   1. Health check every 5 minutes (auto-rebuilds if .next is missing)
#   2. Deploy on code push (if git is configured)
#
# To view cron jobs: crontab -l
# To remove cron jobs: crontab -r

APP_DIR="/home/sxlvhdzo/project3"
NODE_BIN="/home/sxlvhdzo/nodevenv/project3/22/bin"

echo "=== Setting up cron jobs ==="
echo ""

# Get current crontab
CURRENT_CRON=$(crontab -l 2>/dev/null || true)

# Check if health check is already configured
if echo "$CURRENT_CRON" | grep -q "healthcheck-cron.sh"; then
  echo "Health check cron already configured — skipping"
else
  echo "Adding health check cron (every 5 minutes)..."
  (echo "$CURRENT_CRON"; echo "*/5 * * * * bash $APP_DIR/healthcheck-cron.sh >> $APP_DIR/healthcheck.log 2>&1") | crontab -
  echo "  Added: */5 * * * * bash $APP_DIR/healthcheck-cron.sh"
fi

echo ""
echo "=== Cron jobs configured ==="
echo ""
echo "Current crontab:"
crontab -l 2>/dev/null || echo "(empty)"
echo ""
echo "To verify: crontab -l"
echo "To remove: crontab -r"
echo ""
echo "The health check will:"
echo "  1. Check .next/BUILD_ID every 5 minutes"
echo "  2. Auto-rebuild if missing (runs deploy.js)"
echo "  3. Check HTTP health and restart workers if needed"
echo "  4. Log everything to $APP_DIR/healthcheck.log"
