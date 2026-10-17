# Moonlight (Grimm) — touchscreen tab and accelerated scrolling

Version **12.2-Grimm.3**, versionCode **317**. Install directly over your existing
Grimm app without uninstalling. Package `com.limelight.sharedip` and its signing
key are unchanged, so pairings, hosts and settings should carry over.

## Phone and touchscreen keyboard access

Open **Settings > Input Settings > Show touchscreen keyboard tab**, enable it,
then start a stream. The setting is off by default and independent of the virtual
controller overlay. Devices without a touchscreen do not show this setting.

- Tap the small tab on the **left edge**, or swipe it to the right.
- Tap **Show keyboard**. The controls fold away and the existing touch keyboard opens.
- Tap any keyboard key to type. Use **Hide** on the keyboard to close it, or reopen
  the tab and tap **Hide keyboard**.
- **Hide controls** folds away the panel while leaving its edge handle available.
  Turn the setting off to remove the handle entirely from subsequent streams.
- The existing **three-finger tap** and **hold Select** shortcuts still work.

The handle stays above the keyboard and respects the left display cutout inset.
Only the small handle area excludes Android's edge-back gesture. Other screen
areas retain ordinary stream touch input. Interrupted or multi-finger handle
gestures do not accidentally open the panel or keyboard.

## Accelerated scrolling in controller mouse mode

Use the existing **hold Start** shortcut to enter mouse emulation.

- **L1 or L2:** scroll up. **R1 or R2:** scroll down.
- A short press scrolls one step immediately.
- Hold for about **0.35 seconds** to begin repeating. The repeat rate gradually
  increases, up to 30 lines/second, for traversing long pages.
- Release to stop and reset the speed. Pressing both directions stops scrolling;
  changing direction begins again at the slow speed.
- Opening the keyboard, changing mouse mode, backgrounding Moonlight, or losing
  focus resets scrolling. Controls held across those transitions must be released
  before they can resume. A fresh trigger press from neutral remains available.

D-pad pointer movement, A/B mouse clicks, analog stick settings, the controller
keyboard, Android's native keyboard capture and the low-bitrate shared-IP fix
remain in this build. No Wolf deployment or restart is performed.

## Device checks

1. Update in place and confirm existing hosts are still paired.
2. Enable the tab and start a phone-only stream. Open the panel with a tap and a
   swipe; show/type/hide the keyboard. Try with the virtual controller disabled.
3. Try the tab with the keyboard already open, in trackpad and direct-touch modes,
   and with the phone's notch on the left. Touch outside the panel should still
   control the host normally. Confirm Android Back remains usable away from it.
4. Cancel a drag or add a second finger while dragging. No action should fire.
5. Disable the setting, reconnect and confirm there is no tab. Three-finger tap
   and hold Select should still open the keyboard.
6. In mouse mode, tap and hold L1/R1 and L2/R2 on a long page. Check acceleration,
   release, direction changes and both-direction cancellation.
7. Hold a scroll control and open the keyboard, background the app, or switch
   mouse mode. Confirm scrolling stops and stays stopped until a new press.
8. Confirm ordinary gameplay controls outside mouse mode, native Hold Esc, and
   both clients at your normal 7.5 Mbps still work as expected.

This build passes compilation, signing and alignment checks plus standalone
input/gesture/navigation/scroll tests. New touch behavior and scrolling still
need these real-device checks. The prior OSK and shared-IP changes have already
received user testing; this task does not claim live testing of the new features.

## Sources and rebuild

Current source is based on Moonlight Android v12.2 (`b48494cb`) and its pinned
native library `874ac9548f1bd6f095ef2b435c42cdde460e7821` with the shared-IP patch.
The complete source archive includes new UI, input helpers and tests. Upstream
licenses remain included; the private signing key is excluded.

Rebuild with `./BUILD-SHARED-IP.sh` using the existing workspace-local toolkit.
Checks: `tests/run-touch-toolbar-gesture.sh`, `tests/run-held-mouse-scroll.sh`,
`tests/run-controller-input.sh`, `tests/run-keyboard-navigation.sh`, and the
shared-IP request tests in `tests/run-rtsp-shared-ip.sh`.
