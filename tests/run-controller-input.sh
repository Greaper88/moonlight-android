#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/controller-tests
"$toolkit/jdk17/bin/javac" -d build/controller-tests \
 app/src/main/java/com/limelight/nvstream/input/ControllerPacket.java \
 app/src/main/java/com/limelight/binding/input/SelectShortcutState.java \
 app/src/main/java/com/limelight/binding/input/DpadMouseMotion.java \
 tests/ControllerInputTest.java
"$toolkit/jdk17/bin/java" -cp build/controller-tests ControllerInputTest
