import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.ui.StreamKeyboardState;

import java.util.ArrayList;
import java.util.List;

public class StreamKeyboardStateCheck {
    private static int assertions;

    private static void check(boolean result, String description) {
        assertions++;
        if (!result) throw new AssertionError(description);
    }

    private static final class Event {
        final int key;
        final int modifiers;

        Event(int key, int modifiers) {
            this.key = key;
            this.modifiers = modifiers;
        }
    }

    private static final class Keyboard {
        final List<Event> events = new ArrayList<>();
        final StreamKeyboardState state;
        long now = 1_000;

        Keyboard() {
            this(false);
        }

        Keyboard(boolean singlePressLock) {
            state = new StreamKeyboardState((key, modifiers) -> events.add(new Event(key, modifiers)),
                    singlePressLock, () -> now);
        }

        void advance(long milliseconds) {
            now += milliseconds;
        }

        void expectLast(int key, int modifiers, String description) {
            check(!events.isEmpty(), description + ": emitted a key");
            Event last = events.get(events.size() - 1);
            check(last.key == key && last.modifiers == modifiers, description);
        }
    }

    private static void mappingsAndLabels() {
        Keyboard keyboard = new Keyboard();
        int[] bases = {KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_EQUALS};
        int[] functions = {KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_F3,
                KeyEvent.KEYCODE_F4, KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_F6,
                KeyEvent.KEYCODE_F7, KeyEvent.KEYCODE_F8, KeyEvent.KEYCODE_F9,
                KeyEvent.KEYCODE_F10, KeyEvent.KEYCODE_F11, KeyEvent.KEYCODE_F12};
        String[] normal = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="};
        String[] shifted = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+"};

        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Initial layers off");
        for (int i = 0; i < bases.length; i++) {
            check(normal[i].equals(keyboard.state.getLabel(bases[i], normal[i], shifted[i])), "Normal number-row label " + i);
            keyboard.state.activateKey(bases[i]);
            keyboard.expectLast(bases[i], 0, "Normal number-row key " + i);
        }
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        for (int i = 0; i < bases.length; i++) {
            check(shifted[i].equals(keyboard.state.getLabel(bases[i], normal[i], shifted[i])), "Shift number-row label " + i);
        }
        keyboard.state.toggleFunctionLayer();
        for (int i = 0; i < bases.length; i++) {
            check(("F" + (i + 1)).equals(keyboard.state.getLabel(bases[i], normal[i], shifted[i])), "Fn label wins over Shift " + i);
            keyboard.state.activateKey(bases[i]);
            keyboard.expectLast(functions[i], KeyboardPacket.MODIFIER_SHIFT, "Shift+Fn key " + i);
        }
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        for (int i = 0; i < bases.length; i++) {
            check(("F" + (i + 1)).equals(keyboard.state.getLabel(bases[i], normal[i], shifted[i])), "Fn label " + i);
            keyboard.state.activateKey(bases[i]);
            keyboard.expectLast(functions[i], 0, "Fn is a local layer without a protocol modifier " + i);
        }
        keyboard.state.toggleFunctionLayer();
        keyboard.state.activateKey(KeyEvent.KEYCODE_EQUALS);
        keyboard.expectLast(KeyEvent.KEYCODE_EQUALS, 0, "Disabling Fn restores number-row key");
    }

    private static void controllerShortcuts() {
        Keyboard keyboard = new Keyboard(true);
        int report = ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG |
                ControllerPacket.X_FLAG | ControllerPacket.Y_FLAG | ControllerPacket.A_FLAG;
        int pressed = keyboard.state.updateControllerButtons(report);
        check(pressed == report, "Return all rising edges for the UI");
        check(keyboard.events.size() == 2, "X and Y each emit once in the report");
        check(keyboard.events.get(0).key == KeyEvent.KEYCODE_DEL &&
                keyboard.events.get(0).modifiers == KeyboardPacket.MODIFIER_SHIFT,
                "LB applies Shift before X in the same report");
        keyboard.expectLast(KeyEvent.KEYCODE_SPACE, KeyboardPacket.MODIFIER_SHIFT, "LB applies Shift before Y");
        check(keyboard.state.isFunctionLayer(), "RB enables Fn before A processing");
        if ((pressed & ControllerPacket.A_FLAG) != 0) keyboard.state.activateKey(KeyEvent.KEYCODE_0);
        keyboard.expectLast(KeyEvent.KEYCODE_F10, KeyboardPacket.MODIFIER_SHIFT, "UI A activation sees same-report Shift and Fn");
        check(keyboard.state.updateControllerButtons(report) == 0, "Held reports have no rising edges");
        check(keyboard.events.size() == 3, "Held X/Y do not repeat");
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Held bumpers do not toggle layers again");

        keyboard.state.updateControllerButtons(report & ~ControllerPacket.X_FLAG);
        keyboard.state.updateControllerButtons(report);
        check(keyboard.events.size() == 4, "Releasing X rearms only X");
        keyboard.expectLast(KeyEvent.KEYCODE_DEL, KeyboardPacket.MODIFIER_SHIFT, "Rearmed X uses current modifiers");
        keyboard.state.updateControllerButtons(0);
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG | ControllerPacket.Y_FLAG);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Released bumpers toggle layers off next press");
        keyboard.expectLast(KeyEvent.KEYCODE_SPACE, 0, "Same-report layer-off applies before Y");
    }

    private static void holdAndRelease() {
        StreamKeyboardState defaultState = new StreamKeyboardState((key, modifiers) -> {});
        defaultState.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        check(defaultState.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && defaultState.isFunctionLayer(),
                "Default constructor holds Shift and Fn");
        defaultState.updateControllerButtons(0);
        check(defaultState.getModifiers() == 0 && !defaultState.isFunctionLayer(), "Default constructor releases held layers");

        Keyboard keyboard = new Keyboard();
        int both = ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG;
        int actions = ControllerPacket.X_FLAG | ControllerPacket.Y_FLAG | ControllerPacket.A_FLAG;
        int pressed = keyboard.state.updateControllerButtons(both | actions);
        check(pressed == (both | actions), "Hold mode returns all shortcut edges");
        check(keyboard.events.size() == 2, "Hold mode emits X and Y once");
        keyboard.expectLast(KeyEvent.KEYCODE_SPACE, KeyboardPacket.MODIFIER_SHIFT, "Hold applies before same-report Y");
        if ((pressed & ControllerPacket.A_FLAG) != 0) keyboard.state.activateKey(KeyEvent.KEYCODE_1);
        keyboard.expectLast(KeyEvent.KEYCODE_F1, KeyboardPacket.MODIFIER_SHIFT, "Hold applies before same-report A");
        keyboard.advance(1_000);
        keyboard.state.updateControllerButtons(both | actions);
        check(keyboard.events.size() == 3, "Holding shortcuts does not repeat");
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Long holds remain active until release");
        keyboard.state.updateControllerButtons(both);
        pressed = keyboard.state.updateControllerButtons(actions);
        check(pressed == actions, "Release report still returns new action edges");
        keyboard.expectLast(KeyEvent.KEYCODE_SPACE, 0, "Modifier releases happen before same-report X/Y");
        if ((pressed & ControllerPacket.A_FLAG) != 0) keyboard.state.activateKey(KeyEvent.KEYCODE_1);
        keyboard.expectLast(KeyEvent.KEYCODE_1, 0, "Modifier releases happen before UI A activation");
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Release clears both momentary layers");
        keyboard.state.updateControllerButtons(both);
        keyboard.state.updateControllerButtons(ControllerPacket.RB_FLAG);
        check(keyboard.state.getModifiers() == 0 && keyboard.state.isFunctionLayer(), "Each held layer releases independently");
        keyboard.state.updateControllerButtons(ControllerPacket.B_FLAG | ControllerPacket.X_FLAG);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "B report still releases held Fn");
        check(keyboard.events.size() == 6, "B suppresses new X while processing modifier releases");
    }

    private static void active(Keyboard keyboard, int flag, boolean expected, String description) {
        boolean result = flag == ControllerPacket.LB_FLAG ?
                (keyboard.state.getModifiers() & KeyboardPacket.MODIFIER_SHIFT) != 0 : keyboard.state.isFunctionLayer();
        check(result == expected, description);
    }

    private static void doubleTapLocks() {
        for (int flag : new int[] {ControllerPacket.LB_FLAG, ControllerPacket.RB_FLAG}) {
            Keyboard keyboard = new Keyboard();
            keyboard.state.updateControllerButtons(flag);
            active(keyboard, flag, true, "First tap is momentary");
            keyboard.advance(40);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "First tap release turns layer off");
            keyboard.advance(300);
            keyboard.state.updateControllerButtons(flag);
            active(keyboard, flag, true, "Second press at exact 300 ms release boundary locks");
            keyboard.advance(1_000);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, true, "Double-tap lock survives second release and long second hold");
            keyboard.advance(1_000);
            keyboard.state.updateControllerButtons(flag);
            active(keyboard, flag, false, "Pressing a locked bumper unlocks immediately");
            keyboard.advance(1_000);
            keyboard.state.updateControllerButtons(flag);
            active(keyboard, flag, false, "Unlocking press never reactivates as a hold");
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "Unlocking release remains off");
            keyboard.state.updateControllerButtons(flag);
            active(keyboard, flag, true, "Next fresh press can hold after unlocking");
            keyboard.advance(301);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "Long hold release turns layer off");
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(10);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "Long first hold did not prime a double-tap lock");

            keyboard.state.resetInput();
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(10);
            keyboard.state.updateControllerButtons(0);
            keyboard.advance(301);
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(10);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "A second press outside the 300 ms window stays momentary");

            keyboard.state.resetInput();
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(300);
            keyboard.state.updateControllerButtons(0);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(flag);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, true, "A first tap lasting exactly 300 ms may prime a lock");
        }
    }

    private static void independentLayersAndTouch() {
        Keyboard keyboard = new Keyboard();
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG);
        keyboard.advance(20);
        keyboard.state.updateControllerButtons(0);
        keyboard.advance(20);
        keyboard.state.updateControllerButtons(ControllerPacket.RB_FLAG);
        keyboard.advance(20);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Tapping different bumpers does not double-tap either");
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && !keyboard.state.isFunctionLayer(),
                "Shift history is independent of Fn history");
        keyboard.state.updateControllerButtons(ControllerPacket.RB_FLAG);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Both layers can lock independently");
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Touch toggles unlock controller locks");
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Bumpers unlock touch latches immediately");
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Unlocked touch latches stay off on release");

        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        keyboard.advance(500);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Touch can latch a currently held layer so it survives release");
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Next bumper press unlocks latched holds");

        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Turning touch latches off does not cancel physical holds");
        keyboard.advance(500);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Held layer finally turns off when released");
    }

    private static void typingDoesNotLock() {
        for (int flag : new int[] {ControllerPacket.LB_FLAG, ControllerPacket.RB_FLAG}) {
            Keyboard keyboard = new Keyboard();
            int base = flag == ControllerPacket.LB_FLAG ? KeyEvent.KEYCODE_A : KeyEvent.KEYCODE_1;
            int result = flag == ControllerPacket.LB_FLAG ? KeyEvent.KEYCODE_A : KeyEvent.KEYCODE_F1;
            int modifiers = flag == ControllerPacket.LB_FLAG ? KeyboardPacket.MODIFIER_SHIFT : 0;
            for (int chord = 0; chord < 2; chord++) {
                keyboard.state.updateControllerButtons(flag);
                keyboard.advance(20);
                keyboard.state.activateKey(base);
                keyboard.expectLast(result, modifiers, "Rapid chord uses the held layer");
                keyboard.advance(20);
                keyboard.state.updateControllerButtons(0);
                active(keyboard, flag, false, "Rapid typed chord does not prime a lock");
                keyboard.advance(20);
            }
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "First clean tap after typing is still momentary");
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, true, "Clean double taps still lock after earlier typing");

            keyboard.state.resetInput();
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(0);
            keyboard.state.activateKey(KeyEvent.KEYCODE_A);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(flag);
            keyboard.advance(20);
            keyboard.state.updateControllerButtons(0);
            active(keyboard, flag, false, "Intervening typing cancels a previous clean tap");
        }

        Keyboard keyboard = new Keyboard();
        int both = ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG;
        for (int shortcut : new int[] {ControllerPacket.X_FLAG, ControllerPacket.Y_FLAG}) {
            keyboard.state.resetInput();
            for (int chord = 0; chord < 2; chord++) {
                keyboard.state.updateControllerButtons(both | shortcut);
                keyboard.advance(20);
                keyboard.state.updateControllerButtons(0);
                check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(),
                        "X/Y shortcuts disqualify both held bumpers from double-tap lock");
                keyboard.advance(20);
            }
        }
    }

    private static void singlePressLockMode() {
        Keyboard keyboard = new Keyboard(true);
        int both = ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG;
        keyboard.state.updateControllerButtons(both);
        keyboard.advance(500);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Single-press lock persists after a long first press is released");
        keyboard.state.updateControllerButtons(both);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Single-press mode toggles locks off");
        keyboard.state.updateControllerButtons(0);
        keyboard.state.updateControllerButtons(both);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == KeyboardPacket.MODIFIER_SHIFT && keyboard.state.isFunctionLayer(),
                "Rapid single-press-mode taps toggle each time");
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        keyboard.state.toggleFunctionLayer();
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Touch and single-press mode share latches");
    }

    private static void closePriorityAndReset() {
        Keyboard keyboard = new Keyboard();
        int all = ControllerPacket.B_FLAG | ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG |
                ControllerPacket.X_FLAG | ControllerPacket.Y_FLAG | ControllerPacket.A_FLAG;
        check(keyboard.state.updateControllerButtons(all) == all, "B report edges remain available to close UI");
        check(keyboard.events.isEmpty(), "B blocks X and Y in its report");
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "B blocks bumper toggles");
        check(keyboard.state.updateControllerButtons(all) == 0, "B report is recorded, avoiding later delayed actions");
        check(keyboard.events.isEmpty(), "Held B report emits nothing");

        keyboard.state.updateControllerButtons(0);
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG | ControllerPacket.X_FLAG);
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_CTRL);
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_ALT);
        int count = keyboard.events.size();
        keyboard.state.resetInput();
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Reset clears all modifiers and Fn");
        check(keyboard.events.size() == count, "Reset emits no host key");
        check(keyboard.state.updateControllerButtons(ControllerPacket.X_FLAG) == ControllerPacket.X_FLAG,
                "Reset clears controller history");
        keyboard.expectLast(KeyEvent.KEYCODE_DEL, 0, "First shortcut after reset has no old modifiers");
        keyboard.state.resetInput();
        keyboard.state.resetInput();
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Reset is idempotent");
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        keyboard.advance(20);
        keyboard.state.updateControllerButtons(0);
        keyboard.state.resetInput();
        keyboard.state.updateControllerButtons(ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG);
        keyboard.advance(20);
        keyboard.state.updateControllerButtons(0);
        check(keyboard.state.getModifiers() == 0 && !keyboard.state.isFunctionLayer(), "Reset clears double-tap history");
    }

    private static void ordinaryKeysAndSuper() {
        Keyboard keyboard = new Keyboard();
        keyboard.state.toggleFunctionLayer();
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_CTRL);
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_ALT);
        keyboard.state.activateKey(KeyEvent.KEYCODE_A);
        keyboard.expectLast(KeyEvent.KEYCODE_A, KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT,
                "Fn leaves ordinary keys and protocol modifiers intact");
        check("a".equals(keyboard.state.getLabel(KeyEvent.KEYCODE_A, "a", "A")), "Fn leaves ordinary labels intact");
        keyboard.state.toggleModifier(KeyboardPacket.MODIFIER_SHIFT);
        check("A".equals(keyboard.state.getLabel(KeyEvent.KEYCODE_A, "a", "A")), "Ordinary labels still respect Shift with Fn");
        keyboard.state.activateKey(KeyEvent.KEYCODE_ESCAPE);
        keyboard.expectLast(KeyEvent.KEYCODE_ESCAPE, KeyboardPacket.MODIFIER_CTRL | KeyboardPacket.MODIFIER_ALT |
                KeyboardPacket.MODIFIER_SHIFT, "Fn preserves Escape chord");

        keyboard.state.resetInput();
        keyboard.state.toggleFunctionLayer();
        keyboard.state.activateKey(KeyEvent.KEYCODE_META_LEFT);
        keyboard.expectLast(KeyEvent.KEYCODE_META_LEFT, 0, "Super is a standalone key activation");
        keyboard.state.activateKey(KeyEvent.KEYCODE_A);
        keyboard.expectLast(KeyEvent.KEYCODE_A, 0, "Super does not latch a Meta modifier for the next key");
        check(keyboard.state.getModifiers() == 0, "Local Fn and Super add no protocol modifier bits");
    }

    public static void main(String[] arguments) {
        mappingsAndLabels();
        controllerShortcuts();
        holdAndRelease();
        doubleTapLocks();
        independentLayersAndTouch();
        typingDoesNotLock();
        singlePressLockMode();
        closePriorityAndReset();
        ordinaryKeysAndSuper();
        System.out.println(assertions + " keyboard state checks passed");
    }
}
