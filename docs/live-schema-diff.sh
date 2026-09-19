#!/usr/bin/env bash
# ============================================================================
# LIVE SCHEMA DIFF — hosted DB vs current role-based schema
# ============================================================================
# Connects to the hosted MySQL (from DRIVING_SHOOL_COMPANY_LEGACY/.env) and
# reports, table by table, exactly what is missing or out of date compared to
# the current Prisma schema (mirrored by local-db/driving_school_local.sql).
#
# Requires working hosted credentials in DRIVING_SHOOL_COMPANY_LEGACY/.env
# (or pass DATABASE_URL=... inline to override).
#
# Usage:
#   bash docs/live-schema-diff.sh                # uses .env credentials
#   DATABASE_URL="mysql://u:p@host:3306/db" bash docs/live-schema-diff.sh
# ============================================================================

set -uo pipefail

cd "$(dirname "$0")/.."

MYSQL=/c/xampp/mysql/bin/mysql.exe
[ -x "$MYSQL" ] || MYSQL=$(command -v mysql)

if [ -n "${DATABASE_URL:-}" ]; then
  DBURL="$DATABASE_URL"
else
  DBURL=$(grep '^DATABASE_URL=' DRIVING_SHOOL_COMPANY_LEGACY/.env | head -1 | cut -d= -f2-)
fi

DBUSER=$(echo "$DBURL"  | sed -E 's|mysql://([^:]+):([^@]*)@.*|\1|')
DBPASS=$(echo "$DBURL"  | sed -E 's|mysql://([^:]+):([^@]*)@.*|\2|')
DBHOST=$(echo "$DBURL"  | sed -E 's|mysql://[^@]*@([^:/]+).*|\1|')
DBPORT=$(echo "$DBURL"  | sed -E 's|mysql://[^@]*@[^:/]+:([0-9]+).*|\1|' )
DBPORT=${DBPORT:-3306}
DBNAME=$(echo "$DBURL" | sed -E 's|.*/([^/?]+).*|\1|')

echo "== Connecting to $DBHOST:$DBPORT/$DBNAME as $DBUSER =="

# --- 1. Reachability + auth check ---
if ! "$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -e 'SELECT 1' "$DBNAME" >/dev/null 2>&1; then
  echo "❌ AUTH/CONNECTION FAILED — check credentials. Error:"
  "$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -e 'SELECT 1' "$DBNAME" 2>&1 | head -3
  exit 1
fi
echo "✅ Connected."

# --- 2. Compare table lists ---
echo
echo "== 1. TABLES present locally but MISSING on hosted =="
grep -oP 'CREATE TABLE IF NOT EXISTS `\w+`' docs/production-schema-sync.sql \
  | sed 's/CREATE TABLE IF NOT EXISTS `//; s/`//' \
  | while read -r t; do
      n=$("$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DBNAME' AND table_name='$t';" "$DBNAME" 2>/dev/null)
      [ "$n" = "0" ] && echo "  - $t"
    done

echo
echo "== 2. TABLES on hosted but NOT in current schema (extra) =="
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='$DBNAME' ORDER BY table_name;" "$DBNAME" 2>/dev/null \
  | while read -r t; do
      grep -q "\`$t\`" docs/production-schema-sync.sql || echo "  - $t"
    done

echo
echo "== 3. COLUMN diffs on key tables (hosted vs current schema) =="
# For each table in the sync script, compare hosted columns against the DDL.
for t in users user_roles user_subscriptions_request user_test_access \
         irembo_driving_license_requests irembo_special_requests \
         whatsappgroup firebase_devices app_store_compliance languages timezones; do
  # normalize casing for lookup (Windows dump vs Linux hosted)
  tbl=$(grep -oP 'CREATE TABLE IF NOT EXISTS `\w+`' docs/production-schema-sync.sql \
        | sed 's/CREATE TABLE IF NOT EXISTS `//; s/`//' \
        | awk -v want="$t" 'tolower($0)==tolower(want){print; exit}')
  [ -z "$tbl" ] && continue

  # expected columns from the DDL block
  expected=$(awk "/CREATE TABLE IF NOT EXISTS \`$tbl\` \(/,/^\) ENGINE=/" docs/production-schema-sync.sql \
             | grep -oP '^\s+`\w+`' | sed 's/`//g; s/^\s*//' | sort)

  actual=$("$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
    "SELECT column_name FROM information_schema.columns WHERE table_schema='$DBNAME' AND table_name='$tbl' ORDER BY column_name;" "$DBNAME" 2>/dev/null)

  missing=$(comm -23 <(echo "$expected") <(echo "$actual"))
  extra=$(comm -13 <(echo "$expected") <(echo "$actual"))

  if [ -n "$missing" ] || [ -n "$extra" ]; then
    echo "  ── $tbl ──"
    [ -n "$missing" ] && echo "    MISSING columns: $(echo "$missing" | tr '\n' ' ')"
    [ -n "$extra" ]   && echo "    EXTRA on hosted: $(echo "$extra" | tr '\n' ' ')"
  else
    echo "  $tbl: columns match ✅"
  fi
done

echo
echo "== 4. Reference data check =="
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT CONCAT('user_roles: ', COUNT(*)) FROM $DBNAME.user_roles;" "$DBNAME" 2>/dev/null || echo "user_roles table missing"
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT CONCAT('languages: ', COUNT(*)) FROM $DBNAME.languages;" "$DBNAME" 2>/dev/null || echo "languages table missing"
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT CONCAT('timezones: ', COUNT(*)) FROM $DBNAME.timezones;" "$DBNAME" 2>/dev/null || echo "timezones table missing"
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT CONCAT('users: ', COUNT(*)) FROM $DBNAME.users;" "$DBNAME" 2>/dev/null || echo "users table missing"
"$MYSQL" -h "$DBHOST" -P "$DBPORT" -u "$DBUSER" -p"$DBPASS" -N -e \
  "SELECT CONCAT('users with role>0: ', COUNT(*)) FROM $DBNAME.users WHERE role IS NOT NULL AND role > 0;" "$DBNAME" 2>/dev/null || echo "users.role column missing"

echo
echo "== DONE — see docs/production-schema-sync.sql for the safe sync script =="
