#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/keyboard-navigation-tests
"$toolkit/jdk17/bin/javac" -d build/keyboard-navigation-tests \
 app/src/main/java/com/limelight/ui/StreamKeyboardNavigation.java \
 tests/StreamKeyboardNavigationCheck.java
"$toolkit/jdk17/bin/java" -cp build/keyboard-navigation-tests StreamKeyboardNavigationCheck
