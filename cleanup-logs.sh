#!/bin/bash
# ════════════════════════════════════════════════════════════════════
# cleanup-logs.sh — Clean old logs to prevent disk space issues
#
# Run via cron: 0 2 * * * /home/project3/cleanup-logs.sh
# ════════════════════════════════════════════════════════════════════

LOG_DIR="/home/logs"
BACKUP_DIR="/home/backups/mysql"
KEEP_DAYS=7
DATE=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$DATE] Starting log cleanup..."

# 1. Truncate PM2 logs (keep last 1000 lines)
if [ -f "$LOG_DIR/driving-school-out.log" ]; then
    tail -1000 "$LOG_DIR/driving-school-out.log" > "$LOG_DIR/driving-school-out.log.tmp"
    mv "$LOG_DIR/driving-school-out.log.tmp" "$LOG_DIR/driving-school-out.log"
    echo "[$DATE] Truncated driving-school-out.log"
fi

if [ -f "$LOG_DIR/driving-school-error.log" ]; then
    tail -1000 "$LOG_DIR/driving-school-error.log" > "$LOG_DIR/driving-school-error.log.tmp"
    mv "$LOG_DIR/driving-school-error.log.tmp" "$LOG_DIR/driving-school-error.log"
    echo "[$DATE] Truncated driving-school-error.log"
fi

# 2. Delete old PM2 log files
find "$LOG_DIR" -name "*.log.old" -mtime +$KEEP_DAYS -delete 2>/dev/null
find "$LOG_DIR" -name "*.log.*.gz" -mtime +$KEEP_DAYS -delete 2>/dev/null

# 3. Delete old backup files (keep last 7 days)
if [ -d "$BACKUP_DIR" ]; then
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$KEEP_DAYS -delete 2>/dev/null
    echo "[$DATE] Cleaned old MySQL backups"
fi

# 4. Clean npm cache
npm cache clean --force 2>/dev/null

# 5. Clean apt cache
sudo apt-get clean 2>/dev/null

# 6. Delete old system logs
sudo journalctl --vacuum-time=7d 2>/dev/null

# 7. Report disk usage
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}')
echo "[$DATE] Log cleanup complete. Disk usage: $DISK_USAGE"
