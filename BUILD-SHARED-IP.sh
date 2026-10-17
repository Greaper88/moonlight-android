#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
toolkit="${MOONLIGHT_BUILD_TOOLS:-$(cd .. && pwd)/moonlight-android-build-tools}"
export JAVA_HOME="$toolkit/jdk17"
export GRADLE_USER_HOME="$toolkit/gradle"
export ANDROID_USER_HOME="$toolkit/android-user"
export ANDROID_HOME="$toolkit/sdk"
./gradlew assembleNonRootDebug --no-daemon --max-workers=4
