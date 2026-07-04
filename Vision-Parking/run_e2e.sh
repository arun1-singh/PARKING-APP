#!/bin/bash
# run_e2e.sh — Start Appium and run E2E tests.
set -e

export APPIUM_LOG_FILE="${APPIUM_LOG_FILE:-/tmp/appium.log}"
export TEST_REPORT_FILE="tests/report.html"
export APP_PACKAGE="${APP_PACKAGE:-com.example.visionpark}"

cd "$(dirname "$0")"
echo ">>> Working directory: $PWD"

# ── Install / verify Appium ──────────────────────────────────
if ! command -v appium > /dev/null 2>&1; then
  echo ">>> Installing Appium globally..."
  npm install -g appium@2.11.3
fi

echo ">>> Installing uiautomator2 driver..."
appium driver install uiautomator2 || appium driver update uiautomator2 || true

# ── Start Appium ─────────────────────────────────────────────
echo ">>> Starting Appium server..."
appium server \
  --base-path /wd/hub \
  --log "$APPIUM_LOG_FILE" \
  --log-level info \
  --port 4723 \
  --relaxed-security \
  --log-no-colors \
  &
APPIUM_PID=$!
echo ">>> Appium PID: $APPIUM_PID"

# ── Wait for Appium via HTTP (works even without nc/curl flags) ──
echo ">>> Waiting for Appium to be ready..."
APPIUM_READY=0
for i in $(seq 1 90); do
  STATUS=$(curl -sf --max-time 2 \
    "http://127.0.0.1:4723/wd/hub/status" 2>/dev/null | python3 -c \
    "import json,sys; d=json.load(sys.stdin); print(d.get('value',{}).get('ready',False))" \
    2>/dev/null || echo "false")
  if [ "$STATUS" = "True" ] || [ "$STATUS" = "true" ]; then
    echo ">>> Appium ready after ${i}s"
    APPIUM_READY=1
    break
  fi
  sleep 1
done

if [ $APPIUM_READY -ne 1 ]; then
  echo ">>> Appium failed to start. Last 40 lines of log:"
  tail -40 "$APPIUM_LOG_FILE" 2>/dev/null || echo "(no log)"
  exit 1
fi

# ── Python environment ───────────────────────────────────────
if [ -n "$VIRTUAL_ENV" ]; then
  echo ">>> Using venv: $VIRTUAL_ENV"
elif [ -f "$HOME/parking-app-yolo/venv/bin/activate" ]; then
  source "$HOME/parking-app-yolo/venv/bin/activate"
fi

# ── Run tests ────────────────────────────────────────────────
echo ">>> Running E2E tests..."
python3 -m pytest tests \
  -v \
  --tb=short \
  --disable-warnings \
  --html="$TEST_REPORT_FILE" \
  --self-contained-html \
  2>&1
PYTEST_EXIT=$?

echo ">>> Stopping Appium..."
kill "$APPIUM_PID" 2>/dev/null || true
wait "$APPIUM_PID" 2>/dev/null || true

exit $PYTEST_EXIT
