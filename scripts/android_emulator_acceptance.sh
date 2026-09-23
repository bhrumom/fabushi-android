#!/usr/bin/env bash
set -euo pipefail

: "${SOURCE_SHA:?SOURCE_SHA is required}"

PACKAGE=com.ombhrum.fabushi.debug
TEST_PACKAGE=com.ombhrum.fabushi.debug.test
APK="../../exact-head-apk/fabushi-android-${SOURCE_SHA}.apk"
EVIDENCE="../../evidence/emulator"

mkdir -p "$EVIDENCE"

./gradlew --no-daemon assembleDebugAndroidTest
adb uninstall "$PACKAGE" >/dev/null 2>&1 || true
adb install "$APK"
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" \
  | tee "$EVIDENCE/instrumentation.txt"
grep -q "OK (" "$EVIDENCE/instrumentation.txt"

adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
sleep 3
PID_BEFORE="$(adb shell pidof "$PACKAGE" | tr -d '\r')"
test -n "$PID_BEFORE"
adb shell run-as "$PACKAGE" cat shared_prefs/fabushi-coordinator-runtime.xml > "$EVIDENCE/coordinator-before.xml"
GEN_BEFORE="$(sed -n 's/.*name="generation" value="\([0-9][0-9]*\)".*/\1/p' "$EVIDENCE/coordinator-before.xml" | head -n 1)"
test -n "$GEN_BEFORE"

adb shell am force-stop "$PACKAGE"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
sleep 3
PID_AFTER="$(adb shell pidof "$PACKAGE" | tr -d '\r')"
test -n "$PID_AFTER"
test "$PID_BEFORE" != "$PID_AFTER"
adb shell run-as "$PACKAGE" cat shared_prefs/fabushi-coordinator-runtime.xml > "$EVIDENCE/coordinator-after.xml"
GEN_AFTER="$(sed -n 's/.*name="generation" value="\([0-9][0-9]*\)".*/\1/p' "$EVIDENCE/coordinator-after.xml" | head -n 1)"
test -n "$GEN_AFTER"
test "$GEN_AFTER" -gt "$GEN_BEFORE"

printf 'head=%s\npackage=%s\npid_before=%s\npid_after=%s\ngeneration_before=%s\ngeneration_after=%s\n' \
  "$SOURCE_SHA" "$PACKAGE" "$PID_BEFORE" "$PID_AFTER" "$GEN_BEFORE" "$GEN_AFTER" \
  > "$EVIDENCE/process-recreation.txt"
sha256sum "$APK" > "$EVIDENCE/installed-apk.sha256"
adb logcat -d > "$EVIDENCE/logcat.txt"
adb exec-out screencap -p > "$EVIDENCE/relaunch.png"
printf 'head=%s\npackage=%s\n' "$SOURCE_SHA" "$PACKAGE" > "$EVIDENCE/identity.txt"
