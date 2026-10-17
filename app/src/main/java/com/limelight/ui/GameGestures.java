package com.limelight.ui;

public interface GameGestures {
    void toggleKeyboard();
    boolean isKeyboardVisible();
    void handleKeyboardControllerInput(int buttonFlags);
}
