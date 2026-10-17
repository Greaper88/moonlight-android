# Moonlight (Grimm) — optional dual-screen controls

Version **12.2-Grimm.4**, versionCode **318**. Install over the current Grimm app
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

**Actual Thor hardware testing is still needed.** Its firmware must expose the
lower panel as a presentation display, and touches there must not steal the main
Activity's input focus. The implementation follows the presentation approach used
by melonDS Android, but a successful build and unit checks do not establish those
OEM behaviors. There is no real Thor attached to this build environment.

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

## Build and checks

Current sources remain Moonlight Android v12.2 plus Grimm changes and the tested
native shared-IP patch. Rebuild with `./BUILD-SHARED-IP.sh` and the workspace-local
JDK17/SDK37.0/NDK29 toolkit. Keep the original signing key for future updates; it
is not included in source archives. Upstream licenses remain included.

New standalone checks: `tests/run-secondary-display-selection.sh` and
`tests/run-dual-screen-touchpad.sh`. Earlier controller, navigation, toolbar and
scroll checks remain available under tests/. The native libraries are unchanged
from the prior tested APK.
