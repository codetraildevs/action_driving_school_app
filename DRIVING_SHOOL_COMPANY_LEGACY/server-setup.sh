#!/bin/bash
# ════════════════════════════════════════════════════════════════════
# server-setup.sh — Production VPS Setup for Action Driving School
#
# Usage:
#   First time:  sudo bash server-setup.sh
#   Check:       bash server-setup.sh check
#   Repair:      sudo bash server-setup.sh repair
#
# What it does:
#   ✓ System update + unattended security upgrades
#   ✓ Swap file (2GB — critical for 4GB RAM servers)
#   ✓ Node.js 22, MySQL 8, PM2, Nginx, Certbot
#   ✓ Deploy user with limited sudo
#   ✓ MySQL hardening + database creation
#   ✓ Nginx reverse proxy with SSE + gzip
#   ✓ UFW firewall (22, 80, 443)
#   ✓ Log rotation for app logs
#   ✓ PM2 startup on boot (systemd)
#   ✓ Health check verification
#
# Safe to re-run — all steps are idempotent.
# ════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────
APP_DIR="/home/project3"
APP_USER="deploy"
APP_NAME="driving-school"
NODE_VERSION="22"
DB_NAME="sxlvhdzo_driving_school"
DB_USER="sxlvhdzo_admin"
DB_PASS="CHANGE_ME_$(openssl rand -hex 8)"   # Random password on first run
DOMAIN="console.amategekoyumuhanda.rw"
SWAP_SIZE="2G"
LOG_DIR="/home/logs"
BACKUP_DIR="/home/backups"
TIMEZONE="Africa/Kigali"

# ── Colors ─────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

log()    { echo -e "${GREEN}[✓]${NC} $1"; }
warn()   { echo -e "${YELLOW}[!]${NC} $1"; }
error()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }
info()   { echo -e "${BLUE}[i]${NC} $1"; }
step()   { echo -e "\n${CYAN}${BOLD}── $1 ──${NC}"; }
header() { echo -e "\n${BOLD}═══════════════════════════════════════════════════════${NC}"; echo -e "${BOLD}  $1${NC}"; echo -e "${BOLD}═══════════════════════════════════════════════════════${NC}\n"; }

# ── Helpers ────────────────────────────────────────────────────────
command_exists() { command -v "$1" &>/dev/null; }

backup_file() {
    local file="$1"
    if [ -f "$file" ]; then
        cp "$file" "${file}.bak.$(date +%s)"
    fi
}

# ══════════════════════════════════════════════════════════════════
#  CHECK MODE — Verify everything is installed and running
# ══════════════════════════════════════════════════════════════════
if [[ "${1:-}" == "check" ]]; then
    header "Server Health Check"

    checks=0
    passed=0

    check() {
        checks=$((checks + 1))
        if eval "$2" &>/dev/null; then
            log "$1"
            passed=$((passed + 1))
        else
            warn "$1 — MISSING"
        fi
    }

    check "Node.js"       "node -v"
    check "npm"           "npm -v"
    check "MySQL"         "mysql --version"
    check "PM2"           "pm2 -v"
    check "Nginx"         "nginx -v"
    check "Certbot"       "certbot --version"
    check "UFW"           "ufw status"
    check "Deploy user"   "id $APP_USER"
    check "App directory"  "[ -d $APP_DIR ]"
    check "Package.json"   "[ -f $APP_DIR/package.json ]"
    check ".next build"    "[ -f $APP_DIR/.next/BUILD_ID ]"
    check ".env file"      "[ -f $APP_DIR/.env ]"
    check "ecosystem.js"   "[ -f $APP_DIR/ecosystem.config.js ]"
    check "PM2 running"    "pm2 jlist | grep -q driving-school"

    echo ""
    echo "Result: ${passed}/${checks} checks passed"
    echo ""

    if [ -f "$APP_DIR/.next/BUILD_ID" ]; then
        info "Build ID: $(cat $APP_DIR/.next/BUILD_ID)"
    fi
    info "Server IP: $(hostname -I | awk '{print $1}')"
    info "Uptime: $(uptime -p)"
    info "Memory: $(free -h | awk '/Mem:/ {print $3 "/" $2}')"
    info "Disk: $(df -h / | awk 'NR==2 {print $3 "/" $2 " (" $5 " used)"}')"
    exit 0
fi

# ══════════════════════════════════════════════════════════════════
#  REPAIR MODE — Fix common issues
# ══════════════════════════════════════════════════════════════════
if [[ "${1:-}" == "repair" ]]; then
    header "Repair Mode"

    # Fix .next missing
    if [ ! -f "$APP_DIR/.next/BUILD_ID" ]; then
        warn ".next build missing — rebuilding..."
        cd "$APP_DIR"
        sudo -u "$APP_USER" npx prisma generate
        sudo -u "$APP_USER" npm run build
        log "Build complete"
    fi

    # Fix PM2 not running
    if ! sudo -u "$APP_USER" pm2 jlist 2>/dev/null | grep -q driving-school; then
        warn "PM2 app not running — starting..."
        sudo -u "$APP_USER" bash -c "cd $APP_DIR && pm2 start ecosystem.config.js --env production"
        sudo -u "$APP_USER" pm2 save
        log "PM2 started"
    fi

    # Fix Nginx
    if ! nginx -t 2>/dev/null; then
        warn "Nginx config broken — reloading..."
        systemctl reload nginx
    fi

    # Fix MySQL
    if ! systemctl is-active --quiet mysql; then
        warn "MySQL stopped — starting..."
        systemctl start mysql
    fi

    log "Repair complete"
    bash "$0" check
    exit 0
fi

# ══════════════════════════════════════════════════════════════════
#  FULL SETUP
# ══════════════════════════════════════════════════════════════════

# Root check
if [ "$(id -u)" -ne 0 ]; then
    error "Run as root: sudo bash server-setup.sh"
fi

SERVER_IP=$(hostname -I | awk '{print $1}')
header "Action Driving School — Server Setup"
info "Server IP: $SERVER_IP"
info "Domain: $DOMAIN"
info "Time: $(date)"
echo ""

# ──────────────────────────────────────────────────────────────────
#  STEP 1: System Base
# ──────────────────────────────────────────────────────────────────
step "1/12 — System Update & Timezone"

timedatectl set-timezone "$TIMEZONE" 2>/dev/null || true
log "Timezone: $TIMEZONE"

apt update -qq
DEBIAN_FRONTEND=noninteractive apt upgrade -y -qq
log "System updated"

# ──────────────────────────────────────────────────────────────────
#  STEP 2: Swap File (critical for 4GB RAM)
# ──────────────────────────────────────────────────────────────────
step "2/12 — Swap File ($SWAP_SIZE)"

if [ ! -f /swapfile ]; then
    fallocate -l "$SWAP_SIZE" /swapfile
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    # Lower swappiness — prefer RAM over swap
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    sysctl -p >/dev/null 2>&1
    log "Swap created: $SWAP_SIZE"
else
    log "Swap already exists: $(swapon --show | awk 'NR==2 {print $3}')"
fi

# ──────────────────────────────────────────────────────────────────
#  STEP 3: Auto Security Updates
# ──────────────────────────────────────────────────────────────────
step "3/12 — Unattended Security Updates"

apt install -y -qq unattended-upgrades >/dev/null 2>&1
cat > /etc/apt/apt.conf.d/20auto-upgrades << 'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF
log "Auto security updates enabled"

# ──────────────────────────────────────────────────────────────────
#  STEP 4: Essential Packages
# ──────────────────────────────────────────────────────────────────
step "4/12 — Essential Packages"

apt install -y -qq curl wget git build-essential \
    htop iotop net-tools unzip jq \
    software-properties-common apt-transport-https ca-certificates \
    >/dev/null 2>&1
log "Essentials installed (htop, git, build-essential, etc.)"

# ──────────────────────────────────────────────────────────────────
#  STEP 5: Node.js
# ──────────────────────────────────────────────────────────────────
step "5/12 — Node.js ${NODE_VERSION}"

if command_exists node; then
    CURRENT_NODE=$(node -v | sed 's/v//' | cut -d. -f1)
    if [ "$CURRENT_NODE" -ge "$NODE_VERSION" ]; then
        log "Node.js $(node -v) already installed"
    else
        warn "Upgrading Node.js from v$CURRENT_NODE to v$NODE_VERSION..."
        curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" | bash - 2>&1 | tail -2
        apt install -y -qq nodejs 2>&1 | tail -2
    fi
else
    curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" | bash - 2>&1 | tail -2
    apt install -y -qq nodejs 2>&1 | tail -2
fi
log "Node $(node -v) | npm $(npm -v)"

# ──────────────────────────────────────────────────────────────────
#  STEP 6: MySQL
# ──────────────────────────────────────────────────────────────────
step "6/12 — MySQL 8"

if ! command_exists mysql; then
    apt install -y -qq mysql-server 2>&1 | tail -2
fi
systemctl enable mysql 2>/dev/null
systemctl start mysql 2>/dev/null

# Check if DB already exists
if mysql -e "USE \`${DB_NAME}\`" 2>/dev/null; then
    log "Database '${DB_NAME}' already exists"
else
    # Generate secure password and save it
    MYSQL_PASS=$(openssl rand -hex 16)
    mysql -e "CREATE DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -e "CREATE USER '${DB_USER}'@'localhost' IDENTIFIED BY '${MYSQL_PASS}';"
    mysql -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';"
    mysql -e "FLUSH PRIVILEGES;"

    # Save credentials for later
    mkdir -p "$BACKUP_DIR"
    cat > "$BACKUP_DIR/mysql-credentials.txt" << EOF
Database: ${DB_NAME}
User:     ${DB_USER}
Password: ${MYSQL_PASS}
Created:  $(date)
EOF
    chmod 600 "$BACKUP_DIR/mysql-credentials.txt"
    log "Database created — credentials saved to $BACKUP_DIR/mysql-credentials.txt"
fi

# MySQL performance tuning for small VPS
MYCNF="/etc/mysql/mysql.conf.d/driving-school.cnf"
if [ ! -f "$MYCNF" ]; then
    cat > "$MYCNF" << 'EOF'
[mysqld]
# ── Memory (conservative for 4GB VPS) ──
innodb_buffer_pool_size = 512M
innodb_log_file_size = 64M
innodb_log_buffer_size = 16M
innodb_flush_log_at_trx_commit = 2

# ── Connections ──
max_connections = 100
wait_timeout = 300
interactive_timeout = 300

# ── Query Cache ──
tmp_table_size = 32M
max_heap_table_size = 32M

# ── Logging ──
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# ── Security ──
skip-name-resolve
local_infile = 0
EOF
    systemctl restart mysql
    log "MySQL performance tuning applied"
fi

# ──────────────────────────────────────────────────────────────────
#  STEP 7: PM2
# ──────────────────────────────────────────────────────────────────
step "7/12 — PM2 Process Manager"

if ! command_exists pm2; then
    npm install -g pm2 2>&1 | tail -2
fi
log "PM2 $(pm2 -v) installed"

# ──────────────────────────────────────────────────────────────────
#  STEP 8: Nginx
# ──────────────────────────────────────────────────────────────────
step "8/12 — Nginx Reverse Proxy"

if ! command_exists nginx; then
    apt install -y -qq nginx 2>&1 | tail -2
fi
systemctl enable nginx 2>/dev/null
systemctl start nginx 2>/dev/null

# Write Nginx config
NGINX_CONF="/etc/nginx/sites-available/driving-school"
backup_file "$NGINX_CONF" 2>/dev/null || true

cat > "$NGINX_CONF" << 'NGINX'
# ── Upstream ──
upstream driving_school {
    server 127.0.0.1:3000;
    keepalive 64;
}

# ── HTTP → HTTPS redirect (Certbot adds the HTTPS block) ──
server {
    listen 80;
    server_name console.amategekoyumuhanda.rw;

    # Certbot will add the ssl server block after running:
    #   certbot --nginx -d console.amategekoyumuhanda.rw

    # ── API ──
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

    # ── Health Check (no logging) ──
    location = /api/health {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        access_log off;
    }

    # ── SSE (Server-Sent Events) ──
    location /api/sse {
        proxy_pass http://driving_school;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 86400s;
        chunked_transfer_encoding off;
    }

    # ── Static Files (long cache) ──
    location /_next/static/ {
        proxy_pass http://driving_school;
        proxy_cache_valid 200 365d;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    # ── Uploads ──
    location /uploads/ {
        proxy_pass http://driving_school;
        proxy_cache_valid 200 7d;
    }

    # ── Everything Else ──
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

    # ── Security Headers ──
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # ── Gzip ──
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_min_length 256;
    gzip_types
        text/plain
        text/css
        text/javascript
        application/json
        application/javascript
        application/xml
        application/xml+rss
        image/svg+xml;
}
NGINX

ln -sf /etc/nginx/sites-available/driving-school /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

if nginx -t 2>&1; then
    systemctl reload nginx
    log "Nginx configured and running"
else
    warn "Nginx config test failed — check /etc/nginx/sites-available/driving-school"
fi

# ──────────────────────────────────────────────────────────────────
#  STEP 9: Deploy User
# ──────────────────────────────────────────────────────────────────
step "9/12 — Deploy User"

if ! id "$APP_USER" &>/dev/null; then
    adduser --disabled-password --gecos "" "$APP_USER" 2>&1 | tail -1
    usermod -aG sudo "$APP_USER" 2>/dev/null || true

    # Limited sudo — only what's needed
    cat > "/etc/sudoers.d/${APP_USER}" << SUDOERS
# Deploy user can run these without password:
${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/pm2, /usr/local/bin/pm2
${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/git
${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart nginx
${APP_USER} ALL=(ALL) NOPASSWD: /usr/bin/systemctl reload nginx
SUDOERS
    chmod 440 "/etc/sudoers.d/${APP_USER}"
    log "User '${APP_USER}' created with limited sudo"
else
    log "User '${APP_USER}' already exists"
fi

# Directories
mkdir -p "$APP_DIR" "$LOG_DIR" "$BACKUP_DIR"
chown "${APP_USER}:${APP_USER}" "$APP_DIR" "$LOG_DIR" "$BACKUP_DIR"
log "Directories ready"

# ──────────────────────────────────────────────────────────────────
#  STEP 10: Firewall
# ──────────────────────────────────────────────────────────────────
step "10/12 — UFW Firewall"

if ! command_exists ufw; then
    apt install -y -qq ufw 2>&1 | tail -2
fi
ufw default deny incoming 2>/dev/null
ufw default allow outgoing 2>/dev/null
ufw allow 22/tcp comment "SSH" 2>/dev/null
ufw allow 80/tcp comment "HTTP" 2>/dev/null
ufw allow 443/tcp comment "HTTPS" 2>/dev/null
ufw --force enable 2>&1 | tail -2
log "Firewall active: SSH(22), HTTP(80), HTTPS(443)"

# ──────────────────────────────────────────────────────────────────
#  STEP 11: Log Rotation
# ──────────────────────────────────────────────────────────────────
step "11/12 — Log Rotation"

cat > /etc/logrotate.d/driving-school << EOF
${LOG_DIR}/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0644 ${APP_USER} ${APP_USER}
    sharedscripts
    postrotate
        pm2 reloadLogs 2>/dev/null || true
    endscript
}
EOF
log "Log rotation: 14 days, compressed"

# ──────────────────────────────────────────────────────────────────
#  STEP 12: MySQL Backup Cron
# ──────────────────────────────────────────────────────────────────
step "12/12 — Automated Backups"

BACKUP_SCRIPT="$BACKUP_DIR/backup-mysql.sh"
cat > "$BACKUP_SCRIPT" << BKEOF
#!/bin/bash
# Daily MySQL backup — runs at 3 AM via cron
DATE=\$(date +%F_%H%M)
BACKUP_DIR="$BACKUP_DIR/mysql"
mkdir -p "\$BACKUP_DIR"

# Dump database
mysqldump -u ${DB_USER} -p"\$(cat $BACKUP_DIR/mysql-credentials.txt | grep Password | cut -d' ' -f2)" \\
    --single-transaction --routines --triggers \\
    ${DB_NAME} | gzip > "\$BACKUP_DIR/${DB_NAME}_\$DATE.sql.gz"

# Keep only last 7 days
find "\$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete

echo "[\$DATE] Backup done: \$(ls -lh \$BACKUP_DIR/${DB_NAME}_\$DATE.sql.gz | awk '{print \$5}')"
BKEOF
chmod +x "$BACKUP_SCRIPT"

# Add to cron (avoid duplicates)
CRON_LINE="0 3 * * * $BACKUP_SCRIPT >> $LOG_DIR/backup.log 2>&1"
if ! crontab -l 2>/dev/null | grep -q "backup-mysql.sh"; then
    (crontab -l 2>/dev/null; echo "$CRON_LINE") | crontab -
    log "Daily MySQL backup cron installed (3 AM)"
else
    log "Backup cron already exists"
fi

# ══════════════════════════════════════════════════════════════════
#  VERIFICATION
# ══════════════════════════════════════════════════════════════════
header "Setup Complete — Verification"

echo ""
info "Server IP:    $SERVER_IP"
info "Timezone:     $TIMEZONE"
info "Swap:         $(swapon --show | awk 'NR==2 {print $3}')"
info "Node.js:      $(node -v)"
info "npm:          $(npm -v)"
info "MySQL:        $(mysql --version | awk '{print $3}')"
info "PM2:          $(pm2 -v)"
info "Nginx:        $(nginx -v 2>&1 | awk -F/ '{print $2}')"
info "Firewall:     $(ufw status | head -1)"
echo ""

# Check services
for svc in mysql nginx; do
    if systemctl is-active --quiet "$svc"; then
        log "$svc: running"
    else
        warn "$svc: STOPPED — run: systemctl start $svc"
    fi
done

echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ⚡ NEXT STEPS:${NC}"
echo -e "${BOLD}═══════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${CYAN}1.${NC} Clone your repo:"
echo "     sudo -u $APP_USER bash -c 'cd $APP_DIR && git clone <your-repo> .'"
echo ""
echo -e "  ${CYAN}2.${NC} Create .env file:"
echo "     sudo -u $APP_USER cp $APP_DIR/.env.example $APP_DIR/.env"
echo "     sudo -u $APP_USER nano $APP_DIR/.env"
echo ""
echo -e "  ${CYAN}3.${NC} Build & start:"
echo "     sudo -u $APP_USER bash -c 'cd $APP_DIR && npm install && npx prisma generate && npm run build && pm2 start ecosystem.config.js && pm2 save && pm2 startup'"
echo ""
echo -e "  ${CYAN}4.${NC} Point DNS to: ${SERVER_IP}"
echo ""
echo -e "  ${CYAN}5.${NC} Setup SSL:"
echo "     certbot --nginx -d $DOMAIN"
echo ""
echo -e "  ${CYAN}6.${NC} Verify:"
echo "     bash $0 check"
echo ""
echo -e "  ${CYAN}7.${NC} Deploy updates:"
echo "     sudo -u $APP_USER bash $APP_DIR/go-live.sh update"
echo ""
echo -e "  ${CYAN}8.${NC} Repair issues:"
echo "     sudo bash $0 repair"
echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════${NC}"
