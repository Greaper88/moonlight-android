# Moonlight (Grimm) — optional dual-screen controls

Version **12.2.g5**, versionCode **319**. Install over the current Grimm app
without uninstalling. Package `com.limelight.sharedip` and signing key are retained;
existing pairings, hosts and settings should carry over.

## Enable only when wanted

**Settings > Input Settings > Use second screen for controls** is **off by default**.
A second display or DeX connection alone does not activate this mode.

When enabled, **Controls screen** lets you choose Automatic or a named display.
The list shows screen resolution to distinguish displays. Reopen Settings if a
new display was attached while that settings screen was already open.

For Thor, launch Grimm on the **upper screen**, enable the option, select the
lower screen if necessary, and start a stream. The stream stays on the display
where the streaming Activity runs. Display IDs are not assumed to be 0 and 1.

## Lower-screen controls

- Default view is a **relative touchpad**, with explicit left/right mouse buttons.
- Move one finger to move the pointer; tap to left click.
- Tap with two fingers to right click; move two fingers vertically to scroll.
- Hold the left or right mouse button and move a finger on the pad to drag.
- Tap **Show keyboard** to open the Grimm OSK on the lower screen. The remaining
  touchpad area stays available. On smaller screens, the explicit mouse buttons
  hide while the keyboard is visible to leave room for usable keys.
- **Hide keyboard**, the keyboard's **Hide** key, **B**, or holding **Select** closes
  the keyboard and restores the full pad. Hold Select can also open it.
- Controller input normally continues to the game while only the touchpad is
  visible. While the keyboard is open, D-pad/A/B navigate/type/close it, matching
  the existing OSK behavior.

This is a local control surface using the existing streaming connection. It does
not create a second video stream or a second Wolf session.

## Secondary-screen statistics

Tap **Show stats** in the upper-left, opposite **Show keyboard**, to replace the
touchpad with a scrollable stream/device readout. **Hide stats** restores the
touchpad and mouse buttons. The keyboard can remain open alongside stats. Stats
start hidden each time the secondary controls are created.

Stream readings include the existing Moonlight resolution/FPS, decoder, incoming
and rendered FPS, network drops/latency, host processing latency when supplied,
and decoding time. The configured bitrate is labeled **Bitrate target**, not a
measurement of actual traffic. Stream readings update about once per second
while frames arrive, independently of the primary-screen performance overlay.

Device readings include manufacturer/model, Android version, battery level/power
state, **battery** temperature, available/total RAM, thermal status on Android 10+
when reported, and both displays' resolution/refresh rate. Device readings refresh
every three seconds while the panel is visible; unsupported readings are omitted.
No CPU/GPU temperature is inferred from battery temperature. Closing the panel,
backgrounding the app, or removing the display stops the extra stats collection.

Reading or scrolling this panel sends no mouse movement or clicks to the host.
Switching to stats releases active touchpad drags/clicks first.

## Fallback and device behavior

Only Android presentation-capable displays are eligible. The current streaming
display, invalid/off displays, and known placeholder/recording displays are
excluded. If the preferred screen is unavailable, another eligible display is
used; without one, the normal on-stream keyboard remains available.

Display removal, focus loss, app backgrounding, PiP, disconnect and shutdown
release held buttons, cancel delayed clicks and close the lower-screen controls.
The presentation uses its own display density and does not request keyboard or
controller focus. Android's existing native keyboard capture stays in the main
streaming window.

**Hardware report (2026-10-17):** Greaper88 confirmed that dual-screen mode works
on their Thor. This confirmation comes from the maintainer's real-device use;
the automated build and control checks are recorded separately.

## Suggested checks

1. Update and confirm your existing pairing works. Leave dual-screen mode OFF,
   connect DeX or another display and confirm normal single-screen behavior.
2. On Thor, start Grimm on the top screen, explicitly enable dual-screen controls,
   choose the lower display and connect. Confirm video remains unobstructed on top.
3. Move/tap/scroll/drag on the lower pad and confirm the host responds correctly.
   Check gamepad controls continue to reach the game with the keyboard closed.
4. Show the lower keyboard, type mixed case and punctuation, test Ctrl+A and
   Backspace, and hide it by touch and hold Select. Check no game input is stuck.
5. Use both screens' touch input if desired; ensure lower touches do not pause
   streaming, disconnect input or move the upper Activity to the lower screen.
6. While holding a lower mouse button, background the app, sleep/remove the
   display, then resume. There should be no stuck click, delayed click or scrolling.
7. Remove the controls display while the OSK is open. The normal keyboard should
   be available as fallback; reconnect/wake and confirm controls can return.
8. Recheck the existing touchscreen edge tab, accelerated scroll, Hold Esc and
   two clients at 7.5 Mbps behind the same IP. Wolf is not changed or restarted.
9. With the normal performance overlay OFF, open **Show stats** on the lower
   screen. Confirm FPS/latency update while the primary overlay stays off. Scroll
   the readout and confirm no host clicks/scrolling occur. Toggle the keyboard
   while stats are open, then hide stats and check the pad/drag buttons return.
10. Repeat with the normal overlay ON and confirm it remains visible after closing
    the lower stats. Background/remove the display while stats are open; resume
    and confirm there are no stuck inputs and stats begin hidden.

## Build and checks

Current sources remain Moonlight Android v12.2 plus Grimm changes and the tested
native shared-IP patch. Rebuild with `./BUILD-SHARED-IP.sh` and the workspace-local
JDK17/SDK37.0/NDK29 toolkit. Keep the original signing key for future updates; it
is not included in source archives. Upstream licenses remain included.

New standalone checks: `tests/run-secondary-display-selection.sh` and
`tests/run-dual-screen-touchpad.sh`. Earlier controller, navigation, toolbar and
scroll checks remain available under tests/. The native libraries are unchanged
from the prior tested APK.
