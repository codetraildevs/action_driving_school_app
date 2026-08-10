#!/usr/bin/env bash
# Builds backend-deploy.tar.gz from DRIVING_SHOOL_COMPANY_LEGACY for upload to
# cPanel File Manager. Excludes everything that must stay server-side.
set -euo pipefail

cd "$(dirname "$0")/.."
cd DRIVING_SHOOL_COMPANY_LEGACY

OUT="../backend-deploy.tar.gz"
rm -f "$OUT"

tar -czf "$OUT" \
  --exclude='./node_modules' \
  --exclude='./.next' \
  --exclude='./.env' \
  --exclude='./uploads' \
  --exclude='./.git' \
  --exclude='*.log' \
  --exclude='./tsconfig.tsbuildinfo' \
  --exclude='./lib/generated' \
  --exclude='./public/uploads' \
  --exclude='./public/*.zip' \
  .

echo "Created $(cd .. && pwd)/backend-deploy.tar.gz"
ls -la "$OUT"

echo "=== roles.ts included? ==="
tar -tzf "$OUT" | grep 'lib/auth/roles.ts' && echo "OK: roles.ts present"

echo "=== forbidden items present? (should print nothing) ==="
tar -tzf "$OUT" | grep -E 'node_modules|\.next/|\.env$|/uploads/' | head -5 || echo "OK: none excluded"

echo "=== top-level entries ==="
tar -tzf "$OUT" | awk -F/ 'NF>=2{print $1}' | sort -u | head -20
