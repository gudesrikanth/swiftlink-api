#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# test-local.sh — End-to-end endpoint tests against a locally running app.
#
# Usage:
#   ./scripts/test-local.sh            # starts DynamoDB Local + app automatically
#   ./scripts/test-local.sh --skip-start  # app is already running on :8080
# ---------------------------------------------------------------------------
set -euo pipefail

BASE_URL="http://localhost:8080"
DYNAMO_CONTAINER="swiftlink-dynamo-test"
APP_PID=""
SKIP_START=false
PASS=0
FAIL=0

# ── colours ─────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m';  BOLD='\033[1m';   RESET='\033[0m'

# ── helpers ──────────────────────────────────────────────────────────────────
log()  { echo -e "${CYAN}▶ $*${RESET}"; }
ok()   { echo -e "  ${GREEN}✓ $*${RESET}"; PASS=$((PASS + 1)); }
fail() { echo -e "  ${RED}✗ $*${RESET}"; FAIL=$((FAIL + 1)); }

assert_status() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then ok "$label → HTTP $actual"
  else fail "$label → expected HTTP $expected, got HTTP $actual"; fi
}

assert_contains() {
  local label="$1" needle="$2" haystack="$3"
  if echo "$haystack" | grep -q "$needle"; then ok "$label contains '$needle'"
  else fail "$label missing '$needle' in: $haystack"; fi
}

cleanup() {
  [ -n "$APP_PID" ] && kill "$APP_PID" 2>/dev/null && log "App stopped"
  docker rm -f "$DYNAMO_CONTAINER" 2>/dev/null && log "DynamoDB Local stopped" || true
}

# ── argument parsing ─────────────────────────────────────────────────────────
for arg in "$@"; do
  [[ "$arg" == "--skip-start" ]] && SKIP_START=true
done

# ── startup ──────────────────────────────────────────────────────────────────
if [ "$SKIP_START" = false ]; then
  trap cleanup EXIT

  log "Starting DynamoDB Local..."
  docker run -d --name "$DYNAMO_CONTAINER" -p 8000:8000 \
    amazon/dynamodb-local:2.5.2 \
    -jar DynamoDBLocal.jar -sharedDb -inMemory > /dev/null
  sleep 2

  log "Starting SwiftLink app (local profile)..."
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

  JAVA_HOME_25=""
  if [ -d "/opt/homebrew/Cellar/openjdk/25/libexec/openjdk.jdk/Contents/Home" ]; then
    JAVA_HOME_25="/opt/homebrew/Cellar/openjdk/25/libexec/openjdk.jdk/Contents/Home"
  elif [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q "25"; then
    JAVA_HOME_25="$JAVA_HOME"
  fi

  if [ -z "$JAVA_HOME_25" ]; then
    echo -e "${RED}Java 25 not found. Set JAVA_HOME to Java 25 installation.${RESET}"
    exit 1
  fi

  JAVA_HOME="$JAVA_HOME_25" \
  PATH="$JAVA_HOME_25/bin:$PATH" \
  AWS_REGION=us-east-1 \
  AWS_ACCESS_KEY_ID=dummy \
  AWS_SECRET_ACCESS_KEY=dummy \
    mvn -f "$PROJECT_ROOT/pom.xml" spring-boot:run \
      -Dspring-boot.run.profiles=local \
      -Dspring-boot.run.jvmArguments="-Dswiftlink.dynamo-db.endpoint=http://localhost:8000" \
      --no-transfer-progress \
      > /tmp/swiftlink-test.log 2>&1 &
  APP_PID=$!

  log "Waiting for app to be ready..."
  for i in $(seq 1 30); do
    if curl -sf "$BASE_URL/actuator/health" > /dev/null 2>&1; then
      log "App is UP"; break
    fi
    [ "$i" -eq 30 ] && { echo -e "${RED}App failed to start. Check /tmp/swiftlink-test.log${RESET}"; exit 1; }
    sleep 2
  done
fi

echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  SwiftLink Local Tests  —  $BASE_URL${RESET}"
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"

# ── 1. Health ─────────────────────────────────────────────────────────────────
echo ""
log "1. Health endpoints"
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
assert_status "GET /actuator/health" "200" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/liveness")
assert_status "GET /actuator/health/liveness" "200" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/readiness")
assert_status "GET /actuator/health/readiness" "200" "$STATUS"

# ── 2. Create short URL ───────────────────────────────────────────────────────
echo ""
log "2. Create short URL (auto-generated code)"
BODY=$(curl -s -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com/local-test","title":"Local Test","tags":["local","test"]}')
STATUS=$(curl -so /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com/local-test-2","title":"Local Test 2"}')
assert_status "POST /api/v1/urls" "201" "$STATUS"
SHORT_CODE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])")
assert_contains "Response has shortCode" "shortCode" "$BODY"
assert_contains "Response has shortUrl" "shortUrl" "$BODY"
log "  Generated shortCode: $SHORT_CODE"

# ── 3. Create with custom alias ───────────────────────────────────────────────
echo ""
log "3. Create URL with custom alias"
ALIAS="local-$(date +%s)"
BODY=$(curl -s -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://github.com/gudesrikanth/swiftlink-api\",\"customAlias\":\"$ALIAS\"}")
STATUS=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(200 if d.get('shortCode')==\"$ALIAS\" else 0)" 2>/dev/null || echo "0")
if [ "$STATUS" = "1" ] || echo "$BODY" | grep -q "\"shortCode\":\"$ALIAS\""; then
  ok "Custom alias '$ALIAS' created"
else
  # Re-check HTTP status
  HTTP=$(curl -so /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/urls" \
    -H "Content-Type: application/json" \
    -d "{\"longUrl\":\"https://example.com\",\"customAlias\":\"${ALIAS}-2\"}")
  assert_status "POST /api/v1/urls (custom alias)" "201" "$HTTP"
  ALIAS="${ALIAS}-2"
fi

# ── 4. Get URL info ───────────────────────────────────────────────────────────
echo ""
log "4. Get URL info"
BODY=$(curl -s "$BASE_URL/api/v1/urls/$SHORT_CODE")
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "GET /api/v1/urls/$SHORT_CODE" "200" "$STATUS"
assert_contains "Has longUrl" "longUrl" "$BODY"
assert_contains "Has active field" "active" "$BODY"
assert_contains "Has clickCount" "clickCount" "$BODY"

# ── 5. Redirect ───────────────────────────────────────────────────────────────
echo ""
log "5. Redirect"
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/$SHORT_CODE")
assert_status "GET /$SHORT_CODE" "302" "$STATUS"
LOCATION=$(curl -sI "$BASE_URL/$SHORT_CODE" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
assert_contains "Location header set" "example.com" "$LOCATION"

# ── 6. Analytics ──────────────────────────────────────────────────────────────
echo ""
log "6. Analytics (after 3 clicks)"
for _ in 1 2 3; do curl -so /dev/null "$BASE_URL/$SHORT_CODE"; done
sleep 1
BODY=$(curl -s "$BASE_URL/api/v1/urls/$SHORT_CODE/analytics")
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/api/v1/urls/$SHORT_CODE/analytics")
assert_status "GET /api/v1/urls/$SHORT_CODE/analytics" "200" "$STATUS"
assert_contains "Has totalClicks" "totalClicks" "$BODY"
assert_contains "Has clicksByDay" "clicksByDay" "$BODY"
assert_contains "Has recentClicks" "recentClicks" "$BODY"

# ── 7. Validation errors ──────────────────────────────────────────────────────
echo ""
log "7. Input validation"
STATUS=$(curl -so /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{"longUrl":"not-a-url"}')
assert_status "POST bad URL → 400" "400" "$STATUS"

BODY=$(curl -s -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{"longUrl":"not-a-url"}')
assert_contains "Error code VALIDATION_FAILED" "VALIDATION_FAILED" "$BODY"
assert_contains "Field error present" "fieldErrors" "$BODY"

STATUS=$(curl -so /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{}')
assert_status "POST empty body → 400" "400" "$STATUS"

# ── 8. Conflict ───────────────────────────────────────────────────────────────
echo ""
log "8. Duplicate alias conflict"
STATUS=$(curl -so /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://example.com\",\"customAlias\":\"$SHORT_CODE\"}")
assert_status "POST duplicate alias → 409" "409" "$STATUS"

# ── 9. Not found ──────────────────────────────────────────────────────────────
echo ""
log "9. Not found"
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/api/v1/urls/no-such-code")
assert_status "GET /api/v1/urls/no-such-code → 404" "404" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/no-such-code")
assert_status "GET /no-such-code → 404" "404" "$STATUS"

# ── 10. Expired URL ───────────────────────────────────────────────────────────
echo ""
log "10. Expired URL → 410 Gone"
EXPIRE_ALIAS="expired-$(date +%s)"
EXPIRES_AT="$(date -u -v+2S '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || date -u -d '+2 seconds' '+%Y-%m-%dT%H:%M:%SZ')"
curl -so /dev/null -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://example.com\",\"customAlias\":\"$EXPIRE_ALIAS\",\"expiresAt\":\"$EXPIRES_AT\"}"
sleep 3
STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/$EXPIRE_ALIAS")
assert_status "GET expired redirect → 410" "410" "$STATUS"

# ── 11. Delete ────────────────────────────────────────────────────────────────
echo ""
log "11. Delete"
STATUS=$(curl -so /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "DELETE /api/v1/urls/$SHORT_CODE → 204" "204" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "GET after delete → 404" "404" "$STATUS"

# ── 12. OpenAPI ───────────────────────────────────────────────────────────────
echo ""
log "12. OpenAPI / Swagger"
STATUS=$(curl -so /dev/null -w "%{http_code}" -L "$BASE_URL/swagger-ui.html")
assert_status "GET /swagger-ui.html" "200" "$STATUS"
BODY=$(curl -s "$BASE_URL/v3/api-docs")
assert_contains "OpenAPI title" "SwiftLink" "$BODY"

# ── summary ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"
TOTAL=$((PASS + FAIL))
if [ "$FAIL" -eq 0 ]; then
  echo -e "${GREEN}${BOLD}  ALL $TOTAL TESTS PASSED ✓${RESET}"
else
  echo -e "${RED}${BOLD}  $FAIL/$TOTAL TESTS FAILED ✗${RESET}"
fi
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"
echo ""

[ "$FAIL" -eq 0 ]
