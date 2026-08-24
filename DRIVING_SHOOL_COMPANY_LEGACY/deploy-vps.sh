#!/bin/bash
# ──────────────────────────────────────────────────────────────────
# deploy-vps.sh — Deploy updates to the VPS
#
# Run as the deploy user:
#   ./deploy-vps.sh
#
# What it does:
#   1. Pulls latest code from git
#   2. Installs new dependencies
#   3. Runs Prisma generate
#   4. Builds Next.js
#   5. Restarts PM2 with zero downtime
# ──────────────────────────────────────────────────────────────────

set -euo pipefail

APP_DIR="/home/project3"
APP_NAME="driving-school"

echo "═══════════════════════════════════════════════════════"
echo "  Deploying ${APP_NAME}..."
echo "═══════════════════════════════════════════════════════"

cd "$APP_DIR"

# ── 1. Pull latest code ──
echo "[1/5] Pulling latest code..."
git pull origin feature/vps-migration

# ── 2. Install dependencies ──
echo "[2/5] Installing dependencies..."
npm install --production=false

# ── 3. Prisma generate ──
echo "[3/5] Running Prisma generate..."
npx prisma generate

# ── 4. Build Next.js ──
echo "[4/5] Building Next.js..."
npm run build

# ── 5. Restart PM2 (zero-downtime reload) ──
echo "[5/5] Restarting PM2..."
pm2 reload ecosystem.config.js --env production

# ── Done ──
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  ✅ Deploy complete!"
echo "  Build ID: $(cat .next/BUILD_ID)"
echo "  PM2 Status:"
pm2 status
echo "═══════════════════════════════════════════════════════"
