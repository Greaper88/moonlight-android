#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/touch-toolbar-gesture-tests
"$toolkit/jdk17/bin/javac" -d build/touch-toolbar-gesture-tests \
 app/src/main/java/com/limelight/ui/StreamTouchToolbarGesture.java \
 tests/StreamTouchToolbarGestureCheck.java
"$toolkit/jdk17/bin/java" -cp build/touch-toolbar-gesture-tests StreamTouchToolbarGestureCheck
