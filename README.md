# Moonlight (Grimm)

A personal fork of [Moonlight for Android](https://github.com/moonlight-stream/moonlight-android), based on v12.2, with an on-screen keyboard (OSK), optional dual-screen controls, and shared-IP fixes for use with [Greaper88's Wolf fork](https://github.com/Greaper88/wolf).

- **OSK:** touch and controller support, hold Select to toggle, and an optional touchscreen edge tab.
- **Dual-screen devices:** keep the stream on one display and use another as a touchpad and keyboard. Enable **Settings > Input Settings > Use second screen for controls**, then choose the **Controls screen**. This is **off by default**, including when connecting DeX. **Show stats** in the upper-left opens live stream and device readings; **Hide stats** restores the touchpad.
- **Controller mouse controls:** D-pad movement and accelerated scrolling when holding the triggers or shoulder buttons in mouse mode.
- **Low-bitrate shared-IP fix:** preserve the RTSP host identifier used by the Wolf fork to distinguish clients behind the same public IP.

Download the APK from [Grimm releases](https://github.com/Greaper88/moonlight-android/releases).

Grimm uses package `com.limelight.sharedip`. Install new Grimm releases over an existing Grimm installation to retain pairings and settings. The upstream Play Store app is a separate installation.

Dual-screen mode is confirmed working on Greaper88's Thor. See the [dual-screen setup and testing guide](GRIMM-DUAL-SCREEN-TESTING.md) and [other Grimm testing notes](GRIMM-TESTING.md).

Grimm versions use `<upstream version>.g<revision>`, such as **12.2.g5**. The `g` identifies the Grimm revision; this numbering continues from the earlier `12.2-Grimm.1` through `.4` builds. Android's internal version code keeps increasing across all updates.

## AI disclosure

The Grimm changes were developed with AI assistance using **GPT-6.1 Sol Ultra**, under the maintainer's direction. Automated builds and checks are recorded separately from testing on real devices; a passing build does not establish device compatibility. Upstream Moonlight authors retain credit for the original project.

## Upstream Moonlight Android

[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Upstream downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
