# Grimm OSK shortcuts — 12.2.g6

Version **12.2.g6**, versionCode **320**. Update the existing Grimm installation
without uninstalling. The package and signing key are retained, preserving
pairings and settings.

## Keyboard controls

These actions apply while the Grimm OSK is open, on the streaming screen or a
secondary display:

| Button | Action |
| --- | --- |
| D-pad | Move the highlighted key; hold to repeat navigation |
| A | Type or activate the highlighted key |
| B | Close the OSK |
| X | Backspace |
| Y | Space |
| Left bumper (LB/L1) | Hold Shift; double-tap to lock |
| Right bumper (RB/R1) | Hold Fn; double-tap to lock |

The default bumper behavior is **hold to use, double-tap to lock**. Releasing a
single hold turns that layer off. Two short taps within 300 ms lock it on; press
the same bumper again to unlock. Long holds and holds used to type a key do not
count as the first tap.
**Settings > Input Settings > Keyboard bumper behavior** also offers **Single
press to lock**, where one press toggles the layer and releasing has no effect.

The on-screen Shift and Fn keys remain tap-to-toggle in both modes. Their active
state is highlighted. Fn replaces the number-row labels and key events as follows:

| Number-row key | Fn key |
| --- | --- |
| 1–9 | F1–F9 |
| 0 | F10 |
| - | F11 |
| = | F12 |

Shift still applies to function-key chords such as Shift+F10. Fn affects only the
number row and is handled locally; it sends no Fn modifier to the host.
Ctrl, Alt, and locked Shift/Fn remain selected until unlocked or the keyboard is
reset or closed. Each host chord releases its synthetic modifiers immediately.

With controller mouse emulation enabled, joysticks still move/scroll the pointer
and triggers still scroll while the OSK is open. The D-pad and bumpers are
reserved for keyboard navigation and Shift/Fn instead of mouse movement/scrolling.
Fresh analog input is required after opening/closing the keyboard: held sticks
and triggers must first return to neutral. Normal gamepad output remains
suppressed while the OSK is open.

The **Super** key sends one Super/Windows-key press, suitable for opening the
host's launcher or Start menu according to its configuration. It is an ordinary
key rather than a sticky modifier.

## Device checks

1. Open the OSK from Select or touch. Press X and Y in a text field, including
   while different keys are highlighted. Confirm Backspace and Space are sent.
2. Hold LB, type upper-case letters and shifted punctuation with A, then release
   it. Confirm Shift switches off. Double-tap LB to lock, release, then press
   again to unlock. Repeat in the single-press-lock setting.
3. Hold RB and confirm all twelve labels: F1–F9, F10, F11, F12. Type several
   function keys on a host key-event tester; check Shift+F10 and Ctrl+F4. Return
   to numbers by releasing RB. Test Fn lock/unlock and the touch Fn toggle too.
4. Hold a bumper, then release and tap it once; a long hold must not prime a lock.
   Hold X/Y and confirm no duplicate actions from repeated controller reports.
5. Press Super and confirm the host's launcher responds as configured.
6. Close and reopen the OSK. Shift/Fn must reset, numeric labels return, and no
   button should leak into the game or leave a host key held. Repeat after focus
   loss, backgrounding, display removal and controller removal.
7. Test the same controls on the main-screen OSK and Thor's lower-screen OSK.
   Recheck the lower stats panel and touchpad when the keyboard is closed.
8. In mouse mode, open the OSK and neutralize sticks/triggers, then check pointer
   movement and trigger scrolling. D-pad navigation and Shift/Fn must not move
   or scroll the host. Closing with B must not cause a right-click or stuck scroll.

**Device report (2026-10-18):** Greaper88 reported this build working on their Thor
and their friend's Retroid. Thor's dual-screen mode was also confirmed working
on 2026-10-17. Automated checks and APK validation are recorded separately; the
native shared-IP code is unchanged.

Run `tests/run-keyboard-state.sh` for shortcut/Fn state checks and
`tests/run-keyboard-navigation.sh` for navigation checks. Rebuild with
`./BUILD-SHARED-IP.sh` using the existing workspace toolkit and signing key.
