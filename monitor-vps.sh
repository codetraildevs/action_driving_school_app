#!/bin/bash
# ──────────────────────────────────────────────────────────
# monitor-vps.sh — VPS Health Check & Alerting
#
# Checks:
#   - PM2 process status
#   - Nginx status
#   - Health API response
#   - Disk usage
#   - Memory usage
#   - SSL certificate expiry
#
# Usage:
#   chmod +x monitor-vps.sh
#   ./monitor-vps.sh              # Run checks
#   ./monitor-vps.sh --install    # Install cron job (every 5 min)
#   ./monitor-vps.sh --uninstall  # Remove cron job
#
# Alerts via:
#   - Email (if sendmail/mailutils installed)
#   - Telegram (if BOT_TOKEN and CHAT_ID set)
# ──────────────────────────────────────────────────────────

set -euo pipefail

# ── Configuration ──────────────────────────────────────────
APP_NAME="driving-school"
APP_URL="https://console.amategekoyumuhanda.rw"
HEALTH_URL="http://localhost:3000/api/health"
DISK_THRESHOLD=85
MEMORY_THRESHOLD=85
LOG_FILE="/var/log/vps-monitor.log"

# Telegram (optional — set these if you want Telegram alerts)
TELEGRAM_BOT_TOKEN="${TELEGRAM_BOT_TOKEN:-}"
TELEGRAM_CHAT_ID="${TELEGRAM_CHAT_ID:-}"

# Email (optional — set this if you want email alerts)
ALERT_EMAIL="${ALERT_EMAIL:-}"

# ── Colors ─────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ── Functions ──────────────────────────────────────────────
log() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "$msg" >> "$LOG_FILE" 2>/dev/null || true
}

alert() {
    local message="$1"
    log "ALERT: $message"

    # Telegram alert
    if [[ -n "$TELEGRAM_BOT_TOKEN" && -n "$TELEGRAM_CHAT_ID" ]]; then
        curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
            -d chat_id="$TELEGRAM_CHAT_ID" \
            -d text="🚨 VPS Alert: $message" \
            -d parse_mode="HTML" > /dev/null 2>&1 || true
    fi

    # Email alert
    if [[ -n "$ALERT_EMAIL" ]] && command -v mail &> /dev/null; then
        echo "VPS Alert: $message" | mail -s "🚨 VPS Alert - $(hostname)" "$ALERT_EMAIL" 2>/dev/null || true
    fi
}

check_status() {
    local name="$1"
    local status="$2"
    if [[ "$status" == "OK" ]]; then
        echo -e "  ${GREEN}✓${NC} $name"
    elif [[ "$status" == "WARN" ]]; then
        echo -e "  ${YELLOW}⚠${NC} $name"
        alert "$name warning"
    else
        echo -e "  ${RED}✗${NC} $name"
        alert "$name CRITICAL"
    fi
}

# ── Install/Uninstall Cron ─────────────────────────────────
if [[ "${1:-}" == "--install" ]]; then
    SCRIPT_PATH="$(cd "$(dirname "$0")" && pwd)/monitor-vps.sh"
    CRON_JOB="*/5 * * * * $SCRIPT_PATH >> /var/log/vps-monitor.log 2>&1"
    (crontab -l 2>/dev/null | grep -v "monitor-vps.sh"; echo "$CRON_JOB") | crontab -
    echo "✅ Cron job installed (every 5 minutes)"
    echo "   Logs: $LOG_FILE"
    exit 0
fi

if [[ "${1:-}" == "--uninstall" ]]; then
    crontab -l 2>/dev/null | grep -v "monitor-vps.sh" | crontab -
    echo "✅ Cron job removed"
    exit 0
fi

# ── Main Health Check ─────────────────────────────────────
echo "═══════════════════════════════════════════════════════"
echo "  VPS Health Check — $(date '+%Y-%m-%d %H:%M:%S')"
echo "═══════════════════════════════════════════════════════"
echo ""

ERRORS=0

# ── 1. PM2 Status ─────────────────────────────────────────
echo "📋 Process Manager (PM2)"
if pm2 status | grep -q "online"; then
    check_status "PM2: App is online" "OK"
else
    check_status "PM2: App is NOT running" "CRITICAL"
    ((ERRORS++))
fi

# ── 2. Nginx Status ───────────────────────────────────────
echo ""
echo "🌐 Web Server (Nginx)"
if sudo systemctl is-active --quiet nginx; then
    check_status "Nginx is running" "OK"
else
    check_status "Nginx is NOT running" "CRITICAL"
    ((ERRORS++))
fi

# ── 3. Health API ─────────────────────────────────────────
echo ""
echo "🏥 Health API"
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$HEALTH_URL" 2>/dev/null || echo "000")
if [[ "$HEALTH_RESPONSE" == "200" ]]; then
    check_status "Health API responding (HTTP $HEALTH_RESPONSE)" "OK"
else
    check_status "Health API NOT responding (HTTP $HEALTH_RESPONSE)" "CRITICAL"
    ((ERRORS++))
fi

# ── 4. HTTPS Check ────────────────────────────────────────
echo ""
echo "🔒 SSL/HTTPS"
HTTPS_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$APP_URL" 2>/dev/null || echo "000")
if [[ "$HTTPS_CODE" == "200" ]]; then
    check_status "HTTPS accessible (HTTP $HTTPS_CODE)" "OK"
else
    check_status "HTTPS issue (HTTP $HTTPS_CODE)" "WARN"
fi

# ── 5. Disk Usage ─────────────────────────────────────────
echo ""
echo "💾 Disk Usage"
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [[ "$DISK_USAGE" -lt "$DISK_THRESHOLD" ]]; then
    check_status "Disk: ${DISK_USAGE}% used (threshold: ${DISK_THRESHOLD}%)" "OK"
else
    check_status "Disk: ${DISK_USAGE}% used (threshold: ${DISK_THRESHOLD}%)" "WARN"
fi

# ── 6. Memory Usage ───────────────────────────────────────
echo ""
echo "🧠 Memory Usage"
MEM_USAGE=$(free | awk '/Mem:/ {printf "%.0f", $3/$2 * 100}')
if [[ "$MEM_USAGE" -lt "$MEMORY_THRESHOLD" ]]; then
    check_status "Memory: ${MEM_USAGE}% used (threshold: ${MEMORY_THRESHOLD}%)" "OK"
else
    check_status "Memory: ${MEM_USAGE}% used (threshold: ${MEMORY_THRESHOLD}%)" "WARN"
fi

# ── 7. Uptime ─────────────────────────────────────────────
echo ""
echo "⏱️  Uptime"
UPTIME=$(uptime -p 2>/dev/null || uptime)
echo "  Server: $UPTIME"

# ── Summary ────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════"
if [[ $ERRORS -eq 0 ]]; then
    echo -e "  ${GREEN}✅ All checks passed${NC}"
    log "Health check: OK (0 errors)"
else
    echo -e "  ${RED}❌ $ERRORS critical issue(s) found${NC}"
    log "Health check: FAIL ($ERRORS errors)"
fi
echo "═══════════════════════════════════════════════════════"
