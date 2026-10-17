package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/** Defers an unmodified Select press so a keyboard hold never reaches the game. */
public final class SelectShortcutState {
    public static final long HOLD_MILLIS = 750;
    public static final long TAP_MILLIS = 25;

    private boolean down;
    private boolean pending;
    private boolean consumed;
    private boolean toggleRequested;
    private long downAt;
    private long tapUntil;

    public int update(int buttons, long now) {
        boolean selectDown = (buttons & ControllerPacket.BACK_FLAG) != 0;
        int otherButtons = buttons & ~ControllerPacket.BACK_FLAG;
        if (selectDown && !down) {
            pending = otherButtons == 0;
            consumed = false;
            downAt = now;
        }
        if (selectDown && pending && otherButtons != 0) {
            // Preserve game shortcuts/chords instead of opening the keyboard.
            pending = false;
        }
        if (pending && now - downAt >= HOLD_MILLIS && otherButtons == 0) {
            toggleRequested = true;
            pending = false;
            consumed = true;
        }
        if (!selectDown && down) {
            if (pending && otherButtons == 0) {
                tapUntil = now + TAP_MILLIS;
            }
            pending = false;
            consumed = false;
        }
        down = selectDown;
        int forwarded = pending || consumed ? buttons & ~ControllerPacket.BACK_FLAG : buttons;
        return now < tapUntil ? forwarded | ControllerPacket.BACK_FLAG : forwarded;
    }

    public boolean takeToggleRequest() {
        boolean requested = toggleRequested;
        toggleRequested = false;
        return requested;
    }

    public long nextDeadline() {
        return pending ? downAt + HOLD_MILLIS : tapUntil;
    }

    public void suppressUntilRelease() {
        pending = toggleRequested = false;
        consumed = down;
        tapUntil = 0;
    }

    public void reset() {
        down = pending = consumed = toggleRequested = false;
        downAt = tapUntil = 0;
    }
}
