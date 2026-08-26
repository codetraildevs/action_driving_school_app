#!/bin/bash
# ──────────────────────────────────────────────────────────
# debug-login.sh — Diagnose login API issues on the VPS
#
# Usage:
#   chmod +x debug-login.sh
#   ./debug-login.sh
# ──────────────────────────────────────────────────────────

set -euo pipefail

echo "═══════════════════════════════════════════════════════"
echo "  Login Debug Script"
echo "═══════════════════════════════════════════════════════"

echo ""
echo "=== PM2 Status ==="
pm2 status

echo ""
echo "=== Last 20 Error Logs ==="
pm2 logs driving-school --err --lines 20 --nostream

echo ""
echo "=== Last 20 Output Logs ==="
pm2 logs driving-school --out --lines 20 --nostream

echo ""
echo "=== Test Health Endpoint ==="
curl -s http://localhost:3000/api/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:3000/api/health

echo ""
echo "=== Test Login API (POST with empty body) ==="
curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool 2>/dev/null || echo "Failed to connect"

echo ""
echo "=== Nginx Status ==="
sudo systemctl status nginx --no-pager | head -5

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Done. Check above for errors."
echo "═══════════════════════════════════════════════════════"
