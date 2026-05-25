#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# test-dev.sh — End-to-end endpoint tests against the deployed dev environment
#               (AWS Lambda + API Gateway).
#
# Prerequisites:
#   - AWS CLI configured (aws configure) or env vars set
#   - Terraform applied in infra/environments/dev  (provides the API URL)
#
# Usage:
#   ./scripts/test-dev.sh                         # auto-resolves URL from AWS
#   ./scripts/test-dev.sh --url https://xxx.execute-api.us-east-1.amazonaws.com
# ---------------------------------------------------------------------------
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
LAMBDA_FUNCTION="${LAMBDA_FUNCTION:-swiftlink-dev}"
BASE_URL=""
PASS=0
FAIL=0

# ── colours ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m';  BOLD='\033[1m';   RESET='\033[0m'

# ── helpers ───────────────────────────────────────────────────────────────────
log()  { echo -e "${CYAN}▶ $*${RESET}"; }
ok()   { echo -e "  ${GREEN}✓ $*${RESET}"; PASS=$((PASS + 1)); }
fail() { echo -e "  ${RED}✗ $*${RESET}"; FAIL=$((FAIL + 1)); }
warn() { echo -e "  ${YELLOW}⚠ $*${RESET}"; }

assert_status() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then ok "$label → HTTP $actual"
  else fail "$label → expected HTTP $expected, got HTTP $actual"; fi
}

assert_contains() {
  local label="$1" needle="$2" haystack="$3"
  if echo "$haystack" | grep -q "$needle"; then ok "$label contains '$needle'"
  else fail "$label missing '$needle'"; fi
}

# ── argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case $1 in
    --url) BASE_URL="$2"; shift 2 ;;
    --region) AWS_REGION="$2"; shift 2 ;;
    --function) LAMBDA_FUNCTION="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# ── resolve API URL ───────────────────────────────────────────────────────────
if [ -z "$BASE_URL" ]; then
  log "Resolving API Gateway URL from AWS..."
  if ! command -v aws &> /dev/null; then
    echo -e "${RED}AWS CLI not found. Install it or pass --url <api-url>${RESET}"
    exit 1
  fi

  BASE_URL=$(aws apigatewayv2 get-apis \
    --region "$AWS_REGION" \
    --query "Items[?Name=='$LAMBDA_FUNCTION'].ApiEndpoint | [0]" \
    --output text 2>/dev/null || echo "")

  if [ -z "$BASE_URL" ] || [ "$BASE_URL" = "None" ]; then
    echo -e "${RED}Could not resolve API URL for Lambda '$LAMBDA_FUNCTION' in $AWS_REGION.${RESET}"
    echo -e "${YELLOW}Run: terraform apply in infra/environments/dev first.${RESET}"
    echo -e "${YELLOW}Or pass --url https://<id>.execute-api.$AWS_REGION.amazonaws.com${RESET}"
    exit 1
  fi
  # Strip trailing slash
  BASE_URL="${BASE_URL%/}"
  log "Resolved: $BASE_URL"
fi

# ── warm up Lambda (cold start) ───────────────────────────────────────────────
log "Warming up Lambda (may take a few seconds on cold start)..."
for i in $(seq 1 5); do
  STATUS=$(curl -so /dev/null -w "%{http_code}" \
    --max-time 30 "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" = "200" ]; then
    log "Lambda is warm (attempt $i)"
    break
  fi
  [ "$i" -eq 5 ] && { echo -e "${RED}Lambda did not respond after 5 attempts (status=$STATUS)${RESET}"; exit 1; }
  warn "Attempt $i: status=$STATUS — retrying in 5s..."
  sleep 5
done

echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  SwiftLink Dev Tests  —  $BASE_URL${RESET}"
echo -e "${BOLD}═══════════════════════════════════════════════════════${RESET}"

# ── 1. Health ─────────────────────────────────────────────────────────────────
echo ""
log "1. Health endpoints"
STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/actuator/health")
assert_status "GET /actuator/health" "200" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/actuator/health/liveness")
assert_status "GET /actuator/health/liveness" "200" "$STATUS"

STATUS=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/actuator/health/readiness")
assert_status "GET /actuator/health/readiness" "200" "$STATUS"

# ── 2. Create short URL ───────────────────────────────────────────────────────
echo ""
log "2. Create short URL"
BODY=$(curl -s --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com/dev-smoke-test","title":"Dev Smoke Test","tags":["dev","smoke"]}')
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com/dev-smoke-test-2","title":"Dev Smoke Test 2"}')
assert_status "POST /api/v1/urls" "201" "$HTTP"
assert_contains "Response has shortCode" "shortCode" "$BODY"
SHORT_CODE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])" 2>/dev/null || \
  echo "$BODY" | grep -o '"shortCode":"[^"]*"' | cut -d'"' -f4)
log "  Generated shortCode: $SHORT_CODE"

# ── 3. Create with custom alias ───────────────────────────────────────────────
echo ""
log "3. Create URL with custom alias"
ALIAS="dev-smoke-$(date +%s)"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://github.com/gudesrikanth/swiftlink-api\",\"customAlias\":\"$ALIAS\"}")
assert_status "POST custom alias" "201" "$HTTP"

# ── 4. Get URL info ───────────────────────────────────────────────────────────
echo ""
log "4. Get URL info"
BODY=$(curl -s --max-time 15 "$BASE_URL/api/v1/urls/$SHORT_CODE")
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "GET /api/v1/urls/$SHORT_CODE" "200" "$HTTP"
assert_contains "Has longUrl" "longUrl" "$BODY"
assert_contains "Has active" "active" "$BODY"
assert_contains "Has clickCount" "clickCount" "$BODY"

# ── 5. Redirect ───────────────────────────────────────────────────────────────
echo ""
log "5. Redirect"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/$SHORT_CODE")
assert_status "GET /$SHORT_CODE → 302" "302" "$HTTP"
LOCATION=$(curl -sI --max-time 15 "$BASE_URL/$SHORT_CODE" | grep -i "^location:" | tr -d '\r' | awk '{print $2}')
assert_contains "Location header set" "example.com" "$LOCATION"

# ── 6. Analytics ──────────────────────────────────────────────────────────────
echo ""
log "6. Analytics"
for _ in 1 2 3; do curl -so /dev/null --max-time 15 "$BASE_URL/$SHORT_CODE" || true; done
sleep 2
BODY=$(curl -s --max-time 15 "$BASE_URL/api/v1/urls/$SHORT_CODE/analytics")
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/api/v1/urls/$SHORT_CODE/analytics")
assert_status "GET analytics" "200" "$HTTP"
assert_contains "Has totalClicks" "totalClicks" "$BODY"
assert_contains "Has recentClicks" "recentClicks" "$BODY"

# ── 7. Validation errors ──────────────────────────────────────────────────────
echo ""
log "7. Input validation"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{"longUrl":"not-a-url"}')
assert_status "POST bad URL → 400" "400" "$HTTP"

BODY=$(curl -s --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{"longUrl":"not-a-url"}')
assert_contains "VALIDATION_FAILED error" "VALIDATION_FAILED" "$BODY"

HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" -d '{}')
assert_status "POST empty body → 400" "400" "$HTTP"

# ── 8. Conflict ───────────────────────────────────────────────────────────────
echo ""
log "8. Duplicate alias → 409"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://example.com\",\"customAlias\":\"$ALIAS\"}")
assert_status "POST duplicate alias → 409" "409" "$HTTP"

# ── 9. Not found ──────────────────────────────────────────────────────────────
echo ""
log "9. Not found"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/api/v1/urls/no-such-code-xyz")
assert_status "GET missing URL info → 404" "404" "$HTTP"

HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/no-such-code-xyz")
assert_status "GET missing redirect → 404" "404" "$HTTP"

# ── 10. Expired URL ───────────────────────────────────────────────────────────
echo ""
log "10. Expired URL → 410 Gone"
EXPIRE_ALIAS="dev-exp-$(date +%s)"
EXPIRES_AT="$(date -u -v+2S '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || date -u -d '+2 seconds' '+%Y-%m-%dT%H:%M:%SZ')"
curl -so /dev/null --max-time 15 -X POST "$BASE_URL/api/v1/urls" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://example.com\",\"customAlias\":\"$EXPIRE_ALIAS\",\"expiresAt\":\"$EXPIRES_AT\"}" || true
sleep 4
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/$EXPIRE_ALIAS")
assert_status "GET expired → 410" "410" "$HTTP"

# ── 11. Delete ────────────────────────────────────────────────────────────────
echo ""
log "11. Delete"
HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 -X DELETE "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "DELETE → 204" "204" "$HTTP"

HTTP=$(curl -so /dev/null -w "%{http_code}" --max-time 15 "$BASE_URL/api/v1/urls/$SHORT_CODE")
assert_status "GET after delete → 404" "404" "$HTTP"

# ── 12. OpenAPI ───────────────────────────────────────────────────────────────
echo ""
log "12. OpenAPI spec"
BODY=$(curl -s --max-time 15 "$BASE_URL/v3/api-docs")
assert_contains "OpenAPI title" "SwiftLink" "$BODY"

# ── Lambda metrics (informational) ────────────────────────────────────────────
echo ""
log "Lambda metrics (last 5 min)"
if command -v aws &> /dev/null; then
  END=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
  START=$(date -u -v-5M '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null || date -u -d '-5 minutes' '+%Y-%m-%dT%H:%M:%SZ')
  INVOCATIONS=$(aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Invocations \
    --dimensions Name=FunctionName,Value="$LAMBDA_FUNCTION" \
    --start-time "$START" --end-time "$END" \
    --period 300 --statistics Sum \
    --region "$AWS_REGION" \
    --query "Datapoints[0].Sum" --output text 2>/dev/null || echo "n/a")
  ERRORS=$(aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name Errors \
    --dimensions Name=FunctionName,Value="$LAMBDA_FUNCTION" \
    --start-time "$START" --end-time "$END" \
    --period 300 --statistics Sum \
    --region "$AWS_REGION" \
    --query "Datapoints[0].Sum" --output text 2>/dev/null || echo "n/a")
  echo -e "  Invocations: ${CYAN}$INVOCATIONS${RESET}  |  Errors: ${CYAN}$ERRORS${RESET}"
else
  warn "AWS CLI not found — skipping Lambda metrics"
fi

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
