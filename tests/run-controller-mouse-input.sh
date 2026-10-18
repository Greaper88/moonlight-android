#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
mkdir -p build/controller-mouse-tests
"$toolkit/jdk17/bin/javac" -d build/controller-mouse-tests \
 app/src/main/java/com/limelight/nvstream/input/ControllerPacket.java \
 app/src/main/java/com/limelight/binding/input/DpadMouseMotion.java \
 app/src/main/java/com/limelight/binding/input/HeldMouseScroll.java \
 app/src/main/java/com/limelight/binding/input/ControllerMouseInput.java \
 tests/ControllerMouseInputTest.java
"$toolkit/jdk17/bin/java" -cp build/controller-mouse-tests ControllerMouseInputTest
