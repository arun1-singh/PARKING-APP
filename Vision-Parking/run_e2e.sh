#!/bin/bash
set -e

# === Global config ===
export APPIUM_LOG_FILE="${APPIUM_LOG_FILE:-/tmp/appium.log}"
export TEST_REPORT_FILE="tests/report.html"

cleanup_logcat() {
  echo "Capturing final logcat output..."
  adb logcat -d > /tmp/logcat.txt 2>&1 || touch /tmp/logcat.txt
}
trap cleanup_logcat EXIT

# Ensure we're in Vision-Parking directory
cd "$(dirname "$0")"
echo "Current working directory: $PWD"

# ============================================================
# STEP 1: Wait for device to be fully ready
# ============================================================
echo "Waiting for Android device..."
adb wait-for-device

echo "Polling for emulator boot completion..."
BOOT_STATUS=""
for i in $(seq 1 60); do
  BOOT_STATUS=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')
  if [[ "$BOOT_STATUS" == "1" ]]; then
    echo "✅ Emulator boot completed"
    break
  fi
  echo "⏳ Waiting for emulator to boot ($i/60)..."
  sleep 5
done
if [[ "$BOOT_STATUS" != "1" ]]; then
  echo "❌ Emulator did not boot in time."
  exit 1
fi

# ============================================================
# STEP 2: Wait for package manager to be truly stable
# Use pm list packages — the most reliable signal that the
# package manager daemon is fully accepting connections.
# ============================================================
echo "Waiting for package manager to be stable..."
PM_READY=0
for i in $(seq 1 60); do
  if adb shell pm list packages > /dev/null 2>&1; then
    echo "✅ Package manager is stable (attempt $i)"
    PM_READY=1
    break
  fi
  echo "⏳ Package manager not ready yet ($i/60)..."
  sleep 3
done
if [ $PM_READY -ne 1 ]; then
  echo "❌ Package manager never became stable"
  exit 1
fi

# Extra stabilization wait — gives the system time to finish
# background init tasks so the install doesn't race with them
echo "Waiting 15s for system to fully stabilize..."
sleep 15

# Dismiss any lingering system dialogs
echo "Dismissing any system dialogs..."
adb shell input keyevent KEYCODE_BACK  2>/dev/null || true
sleep 1
adb shell input keyevent KEYCODE_ENTER 2>/dev/null || true
sleep 1

# ============================================================
# STEP 3: Install the APK
# The workflow already installs the APK before calling this
# script. We skip re-installation if the app is already
# present, otherwise we install it (local dev / re-run case).
# ============================================================
APP_PACKAGE="${APP_PACKAGE:-com.example.visionpark}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if adb shell pm list packages 2>/dev/null | grep -q "$APP_PACKAGE"; then
  echo "✅ App $APP_PACKAGE already installed — skipping APK install"
else
  echo "App not found — installing APK..."
  INSTALL_SUCCESS=0
  for i in $(seq 1 5); do
    echo "Install attempt $i/5..."
    # Verify package manager is still up before each attempt
    if ! adb shell pm list packages > /dev/null 2>&1; then
      echo "⏳ Package manager not responding, waiting 30s..."
      sleep 30
      continue
    fi

    if adb install -r "$APK_PATH" 2>&1; then
      INSTALL_SUCCESS=1
      echo "✅ APK installed successfully on attempt $i"
      break
    else
      echo "❌ APK install failed on attempt $i, waiting 30s before retry..."
      sleep 30
    fi
  done
  if [ $INSTALL_SUCCESS -ne 1 ]; then
    echo "❌ APK install failed after 5 attempts"
    # Print diagnostic info
    echo "--- ADB devices ---"
    adb devices
    echo "--- Package manager status ---"
    adb shell service check package || true
    exit 1
  fi
fi

# ============================================================
# STEP 4: Install and start Appium
# ============================================================
if ! command -v appium &> /dev/null; then
  echo "Installing Appium globally..."
  npm install -g appium
fi

echo "Installing/verifying uiautomator2 driver..."
appium driver install uiautomator2 2>/dev/null || echo "Driver already installed, continuing..."

echo "Starting Appium server..."
nohup appium --base-path /wd/hub --log "$APPIUM_LOG_FILE" --log-level info &
APPIUM_PID=$!

echo "Waiting for Appium to start (up to 60s)..."
APPIUM_READY=0
for i in $(seq 1 60); do
  if nc -z 127.0.0.1 4723 2>/dev/null; then
    echo "✅ Appium is running"
    APPIUM_READY=1
    break
  fi
  sleep 1
done

if [ $APPIUM_READY -ne 1 ]; then
  echo "❌ Appium did not start in time"
  echo "--- Appium log ---"
  cat "$APPIUM_LOG_FILE" || true
  exit 1
fi

# Final dialog dismiss before tests
echo "Final dialog dismiss before tests..."
adb shell input keyevent KEYCODE_BACK  2>/dev/null || true
sleep 1
adb shell input keyevent KEYCODE_ENTER 2>/dev/null || true
sleep 1

# ============================================================
# STEP 5: Run Python tests
# In CI, Python deps are installed by the workflow step.
# Locally, activate venv if it exists.
# ============================================================
echo "Setting up Python environment..."
if [ -n "$VIRTUAL_ENV" ]; then
  echo "✅ Already in a virtual environment: $VIRTUAL_ENV"
elif [ -f "$HOME/parking-app-yolo/venv/bin/activate" ]; then
  echo "Activating local venv..."
  source "$HOME/parking-app-yolo/venv/bin/activate"
  pip install pytest pytest-html appium-python-client --quiet
elif command -v python3 &> /dev/null; then
  echo "✅ Using system Python: $(which python3)"
  # In CI, deps are already installed by the workflow step
else
  echo "❌ No Python environment found"
  exit 1
fi

echo "Running Pytest E2E tests..."
python3 -m pytest tests \
  -v \
  --disable-warnings \
  --html="$TEST_REPORT_FILE" \
  --self-contained-html

PYTEST_EXIT=$?

echo "Stopping Appium (PID: $APPIUM_PID)..."
kill $APPIUM_PID 2>/dev/null || true
wait $APPIUM_PID 2>/dev/null || true

exit $PYTEST_EXIT
