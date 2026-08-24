#!/bin/bash
# ──────────────────────────────────────────────────────────────────
# vps-setup.sh — One-time VPS setup for Action Driving School
#
# Run as root on a fresh Ubuntu 22.04/24.04 VPS:
#   curl -O https://your-server/vps-setup.sh && bash vps-setup.sh
#
# What it does:
#   1. Installs Node.js 22, MySQL, PM2, Nginx, Certbot
#   2. Creates deploy user with sudo
#   3. Configures firewall (UFW)
#   4. Sets up PM2 to auto-start on boot
# ──────────────────────────────────────────────────────────────────

set -euo pipefail

APP_DIR="/home/project3"
APP_USER="deploy"
NODE_VERSION="22"
DB_NAME="sxlvhdzo_driving_school"
DB_USER="sxlvhdzo_admin"

echo "═══════════════════════════════════════════════════════"
echo "  Action Driving School — VPS Setup"
echo "═══════════════════════════════════════════════════════"

# ── 1. System Update ──
echo "[1/8] Updating system..."
apt update && apt upgrade -y

# ── 2. Install Node.js 22 ──
echo "[2/8] Installing Node.js ${NODE_VERSION}..."
curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" | bash -
apt install -y nodejs
echo "Node: $(node -v) | npm: $(npm -v)"

# ── 3. Install MySQL ──
echo "[3/8] Installing MySQL..."
apt install -y mysql-server
systemctl enable mysql
systemctl start mysql

# Create database and user
mysql -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`;"
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY 'CHANGE_THIS_PASSWORD';"
mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"
echo "✅ Database '${DB_NAME}' ready — CHANGE THE PASSWORD in .env!"

# ── 4. Install PM2 ──
echo "[4/8] Installing PM2..."
npm install -g pm2

# ── 5. Install Nginx ──
echo "[5/8] Installing Nginx..."
apt install -y nginx
systemctl enable nginx
systemctl start nginx

# ── 6. Install Certbot (SSL) ──
echo "[6/8] Installing Certbot..."
apt install -y certbot python3-certbot-nginx

# ── 7. Create deploy user ──
echo "[7/8] Creating deploy user..."
if ! id "$APP_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$APP_USER"
    usermod -aG sudo "$APP_USER"
    # Allow deploy user to restart PM2 without password
    echo "${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/pm2" > "/etc/sudoers.d/${APP_USER}"
    chmod 440 "/etc/sudoers.d/${APP_USER}"
fi

# Create app directory
mkdir -p "$APP_DIR"
chown "${APP_USER}:${APP_USER}" "$APP_DIR"

# Create logs directory
mkdir -p /home/logs
chown "${APP_USER}:${APP_USER}" /home/logs

# ── 8. Configure Firewall ──
echo "[8/8] Configuring firewall..."
apt install -y ufw
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw --force enable

# ── Setup PM2 startup (as deploy user) ──
echo ""
echo "═══════════════════════════════════════════════════════"
echo "  ✅ Setup complete!"
echo ""
echo "  Next steps:"
echo "  1. su - ${APP_USER}"
echo "  2. cd ${APP_DIR}"
echo "  3. git clone <your-repo> ."
echo "  4. cp .env.example .env   (edit with real values)"
echo "  5. npm install"
echo "  6. npx prisma generate"
echo "  7. npm run build"
echo "  8. pm2 start ecosystem.config.js"
echo "  9. pm2 save"
echo "  10. pm2 startup   (run the command it prints)"
echo ""
echo "  Then configure Nginx + SSL:"
echo "  11. Point domain DNS to this server IP"
echo "  12. certbot --nginx -d console.amategekoyumuhanda.rw"
echo "═══════════════════════════════════════════════════════"
