#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/keyboard-state-tests
"$toolkit/jdk17/bin/javac" -cp "$toolkit/sdk/platforms/android-37.0/android.jar" \
 -d build/keyboard-state-tests \
 app/src/main/java/com/limelight/nvstream/input/ControllerPacket.java \
 app/src/main/java/com/limelight/nvstream/input/KeyboardPacket.java \
 app/src/main/java/com/limelight/ui/StreamKeyboardState.java \
 tests/StreamKeyboardStateCheck.java
# KeyEvent's integer constants are inlined, so these tests need no Android runtime.
"$toolkit/jdk17/bin/java" -cp build/keyboard-state-tests StreamKeyboardStateCheck
