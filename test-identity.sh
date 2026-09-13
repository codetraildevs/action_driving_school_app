#!/bin/bash
# ──────────────────────────────────────────────────────────────────
# test-identity.sh — Verify login identity == token identity == profile identity
#
# Usage:
#   ./test-identity.sh <base-url> <phone> <password-or-device-id> [clientType]
#
# Examples:
#   # Admin account (phone-only app login, password ignored):
#   ./test-identity.sh https://console.amategekoyumuhanda.rw 0732657995 x android_app
#
#   # Regular user (password == the device ANDROID_ID shown on the app login screen):
#   ./test-identity.sh https://console.amategekoyumuhanda.rw 0732657993 <ANDROID_ID> android_app
#
# It prints the four IDs that MUST match for the same account:
#   1. phone number entered
#   2. user id returned by login
#   3. user id inside the access token (decoded, not verified)
#   4. user id returned by GET /api/users/profile
# ──────────────────────────────────────────────────────────────────
set -euo pipefail

BASE_URL="${1:?Usage: test-identity.sh <base-url> <phone> <password> [clientType]}"
PHONE="${2:?missing phone}"
PASSWORD="${3:?missing password/device-id}"
CLIENT_TYPE="${4:-android_app}"

bold() { printf '\033[1m%s\033[0m\n' "$1"; }

bold "═══ 1. LOGIN (phone entered: $PHONE) ═══"
LOGIN_JSON=$(curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"identifier\":\"$PHONE\",\"password\":\"$PASSWORD\",\"clientType\":\"$CLIENT_TYPE\"}")

if ! echo "$LOGIN_JSON" | grep -q '"success":true'; then
  echo "❌ Login failed. Response:"
  echo "$LOGIN_JSON"
  exit 1
fi

LOGIN_USER_ID=$(echo "$LOGIN_JSON" | sed -n 's/.*"user":{"id":\([0-9]*\).*/\1/p')
LOGIN_PHONE=$(echo "$LOGIN_JSON" | sed -n 's/.*"phoneNumber":"\([^"]*\)".*/\1/p')
ACCESS_TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
REFRESH_TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')

echo "  login returned: userId=$LOGIN_USER_ID phone=$LOGIN_PHONE"

bold "═══ 2. TOKEN (decoded payload, signature not verified here) ═══"
TOKEN_PAYLOAD_B64=$(echo "$ACCESS_TOKEN" | cut -d. -f2 | tr '_-' '/+')
# Pad to a multiple of 4
case $(( ${#TOKEN_PAYLOAD_B64} % 4 )) in
  2) TOKEN_PAYLOAD_B64="${TOKEN_PAYLOAD_B64}==" ;;
  3) TOKEN_PAYLOAD_B64="${TOKEN_PAYLOAD_B64}=" ;;
esac
TOKEN_PAYLOAD=$(echo "$TOKEN_PAYLOAD_B64" | base64 -d 2>/dev/null || echo "$TOKEN_PAYLOAD_B64" | base64 --decode 2>/dev/null)
TOKEN_USER_ID=$(echo "$TOKEN_PAYLOAD" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')
echo "  token claims: $TOKEN_PAYLOAD"
echo "  token.userId=$TOKEN_USER_ID"

bold "═══ 3. PROFILE (GET /api/users/profile with the token) ═══"
PROFILE_JSON=$(curl -sS "$BASE_URL/api/users/profile" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if ! echo "$PROFILE_JSON" | grep -q '"success":true'; then
  echo "❌ Profile request failed. Response:"
  echo "$PROFILE_JSON"
  exit 1
fi

PROFILE_USER_ID=$(echo "$PROFILE_JSON" | sed -n 's/.*"data":{"id":\([0-9]*\).*/\1/p')
PROFILE_PHONE=$(echo "$PROFILE_JSON" | sed -n 's/.*"phoneNumber":"\([^"]*\)".*/\1/p')
echo "  profile returned: userId=$PROFILE_USER_ID phone=$PROFILE_PHONE"

bold "═══ 4. VERDICT ═══"
FAIL=0
if [ "$LOGIN_PHONE" != "$PHONE" ]; then
  echo "  ⚠️  note: login normalized the phone ($PHONE → $LOGIN_PHONE) — OK if digits match"
fi
if [ "$LOGIN_USER_ID" = "$TOKEN_USER_ID" ] && [ "$TOKEN_USER_ID" = "$PROFILE_USER_ID" ]; then
  echo "  ✅ ALL IDS MATCH: login.userId == token.userId == profile.userId == $LOGIN_USER_ID"
else
  echo "  ❌ ID MISMATCH: login=$LOGIN_USER_ID token=$TOKEN_USER_ID profile=$PROFILE_USER_ID"
  echo "     → cross-user leak! Check [AUTH-DEBUG] lines in pm2 logs."
  FAIL=1
fi
if [ "$PROFILE_PHONE" != "$LOGIN_PHONE" ]; then
  echo "  ❌ PHONE MISMATCH: login=$LOGIN_PHONE profile=$PROFILE_PHONE"
  FAIL=1
fi

bold "═══ 5. REFRESH keeps the same identity ═══"
REFRESH_JSON=$(curl -sS -X POST "$BASE_URL/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
NEW_ACCESS=$(echo "$REFRESH_JSON" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' || true)
if [ -n "$NEW_ACCESS" ]; then
  NEW_B64=$(echo "$NEW_ACCESS" | cut -d. -f2 | tr '_-' '/+')
  case $(( ${#NEW_B64} % 4 )) in
    2) NEW_B64="${NEW_B64}==" ;;
    3) NEW_B64="${NEW_B64}=" ;;
  esac
  NEW_PAYLOAD=$(echo "$NEW_B64" | base64 -d 2>/dev/null || echo "$NEW_B64" | base64 --decode 2>/dev/null)
  NEW_UID=$(echo "$NEW_PAYLOAD" | sed -n 's/.*"userId":\([0-9]*\).*/\1/p')
  if [ "$NEW_UID" = "$LOGIN_USER_ID" ]; then
    echo "  ✅ refreshed token keeps userId=$NEW_UID"
  else
    echo "  ❌ refreshed token switched user: $LOGIN_USER_ID → $NEW_UID"
    FAIL=1
  fi
else
  echo "  ⚠️  refresh failed: $REFRESH_JSON"
fi

bold "═══ 6. No-store header present ═══"
CACHE_HDR=$(curl -sSI -X GET "$BASE_URL/api/users/profile" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | grep -i "cache-control" || true)
echo "  $CACHE_HDR"
echo "$CACHE_HDR" | grep -qi "no-store" && echo "  ✅ profile is not cacheable" \
  || { echo "  ⚠️  missing no-store on profile response"; FAIL=1; }

echo ""
if [ "$FAIL" -eq 0 ]; then
  bold "✅ IDENTITY CHAIN OK for $PHONE"
else
  bold "❌ IDENTITY PROBLEMS FOUND — see above"
  exit 1
fi
