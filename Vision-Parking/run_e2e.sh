#!/bin/bash
set -e

# === Global config ===
export APPIUM_LOG_FILE="/tmp/appium.log"
export TEST_REPORT_FILE="tests/report.html"

cleanup_logcat() {
  echo "Capturing final logcat output..."
  adb logcat -d > /tmp/logcat.txt 2>&1 || touch /tmp/logcat.txt
}
trap cleanup_logcat EXIT

# Ensure we're in Vision-Parking directory
cd "$(dirname "$0")"
echo "Current working directory: $PWD"

echo "Waiting for Android device..."
adb wait-for-device

echo "Polling for emulator boot completion..."
for i in $(seq 1 30); do
  BOOT_STATUS=$(adb shell getprop sys.boot_completed | tr -d '\r')
  if [[ "$BOOT_STATUS" == "1" ]]; then
    echo "✅ Emulator boot completed"
    break
  fi
  echo "⏳ Waiting for emulator to boot ($i/30)..."
  sleep 5
done
if [[ "$BOOT_STATUS" != "1" ]]; then
  echo "❌ Emulator did not boot in time."
  exit 1
fi

echo "Waiting for package manager service..."
for i in $(seq 1 30); do
  if adb shell service check package | grep -q "found"; then
    echo "✅ Package manager service is available"
    break
  fi
  echo "⏳ Waiting for package manager service ($i/30)..."
  sleep 5
done

echo "Sleeping 10 seconds before install..."
sleep 10

# === Dismiss any lingering system dialogs before install ===
echo "Dismissing any system dialogs..."
adb shell input keyevent KEYCODE_ENTER 2>/dev/null || true
sleep 2
adb shell input keyevent KEYCODE_ENTER 2>/dev/null || true
sleep 1

echo "Installing app-debug.apk with retries..."
INSTALL_SUCCESS=0
for i in $(seq 1 5); do
  if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
    INSTALL_SUCCESS=1
    echo "✅ APK installed successfully on attempt $i"
    break
  else
    echo "❌ APK install failed on attempt $i, retrying in 30s..."
    sleep 30
  fi
done
if [ $INSTALL_SUCCESS -ne 1 ]; then
  echo "APK install failed after 5 attempts"
  exit 1
fi

# Install Appium globally if needed
if ! command -v appium &> /dev/null; then
  echo "Installing Appium globally..."
  npm install -g appium
fi

# Install uiautomator2 driver (ignore if already installed)
echo "Installing uiautomator2 driver..."
appium driver install uiautomator2 2>/dev/null || echo "Driver already installed, continuing..."

echo "Starting Appium server..."
nohup appium --base-path /wd/hub --log "$APPIUM_LOG_FILE" --log-level info &
APPIUM_PID=$!

echo "Waiting for Appium to start..."
for i in {1..60}; do
  if nc -z 127.0.0.1 4723; then
    echo "✅ Appium is running"
    break
  fi
  sleep 1
done

if ! nc -z 127.0.0.1 4723; then
  echo "❌ Appium did not start"
  cat "$APPIUM_LOG_FILE"
  exit 1
fi

# === Final pre-test: dismiss system dialogs via adb ===
echo "Pre-test: dismissing any system UI dialogs..."
adb shell input keyevent KEYCODE_ENTER 2>/dev/null || true
sleep 1
adb shell input keyevent KEYCODE_BACK 2>/dev/null || true
sleep 1

# Activate virtual environment and install Python dependencies
source ~/parking-app-yolo/venv/bin/activate
pip install pytest pytest-html appium-python-client --quiet

echo "Running Pytest E2E tests..."
pytest tests \
  -v \
  --disable-warnings \
  --html="$TEST_REPORT_FILE" \
  --self-contained-html

PYTEST_EXIT=$?

echo "Stopping Appium..."
kill $APPIUM_PID 2>/dev/null || true
wait $APPIUM_PID 2>/dev/null || true

exit $PYTEST_EXIT
