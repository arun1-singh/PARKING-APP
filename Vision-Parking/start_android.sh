#!/bin/bash

# 1. Kill emulator forcefully
echo "Force killing any existing emulator instances..."
~/Android/Sdk/platform-tools/adb emu kill 2>/dev/null || true
pkill -9 -f emulator || true
pkill -9 -f qemu || true
sleep 3

# 2. Clean up lock files
echo "Cleaning up lock files and cache..."
rm -rf ~/.android/avd/*.lock
rm -rf ~/.android/cache

# 3. Kill ADB server
echo "Restarting ADB server..."
~/Android/Sdk/platform-tools/adb kill-server
sleep 1

# 4. Start emulator with clean state
echo "Starting emulator Pixel_4_API_30 with swiftshader and clean state..."
~/Android/Sdk/emulator/emulator -avd Pixel_4_API_30 \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -wipe-data \
  -no-audio &

# 5. Wait for boot
echo "Waiting 90 seconds for emulator to boot..."
sleep 90

# 6. Start ADB and list devices
~/Android/Sdk/platform-tools/adb start-server
echo "Checking attached devices:"
~/Android/Sdk/platform-tools/adb devices

# 7. Compile and build the project
echo "Compiling project to check for Java compiler errors..."
cd ~/parking-app-yolo/parking_app_integration/Vision-Parking
./gradlew compileDebugJavaWithJavac 2>&1 | tail -50

echo "Assembling debug APK..."
./gradlew assembleDebug 2>&1 | tail -5

# 8. Install APK to emulator
echo "Installing APK to the emulator..."
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
