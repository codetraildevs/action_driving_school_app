#!/bin/bash
# ──────────────────────────────────────────────────────────────────
# go-live.sh — ONE command to set up and deploy everything
#
# First time (as root):
#   bash go-live.sh
#
# Future updates (as deploy user):
#   bash go-live.sh update
#
# What it does:
#   FIRST TIME: Installs everything, clones repo, builds, starts app
#   UPDATE:     Pulls code, rebuilds, zero-downtime restart
# ──────────────────────────────────────────────────────────────────

set -euo pipefail

# ── Config ──
APP_DIR="/home/project3"
APP_USER="deploy"
APP_NAME="driving-school"
NODE_VERSION="22"
DB_NAME="sxlvhdzo_driving_school"
DB_USER="sxlvhdzo_admin"
DOMAIN="console.amategekoyumuhanda.rw"
REPO_URL=""  # Set your git repo URL here

# ── Colors ──
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()   { echo -e "${GREEN}[✅]${NC} $1"; }
warn()  { echo -e "${YELLOW}[⚠️]${NC} $1"; }
error() { echo -e "${RED}[❌]${NC} $1"; exit 1; }
info()  { echo -e "${BLUE}[ℹ️]${NC} $1"; }

# ══════════════════════════════════════════════════════════════════
#  UPDATE MODE — Pull, build, restart
# ══════════════════════════════════════════════════════════════════
if [[ "${1:-}" == "update" ]]; then
    echo ""
    echo "═══════════════════════════════════════════════════════"
    echo "  🚀 Deploying update..."
    echo "═══════════════════════════════════════════════════════"
    echo ""

    cd "$APP_DIR"

    # Save current build ID for rollback
    OLD_BUILD=""
    if [ -f ".next/BUILD_ID" ]; then
        OLD_BUILD=$(cat .next/BUILD_ID)
        info "Current build: $OLD_BUILD"
    fi

    # 1. Pull
    log "[1/5] Pulling latest code..."
    git pull origin feature/vps-migration || error "git pull failed"

    # 2. Dependencies
    log "[2/5] Installing dependencies..."
    npm install --production=false 2>&1 | tail -3

    # 3. Prisma
    log "[3/5] Generating Prisma client..."
    npx prisma generate 2>&1 | tail -2

    # 4. Build
    log "[4/5] Building Next.js..."
    npm run build 2>&1 | tail -5

    NEW_BUILD=$(cat .next/BUILD_ID)
    if [ "$OLD_BUILD" = "$NEW_BUILD" ]; then
        warn "Build ID unchanged ($NEW_BUILD) — code may not have changed"
    fi

    # 5. Zero-downtime restart
    log "[5/5] Restarting with PM2..."
    pm2 reload ecosystem.config.js --env production 2>&1 | tail -5

    echo ""
    echo "═══════════════════════════════════════════════════════"
    echo -e "  ${GREEN}✅ Deploy complete!${NC}"
    echo "  Build: $(cat .next/BUILD_ID)"
    echo "  Time:  $(date)"
    echo ""
    pm2 status
    echo "═══════════════════════════════════════════════════════"
    exit 0
fi

# ══════════════════════════════════════════════════════════════════
#  FIRST-TIME SETUP — Full server provisioning
# ══════════════════════════════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  🚀 Action Driving School — VPS Setup"
echo "═══════════════════════════════════════════════════════"
echo ""

# ── Detect if running as root ──
if [ "$(id -u)" -ne 0 ]; then
    error "Run as root: sudo bash go-live.sh"
fi

# ── Step 1: System Update ──
log "[1/10] Updating system..."
apt update -qq && apt upgrade -y -qq 2>&1 | tail -3

# ── Step 2: Install Node.js ──
log "[2/10] Installing Node.js ${NODE_VERSION}..."
if ! command -v node &>/dev/null || [[ "$(node -v)" != "v${NODE_VERSION}"* ]]; then
    curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" | bash - 2>&1 | tail -2
    apt install -y -qq nodejs 2>&1 | tail -2
fi
info "Node $(node -v) | npm $(npm -v)"

# ── Step 3: Install MySQL ──
log "[3/10] Installing MySQL..."
if ! command -v mysql &>/dev/null; then
    apt install -y -qq mysql-server 2>&1 | tail -2
fi
systemctl enable mysql 2>/dev/null || true
systemctl start mysql 2>/dev/null || true

# Create database + user
mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`;" 2>/dev/null || true
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY 'CHANGE_ME_NOW';" 2>/dev/null || true
mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';" 2>/dev/null || true
mysql -e "FLUSH PRIVILEGES;" 2>/dev/null || true
log "Database '${DB_NAME}' ready"

# ── Step 4: Install PM2 ──
log "[4/10] Installing PM2..."
if ! command -v pm2 &>/dev/null; then
    npm install -g pm2 2>&1 | tail -2
fi

# ── Step 5: Install Nginx ──
log "[5/10] Installing Nginx..."
if ! command -v nginx &>/dev/null; then
    apt install -y -qq nginx 2>&1 | tail -2
fi
systemctl enable nginx 2>/dev/null || true
systemctl start nginx 2>/dev/null || true

# ── Step 6: Install Certbot ──
log "[6/10] Installing Certbot..."
if ! command -v certbot &>/dev/null; then
    apt install -y -qq certbot python3-certbot-nginx 2>&1 | tail -2
fi

# ── Step 7: Create deploy user ──
log "[7/10] Creating deploy user..."
if ! id "$APP_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$APP_USER" 2>&1 | tail -2
    usermod -aG sudo "$APP_USER" 2>/dev/null || true
    echo "${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/pm2, /usr/bin/git, /usr/bin/npm, /usr/bin/node, /usr/local/bin/npx, /usr/local/bin/pm2" > "/etc/sudoers.d/${APP_USER}"
    chmod 440 "/etc/sudoers.d/${APP_USER}"
    log "User '${APP_USER}' created with PM2 sudo access"
else
    log "User '${APP_USER}' already exists"
fi

# Create directories
mkdir -p "$APP_DIR" /home/logs
chown "${APP_USER}:${APP_USER}" "$APP_DIR" /home/logs

# ── Step 8: Configure Firewall ──
log "[8/10] Configuring firewall..."
if ! ufw status | grep -q "active"; then
    apt install -y -qq ufw 2>&1 | tail -2
    ufw allow 22/tcp 2>/dev/null
    ufw allow 80/tcp 2>/dev/null
    ufw allow 443/tcp 2>/dev/null
    ufw --force enable 2>&1 | tail -2
fi
log "Firewall: SSH(22), HTTP(80), HTTPS(443) open"

# ── Step 9: Clone & Build App ──
log "[9/10] Setting up application..."

if [ ! -f "${APP_DIR}/package.json" ]; then
    if [ -z "$REPO_URL" ]; then
        warn "No REPO_URL set — skipping clone"
        warn "Run manually: cd ${APP_DIR} && git clone <your-repo> ."
    else
        cd "$APP_DIR"
        git clone "$REPO_URL" . 2>&1 | tail -3
        log "Repository cloned"
    fi
fi

# Build if needed
if [ -f "${APP_DIR}/package.json" ]; then
    cd "$APP_DIR"

    # .env file
    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            cp .env.example .env
            warn ".env created from .env.example — EDIT IT with real values!"
        else
            warn "No .env file — create one before starting the app"
        fi
    fi

    # Install + build
    log "Installing dependencies..."
    sudo -u "$APP_USER" bash -c "cd ${APP_DIR} && npm install --production=false" 2>&1 | tail -3

    log "Generating Prisma client..."
    sudo -u "$APP_USER" bash -c "cd ${APP_DIR} && npx prisma generate" 2>&1 | tail -2

    log "Building Next.js..."
    sudo -u "$APP_USER" bash -c "cd ${APP_DIR} && npm run build" 2>&1 | tail -5

    # Copy ecosystem config
    if [ ! -f "${APP_DIR}/ecosystem.config.js" ]; then
        warn "ecosystem.config.js not found — PM2 config may be missing"
    fi

    # Start with PM2
    log "Starting app with PM2..."
    sudo -u "$APP_USER" bash -c "cd ${APP_DIR} && pm2 start ecosystem.config.js --env production" 2>&1 | tail -5

    # Save PM2 process list + setup auto-start
    sudo -u "$APP_USER" pm2 save 2>&1 | tail -2
    sudo -u "$APP_USER" pm2 startup systemd -u "$APP_USER" --hp "/home/${APP_USER}" 2>&1 | tail -3
fi

# ── Step 10: Nginx Config ──
log "[10/10] Configuring Nginx..."

cat > /etc/nginx/sites-available/driving-school << 'NGINX'
upstream driving_school {
    server 127.0.0.1:3000;
    keepalive 64;
}

server {
    listen 80;
    server_name console.amategekoyumuhanda.rw;

    location /api/ {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        client_max_body_size 50M;
    }

    location = /api/health {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        access_log off;
    }

    location /api/sse {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 86400s;
        chunked_transfer_encoding off;
    }

    location /_next/static/ {
        proxy_pass http://driving_school;
        proxy_cache_valid 200 365d;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    location / {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml image/svg+xml;
}
NGINX

ln -sf /etc/nginx/sites-available/driving-school /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t 2>&1 && systemctl reload nginx
log "Nginx configured"

# ══════════════════════════════════════════════════════════════════
#  DONE
# ══════════════════════════════════════════════════════════════════
echo ""
echo "═══════════════════════════════════════════════════════"
echo -e "  ${GREEN}✅ SETUP COMPLETE!${NC}"
echo ""
echo "  Server is running at: http://$(hostname -I | awk '{print $1}'):3000"
echo ""
echo "  ⚡ IMMEDIATE ACTIONS:"
echo "  ─────────────────────"
echo "  1. Edit .env with real database password:"
echo "     nano ${APP_DIR}/.env"
echo ""
echo "  2. Point DNS to this server IP:"
echo "     ${DOMAIN} → A → $(hostname -I | awk '{print $1}')"
echo ""
echo "  3. Setup SSL (after DNS propagates):"
echo "     certbot --nginx -d ${DOMAIN}"
echo ""
echo "  📋 USEFUL COMMANDS:"
echo "  ─────────────────────"
echo "  pm2 status              — check app status"
echo "  pm2 logs driving-school — view live logs"
echo "  pm2 restart driving-school — restart app"
echo "  bash go-live.sh update  — deploy updates"
echo ""
echo "  🔄 FUTURE DEPLOYS:"
echo "  ─────────────────────"
echo "  Just run: bash go-live.sh update"
echo "═══════════════════════════════════════════════════════"
