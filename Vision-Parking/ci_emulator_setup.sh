#!/bin/bash
# ci_emulator_setup.sh
# Called by android-emulator-runner after the emulator boots.
# All multi-line logic lives here so it runs as a proper bash script.
set -e

APP_PACKAGE="${APP_PACKAGE:-com.example.visionpark}"
APK="Vision-Parking/app/build/outputs/apk/debug/app-debug.apk"

echo "=== Step 1: Wait for device ==="
adb wait-for-device

echo "=== Step 2: Wait for boot ==="
adb shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 1; done'
echo "Boot completed"

echo "=== Step 3: Wait for package manager ==="
PM_READY=0
I=0
while [ $I -lt 30 ]; do
  I=$((I+1))
  if adb shell pm list packages > /dev/null 2>&1; then
    echo "Package manager stable after attempt $I"
    PM_READY=1
    break
  fi
  echo "Waiting for package manager... attempt $I"
  sleep 3
done
if [ $PM_READY -ne 1 ]; then
  echo "Package manager never became stable"
  exit 1
fi

echo "=== Step 4: Extra stabilization wait 20s ==="
sleep 20

echo "=== Step 5: Dismiss system dialogs ==="
adb shell input keyevent KEYCODE_BACK  2>/dev/null  || true
sleep 1
adb shell input keyevent KEYCODE_ENTER 2>/dev/null  || true
sleep 1

echo "=== Step 6: Install APK ==="
INSTALL_SUCCESS=0
ATTEMPT=0
while [ $ATTEMPT -lt 5 ]; do
  ATTEMPT=$((ATTEMPT+1))
  echo "Install attempt $ATTEMPT/5..."
  if ! adb shell pm list packages > /dev/null 2>&1; then
    echo "Package manager not responding, waiting 30s..."
    sleep 30
    continue
  fi
  if adb install -r "$APK" 2>&1; then
    INSTALL_SUCCESS=1
    echo "APK installed on attempt $ATTEMPT"
    break
  else
    echo "Install failed on attempt $ATTEMPT, waiting 30s..."
    sleep 30
  fi
done
if [ $INSTALL_SUCCESS -ne 1 ]; then
  echo "APK install failed after 5 attempts"
  adb devices
  exit 1
fi

echo "=== Step 7: Verify installation ==="
adb shell pm list packages | grep "$APP_PACKAGE" || { echo "App not found after install"; exit 1; }

echo "=== Step 8: Grant permissions ==="
adb shell pm grant "$APP_PACKAGE" android.permission.CAMERA               || true
adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_FINE_LOCATION  || true
adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_COARSE_LOCATION || true
adb shell pm grant "$APP_PACKAGE" android.permission.WRITE_EXTERNAL_STORAGE || true
adb shell pm grant "$APP_PACKAGE" android.permission.READ_EXTERNAL_STORAGE  || true

echo "=== Step 9: Clear app data ==="
adb shell pm clear "$APP_PACKAGE" || echo "Could not clear app data"

echo "=== Step 10: Launch and verify app ==="
adb shell am start -n "$APP_PACKAGE/com.example.visionpark.activities.SplashScreenActivity"
sleep 8
adb shell "ps -A 2>/dev/null | grep $APP_PACKAGE" \
  || adb shell "ps 2>/dev/null | grep $APP_PACKAGE" \
  || echo "App process not found (may be normal)"
adb shell dumpsys activity activities | grep "mResumedActivity" \
  || echo "Could not get current activity"

echo "=== Step 11: Stop app before tests ==="
adb shell am force-stop "$APP_PACKAGE"
sleep 2

echo "=== Step 12: Run E2E tests ==="
cd Vision-Parking
timeout 25m ./run_e2e.sh
