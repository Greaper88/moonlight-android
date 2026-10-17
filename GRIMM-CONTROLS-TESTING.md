# Moonlight (Grimm) 12.2 — keyboard and D-pad test build

Version **12.2-Grimm.2**, versionCode **316**. Based on upstream Moonlight Android
v12.2 (`b48494cb`) and native library `874ac9548f1bd6f095ef2b435c42cdde460e7821`.
Retains the tested low-bitrate shared-IP fix and Android 16.1 keyboard capture.

Install directly over the existing Grimm or Shared IP test app. Do not uninstall.
Package remains `com.limelight.sharedip`, signed by the same local key. Existing
pairings, settings and hosts should carry over. Stock Moonlight is separate.

## Keyboard

- **Hold Select alone for 0.75 seconds** to show or hide the keyboard.
- A normal short Select press still reaches the game, on release. Select chords
  with other buttons remain available; those chords do not open the keyboard.
- **D-pad** moves the highlighted key; hold it to repeat navigation.
- **A** activates the highlighted key. **B**, Android Back, or **Hide** closes it.
- Touch any key to type it. The existing three-finger gesture opens this keyboard.
- Letters, numbers, punctuation, Space, Backspace, Enter, Esc, Tab and arrow keys
  are included. **Ctrl, Alt and Shift** toggle locally; active toggles stay lit
  until toggled off or the keyboard closes. Each typed chord releases its remote
  modifiers immediately, so closing the keyboard cannot leave them latched.
- While the keyboard is open, controller inputs navigate it rather than reaching
  the game. Buttons still held when it closes must be released before resuming
  game input. Analog sticks must return to neutral.
- The overlay has compact sizing for small screens. It uses a US-style layout
  and sends key events interpreted by the host's configured keyboard layout.

## Mouse emulation

- Keep the existing **hold Start** shortcut to toggle mouse emulation; the Moonlight
  mouse-emulation preference must be enabled, as before.
- **D-pad** now moves the pointer, including diagonals. It starts slowly and
  accelerates while held; release stops it.
- **A** is left click; **B** is right click. **L1/R1** scroll up/down.
- Analog stick pointer/scroll behavior remains available according to settings.
- In normal gamepad mode, D-pad input still goes to the game.

## Suggested device checks

1. Confirm installation updates in place and hosts remain paired.
2. In a text field, try normal Select, hold Select, D-pad navigation and A typing.
   Confirm the hold doesn't trigger a Select action in the game.
3. Type a username, mixed case, numbers and punctuation. Test Backspace, Enter,
   Esc, Tab and Ctrl+A. Toggle modifiers off afterward, or close/reopen the OSK.
4. Hold Select to close, reopen, then close with B and with touch. Confirm B does
   not also reach the game and no direction, key or modifier remains stuck.
5. Enable mouse emulation with Start. Test D-pad movement/diagonals/acceleration,
   A/B clicks and shoulder scrolling, then toggle it off and verify game D-pad.
6. Open/close the keyboard in mouse mode, including while holding a direction,
   click or stick. Pointer motion should stop while the keyboard is visible.
7. Disconnect a controller while navigating the keyboard, then reconnect it.
   Test two controllers if available: idle reports from one must not repeat the
   other's key activation.
8. Recheck native Hold Esc behavior and two clients at **7.5 Mbps** behind the same
   IP. This build does not modify, deploy or restart Wolf.

Automated checks cover Select timing/chords/cleanup, D-pad motion/diagonal speed,
weighted keyboard navigation, and the RTSP shared-IP regression. The APK is built
and signature/alignment checked; the new controls still need real-device testing.

## Rebuild

Run `./BUILD-SHARED-IP.sh` with the workspace-local JDK17/SDK37.0/NDK29 toolkit.
The script uses the original signing key in the toolkit's android-user directory.
Keep that key for future compatible updates; it is not in the source archive.

Checks: `./tests/run-controller-input.sh`, `./tests/run-keyboard-navigation.sh`,
and `./tests/run-rtsp-shared-ip.sh`. Sources retain their upstream licenses.
