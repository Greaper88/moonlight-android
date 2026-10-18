package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/** Splits keyboard-owned digital controls from the analog controls retained by mouse mode. */
public final class ControllerMouseInput {
    private ControllerMouseInput() {}

    public static int buttonsForMouse(int buttons, boolean keyboardVisible) {
        return keyboardVisible ? 0 : buttons;
    }

    public static boolean scrollUp(int buttons, byte leftTrigger, boolean keyboardVisible) {
        return (!keyboardVisible && (buttons & ControllerPacket.LB_FLAG) != 0) ||
                HeldMouseScroll.isTriggerPressed(leftTrigger);
    }

    public static boolean scrollDown(int buttons, byte rightTrigger, boolean keyboardVisible) {
        return (!keyboardVisible && (buttons & ControllerPacket.RB_FLAG) != 0) ||
                HeldMouseScroll.isTriggerPressed(rightTrigger);
    }
}
