#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/dual-screen-touchpad-tests
"$toolkit/jdk17/bin/javac" -d build/dual-screen-touchpad-tests \
 app/src/main/java/com/limelight/ui/DualScreenTouchpad.java \
 tests/DualScreenTouchpadCheck.java
"$toolkit/jdk17/bin/java" -cp build/dual-screen-touchpad-tests DualScreenTouchpadCheck
