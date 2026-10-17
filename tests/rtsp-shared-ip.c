// Include the implementation to exercise its internal target/header selection.
#include "RtspConnection.c"
#undef NDEBUG
#include <assert.h>
STREAM_CONFIGURATION StreamConfig;
AUDIO_RENDERER_CALLBACKS AudioCallbacks;
CONNECTION_LISTENER_CALLBACKS ListenerCallbacks;
struct sockaddr_storage RemoteAddr;
uint16_t RtspPortNumber = 48010;

static void check(const char* sessionUrl, int bitrate, int caps, int remote,
                  int audio, const char* expectedHost, const char* expectedTarget) {
    SERVER_INFORMATION server = {0};
    server.rtspSessionUrl = (char*)sessionUrl;
    StreamConfig.bitrate = bitrate;
    StreamConfig.streamingRemotely = remote;
    StreamConfig.audioConfiguration = audio;
    AudioCallbacks.capabilities = caps;
    useEnet = false;
    initializeRtspTarget(&server);
    assert(strcmp(urlAddr, expectedHost) == 0);
    assert(strcmp(rtspTargetUrl, expectedTarget) == 0);
    const char* methods[] = {"OPTIONS", "DESCRIBE", "SETUP", "ANNOUNCE", "PLAY"};
    for (size_t i = 0; i < sizeof(methods)/sizeof(methods[0]); i++) {
        RTSP_MESSAGE request;
        char* target = i == 2 || i == 4 ? "streamid=audio/0/0" : rtspTargetUrl;
        assert(initializeRtspRequest(&request, (char*)methods[i], target));
        assert(strcmp(getOptionContent(request.options, "Host"), expectedHost) == 0);
        int length;
        char* wire = serializeRtspMessage(&request, &length);
        assert(wire != NULL);
        char hostLine[256];
        snprintf(hostLine, sizeof(hostLine), "Host: %s\r\n", expectedHost);
        assert(strstr(wire, hostLine) != NULL);
        free(wire);
        freeMessage(&request);
    }
}
int main(void) {
    const char* first = "rtsp://123.45.67.89:48010";
    const char* second = "rtsp://98.76.54.32:48010";
    const int rates[] = {500, 1000, 5000, 7500, 10000, 14999};
    for (size_t i = 0; i < sizeof(rates)/sizeof(rates[0]); i++) {
        check(first, rates[i], 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
              "123.45.67.89", "rtsp://0.0.0.0:48010");
        check(second, rates[i], 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
              "98.76.54.32", "rtsp://0.0.0.0:48010");
    }
    check(first, 15000, 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO, "123.45.67.89", first);
    check(first, 20000, 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO, "123.45.67.89", first);
    check(first, 20000, CAPABILITY_SLOW_OPUS_DECODER, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
          "123.45.67.89", "rtsp://0.0.0.0:48010");
    check(first, 20000, 0, STREAM_CFG_REMOTE, AUDIO_CONFIGURATION_51_SURROUND,
          "123.45.67.89", "rtsp://0.0.0.0:48010");
    check(NULL, 5000, 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
          "0.0.0.0", "rtsp://0.0.0.0:48010");
    check("invalid", 5000, 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
          "0.0.0.0", "rtsp://0.0.0.0:48010");
    check("rtsp://[2001:db8::1234]:48010/path", 5000, 0, STREAM_CFG_LOCAL, AUDIO_CONFIGURATION_STEREO,
          "[2001:db8::1234]", "rtsp://0.0.0.0:48010");
    puts("PASS: 19 session configurations, 95 serialized RTSP requests");
}
