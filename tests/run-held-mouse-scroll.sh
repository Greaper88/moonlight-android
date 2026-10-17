#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/scroll-tests
"$toolkit/jdk17/bin/javac" -d build/scroll-tests \
 app/src/main/java/com/limelight/binding/input/HeldMouseScroll.java \
 tests/HeldMouseScrollTest.java
"$toolkit/jdk17/bin/java" -cp build/scroll-tests HeldMouseScrollTest
