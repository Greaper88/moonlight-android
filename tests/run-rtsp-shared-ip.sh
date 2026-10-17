#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
core=app/src/main/jni/moonlight-core/moonlight-common-c
mkdir -p build/shared-ip-tests
cc -g -fsanitize=address,undefined -DHAS_SOCKLEN_T -ffunction-sections -fdata-sections \
  -I"$core/nanors" -I"$core/nanors/deps/obl" -I"$core/src" -I"$core/enet/include" tests/rtsp-shared-ip.c \
  "$core/src/RtspParser.c" "$core/src/Platform.c" "$core/src/PlatformSockets.c" \
  -Wl,--gc-sections -pthread -o build/shared-ip-tests/rtsp-shared-ip
ASAN_OPTIONS=detect_leaks=0 build/shared-ip-tests/rtsp-shared-ip
