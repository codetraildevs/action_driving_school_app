#!/bin/bash
# ──────────────────────────────────────────────────────────
# test-ssl.sh — Diagnose SSL and port 80 connectivity
#
# Usage:
#   chmod +x test-ssl.sh
#   ./test-ssl.sh
# ──────────────────────────────────────────────────────────

set -euo pipefail

echo "═══════════════════════════════════════════════════════"
echo "  SSL & Port 80 Diagnostic Script"
echo "═══════════════════════════════════════════════════════"

echo ""
echo "=== Public IP ==="
curl -s ifconfig.me

echo ""
echo "=== Domain DNS Resolution ==="
dig +short console.amategekoyumuhanda.rw 2>/dev/null || echo "dig not installed"

echo ""
echo "=== Firewall (port 80) ==="
sudo ufw status | grep 80 || echo "No UFW rule for port 80"

echo ""
echo "=== Nginx Sites Enabled ==="
ls -la /etc/nginx/sites-enabled/

echo ""
echo "=== Testing port 80 locally ==="
curl -I http://localhost:80 2>/dev/null | head -5

echo ""
echo "=== Testing HTTPS externally ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" https://console.amategekoyumuhanda.rw/

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  Done."
echo "═══════════════════════════════════════════════════════"
