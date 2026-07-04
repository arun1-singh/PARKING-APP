#!/bin/bash
# run_e2e.sh — Start Appium and run pytest E2E tests.
# ci_emulator_setup.sh handles device readiness and APK install before this runs.
set -e

export APPIUM_LOG_FILE="${APPIUM_LOG_FILE:-/tmp/appium.log}"
export TEST_REPORT_FILE="tests/report.html"
export APP_PACKAGE="${APP_PACKAGE:-com.example.visionpark}"

# Run from Vision-Parking dir
cd "$(dirname "$0")"
echo ">>> Working directory: $PWD"

# ── Appium ──────────────────────────────────────────────────
if ! command -v appium > /dev/null 2>&1; then
  echo ">>> Installing Appium..."
  npm install -g appium
fi

echo ">>> Installing uiautomator2 driver..."
appium driver install uiautomator2 2>/dev/null || true

echo ">>> Starting Appium server..."
nohup appium --base-path /wd/hub \
  --log "$APPIUM_LOG_FILE" \
  --log-level warn \
  --allow-insecure chromedriver_autodownload \
  > /dev/null 2>&1 &
APPIUM_PID=$!

echo ">>> Waiting for Appium (max 60s)..."
for i in $(seq 1 60); do
  if nc -z 127.0.0.1 4723 2>/dev/null; then
    echo ">>> Appium ready after ${i}s"
    break
  fi
  sleep 1
done

if ! nc -z 127.0.0.1 4723 2>/dev/null; then
  echo ">>> Appium failed to start"
  cat "$APPIUM_LOG_FILE" 2>/dev/null | tail -30
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
  -x \
  2>&1
PYTEST_EXIT=$?

echo ">>> Stopping Appium..."
kill "$APPIUM_PID" 2>/dev/null || true
wait "$APPIUM_PID" 2>/dev/null || true

exit $PYTEST_EXIT
