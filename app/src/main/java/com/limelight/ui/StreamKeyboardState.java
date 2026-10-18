package com.limelight.ui;

import android.view.KeyEvent;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;

import java.util.function.LongSupplier;

/** Keyboard layers and controller shortcuts, independent of view focus and Android input dispatch. */
public final class StreamKeyboardState {
    public interface Listener {
        void onKeyActivated(int keyCode, int modifiers);
    }

    private final Listener listener;
    private final boolean singlePressLock;
    private final LongSupplier clock;
    private final ModifierLayer shift = new ModifierLayer();
    private final ModifierLayer function = new ModifierLayer();
    private int modifiers;
    private int lastControllerButtons;

    public StreamKeyboardState(Listener listener) {
        this(listener, false, () -> System.nanoTime() / 1_000_000L);
    }

    public StreamKeyboardState(Listener listener, boolean singlePressLock, LongSupplier clock) {
        this.listener = listener;
        this.singlePressLock = singlePressLock;
        this.clock = clock;
    }

    /** Applies layer changes before shortcuts so a single controller report can form a chord. */
    public int updateControllerButtons(int flags) {
        long now = clock.getAsLong();
        int pressed = flags & ~lastControllerButtons;
        int released = lastControllerButtons & ~flags;
        lastControllerButtons = flags;

        // Even the report that closes the keyboard must release momentary modifiers.
        if ((released & ControllerPacket.LB_FLAG) != 0) {
            shift.release(now);
        }
        if ((released & ControllerPacket.RB_FLAG) != 0) {
            function.release(now);
        }
        // Closing wins over every keyboard action in the same report.
        if ((pressed & ControllerPacket.B_FLAG) != 0) {
            return pressed;
        }
        if ((pressed & ControllerPacket.LB_FLAG) != 0) {
            shift.press(now, singlePressLock);
        }
        if ((pressed & ControllerPacket.RB_FLAG) != 0) {
            function.press(now, singlePressLock);
        }
        if ((pressed & ControllerPacket.X_FLAG) != 0) {
            activateKey(KeyEvent.KEYCODE_DEL);
        }
        if ((pressed & ControllerPacket.Y_FLAG) != 0) {
            activateKey(KeyEvent.KEYCODE_SPACE);
        }
        return pressed;
    }

    public int getModifiers() {
        return modifiers | (shift.isActive() ? KeyboardPacket.MODIFIER_SHIFT : 0);
    }

    public boolean isFunctionLayer() {
        return function.isActive();
    }

    public void toggleModifier(int modifier) {
        if ((modifier & KeyboardPacket.MODIFIER_SHIFT) != 0) {
            shift.toggleLatch();
        }
        modifiers ^= modifier & ~KeyboardPacket.MODIFIER_SHIFT;
    }

    public void toggleFunctionLayer() {
        function.toggleLatch();
    }

    public void activateKey(int baseKeyCode) {
        // Typing is a chord, not a double-tap gesture, even when its bumper hold was brief.
        shift.markUsedForTyping();
        function.markUsedForTyping();
        listener.onKeyActivated(isFunctionLayer() ? functionKey(baseKeyCode) : baseKeyCode, getModifiers());
    }

    public String getLabel(int baseKeyCode, String normal, String shifted) {
        if (isFunctionLayer()) {
            switch (baseKeyCode) {
                case KeyEvent.KEYCODE_1: return "F1";
                case KeyEvent.KEYCODE_2: return "F2";
                case KeyEvent.KEYCODE_3: return "F3";
                case KeyEvent.KEYCODE_4: return "F4";
                case KeyEvent.KEYCODE_5: return "F5";
                case KeyEvent.KEYCODE_6: return "F6";
                case KeyEvent.KEYCODE_7: return "F7";
                case KeyEvent.KEYCODE_8: return "F8";
                case KeyEvent.KEYCODE_9: return "F9";
                case KeyEvent.KEYCODE_0: return "F10";
                case KeyEvent.KEYCODE_MINUS: return "F11";
                case KeyEvent.KEYCODE_EQUALS: return "F12";
            }
        }
        return (getModifiers() & KeyboardPacket.MODIFIER_SHIFT) != 0 ? shifted : normal;
    }

    public void resetInput() {
        modifiers = 0;
        shift.reset();
        function.reset();
        lastControllerButtons = 0;
    }

    /** Touch and controller locks share a latch; the momentary hold remains independent. */
    private static final class ModifierLayer {
        private static final long DOUBLE_TAP_MS = 300;

        private boolean latched;
        private boolean held;
        private boolean buttonPressed;
        private boolean primeTapOnRelease;
        private boolean hasPreviousTap;
        private long pressTime;
        private long previousTapRelease;

        boolean isActive() {
            return latched || held;
        }

        void toggleLatch() {
            latched = !latched;
            hasPreviousTap = false;
            primeTapOnRelease = false;
        }

        void markUsedForTyping() {
            primeTapOnRelease = false;
            hasPreviousTap = false;
        }

        void press(long now, boolean singlePressLock) {
            buttonPressed = true;
            pressTime = now;
            primeTapOnRelease = false;
            if (singlePressLock || latched) {
                // Unlocking must stay off for the entire press, rather than become a hold.
                latched = !latched;
                held = false;
            }
            else if (hasPreviousTap && now - previousTapRelease >= 0 &&
                    now - previousTapRelease <= DOUBLE_TAP_MS) {
                latched = true;
                held = false;
            }
            else {
                held = true;
                primeTapOnRelease = true;
            }
            hasPreviousTap = false;
        }

        void release(long now) {
            held = false;
            if (!buttonPressed) {
                return;
            }
            long duration = now - pressTime;
            hasPreviousTap = primeTapOnRelease && duration >= 0 && duration <= DOUBLE_TAP_MS;
            previousTapRelease = now;
            primeTapOnRelease = false;
            buttonPressed = false;
        }

        void reset() {
            latched = false;
            held = false;
            buttonPressed = false;
            primeTapOnRelease = false;
            hasPreviousTap = false;
            pressTime = 0;
            previousTapRelease = 0;
        }
    }

    private static int functionKey(int baseKeyCode) {
        // Android places 0 before 1 in its key codes, so keyboard-position arithmetic is unsafe.
        switch (baseKeyCode) {
            case KeyEvent.KEYCODE_1: return KeyEvent.KEYCODE_F1;
            case KeyEvent.KEYCODE_2: return KeyEvent.KEYCODE_F2;
            case KeyEvent.KEYCODE_3: return KeyEvent.KEYCODE_F3;
            case KeyEvent.KEYCODE_4: return KeyEvent.KEYCODE_F4;
            case KeyEvent.KEYCODE_5: return KeyEvent.KEYCODE_F5;
            case KeyEvent.KEYCODE_6: return KeyEvent.KEYCODE_F6;
            case KeyEvent.KEYCODE_7: return KeyEvent.KEYCODE_F7;
            case KeyEvent.KEYCODE_8: return KeyEvent.KEYCODE_F8;
            case KeyEvent.KEYCODE_9: return KeyEvent.KEYCODE_F9;
            case KeyEvent.KEYCODE_0: return KeyEvent.KEYCODE_F10;
            case KeyEvent.KEYCODE_MINUS: return KeyEvent.KEYCODE_F11;
            case KeyEvent.KEYCODE_EQUALS: return KeyEvent.KEYCODE_F12;
            default: return baseKeyCode;
        }
    }
}
