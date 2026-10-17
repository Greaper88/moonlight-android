#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/secondary-display-selection-tests
"$toolkit/jdk17/bin/javac" -d build/secondary-display-selection-tests \
 app/src/main/java/com/limelight/ui/SecondaryDisplaySelection.java \
 tests/SecondaryDisplaySelectionCheck.java
"$toolkit/jdk17/bin/java" -cp build/secondary-display-selection-tests SecondaryDisplaySelectionCheck
