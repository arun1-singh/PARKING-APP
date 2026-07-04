#!/bin/bash
# ci_emulator_setup.sh
# Executed by ReactiveCircus/android-emulator-runner after emulator boots.
# The emulator-runner already waits for boot before calling this script,
# so we skip redundant boot checks and go straight to the essentials.
set -e

APP_PACKAGE="${APP_PACKAGE:-com.example.visionpark}"
APK="Vision-Parking/app/build/outputs/apk/debug/app-debug.apk"

echo ">>> Waiting for package manager..."
I=0
while [ $I -lt 20 ]; do
  I=$((I+1))
  if adb shell pm list packages > /dev/null 2>&1; then
    echo ">>> Package manager ready (attempt $I)"
    break
  fi
  sleep 3
done

# Brief stabilization
sleep 5

echo ">>> Disabling animations (best-effort, errors suppressed)..."
adb shell settings put global window_animation_scale 0.0    2>/dev/null || true
adb shell settings put global transition_animation_scale 0.0 2>/dev/null || true
adb shell settings put global animator_duration_scale 0.0   2>/dev/null || true

echo ">>> Installing APK..."
INSTALLED=0
ATTEMPT=0
while [ $ATTEMPT -lt 3 ]; do
  ATTEMPT=$((ATTEMPT+1))
  if adb install -r "$APK" 2>&1; then
    INSTALLED=1
    echo ">>> APK installed on attempt $ATTEMPT"
    break
  fi
  echo ">>> Install attempt $ATTEMPT failed, waiting 10s..."
  sleep 10
done

if [ $INSTALLED -ne 1 ]; then
  echo ">>> APK install failed"
  exit 1
fi

echo ">>> Verifying install..."
adb shell pm list packages | grep "$APP_PACKAGE" || { echo ">>> App not found"; exit 1; }

echo ">>> Granting permissions..."
adb shell pm grant "$APP_PACKAGE" android.permission.CAMERA               2>/dev/null || true
adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_FINE_LOCATION  2>/dev/null || true
adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb shell pm grant "$APP_PACKAGE" android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true
adb shell pm grant "$APP_PACKAGE" android.permission.READ_EXTERNAL_STORAGE  2>/dev/null || true

echo ">>> Clearing app data..."
adb shell pm clear "$APP_PACKAGE" 2>/dev/null || true

echo ">>> Running E2E tests..."
cd Vision-Parking
bash run_e2e.sh
