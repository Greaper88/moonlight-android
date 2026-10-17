package com.limelight.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.KeyboardPacket;

/** An in-stream keyboard that works with touch and controllers without relying on an Android IME. */
public final class StreamKeyboard extends LinearLayout {
    public interface Listener {
        void onKeyActivated(int keyCode, int modifiers);
        void onCloseRequested();
    }

    private static final int NAVIGATION_MASK = ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG |
            ControllerPacket.LEFT_FLAG | ControllerPacket.RIGHT_FLAG;
    private static final long REPEAT_DELAY_MS = 350;
    private static final long REPEAT_INTERVAL_MS = 90;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final Key[][] rows;
    private final TextView[][] buttons;
    private final StreamKeyboardNavigation navigation;
    private int modifiers;
    private int lastControllerButtons;
    private int navigationButtons;

    private final Runnable repeatNavigation = new Runnable() {
        @Override
        public void run() {
            if (getVisibility() != VISIBLE || navigationButtons == 0) {
                return;
            }
            moveSelection();
            handler.postDelayed(this, REPEAT_INTERVAL_MS);
        }
    };

    public StreamKeyboard(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        boolean compact = getResources().getConfiguration().screenWidthDp < 480 ||
                getResources().getConfiguration().screenHeightDp < 300;
        setOrientation(VERTICAL);
        setPadding(dp(5), dp(4), dp(5), dp(5));
        setBackgroundColor(Color.argb(245, 24, 27, 32));
        setFocusable(false);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        TextView hint = new TextView(context);
        hint.setText("Grimm keyboard  ·  D-pad: move  ·  A: type  ·  B: close");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(compact ? 10 : 12);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 0, 0, dp(3));
        addView(hint, new LayoutParams(LayoutParams.MATCH_PARENT, dp(compact ? 19 : 23)));

        rows = new Key[][] {
                {key("Esc", KeyEvent.KEYCODE_ESCAPE), key("1", "!", KeyEvent.KEYCODE_1),
                        key("2", "@", KeyEvent.KEYCODE_2), key("3", "#", KeyEvent.KEYCODE_3),
                        key("4", "$", KeyEvent.KEYCODE_4), key("5", "%", KeyEvent.KEYCODE_5),
                        key("6", "^", KeyEvent.KEYCODE_6), key("7", "&", KeyEvent.KEYCODE_7),
                        key("8", "*", KeyEvent.KEYCODE_8), key("9", "(", KeyEvent.KEYCODE_9),
                        key("0", ")", KeyEvent.KEYCODE_0), key("-", "_", KeyEvent.KEYCODE_MINUS),
                        key("=", "+", KeyEvent.KEYCODE_EQUALS), wide("⌫", KeyEvent.KEYCODE_DEL, 1.5f)},
                {wide("Tab", KeyEvent.KEYCODE_TAB, 1.2f), letter("q", KeyEvent.KEYCODE_Q),
                        letter("w", KeyEvent.KEYCODE_W), letter("e", KeyEvent.KEYCODE_E),
                        letter("r", KeyEvent.KEYCODE_R), letter("t", KeyEvent.KEYCODE_T),
                        letter("y", KeyEvent.KEYCODE_Y), letter("u", KeyEvent.KEYCODE_U),
                        letter("i", KeyEvent.KEYCODE_I), letter("o", KeyEvent.KEYCODE_O),
                        letter("p", KeyEvent.KEYCODE_P), key("[", "{", KeyEvent.KEYCODE_LEFT_BRACKET),
                        key("]", "}", KeyEvent.KEYCODE_RIGHT_BRACKET), key("\\", "|", KeyEvent.KEYCODE_BACKSLASH)},
                {modifier("Ctrl", KeyboardPacket.MODIFIER_CTRL), letter("a", KeyEvent.KEYCODE_A),
                        letter("s", KeyEvent.KEYCODE_S), letter("d", KeyEvent.KEYCODE_D),
                        letter("f", KeyEvent.KEYCODE_F), letter("g", KeyEvent.KEYCODE_G),
                        letter("h", KeyEvent.KEYCODE_H), letter("j", KeyEvent.KEYCODE_J),
                        letter("k", KeyEvent.KEYCODE_K), letter("l", KeyEvent.KEYCODE_L),
                        key(";", ":", KeyEvent.KEYCODE_SEMICOLON), key("'", "\"", KeyEvent.KEYCODE_APOSTROPHE),
                        wide("Enter", KeyEvent.KEYCODE_ENTER, 1.6f)},
                {modifier("Shift", KeyboardPacket.MODIFIER_SHIFT), letter("z", KeyEvent.KEYCODE_Z),
                        letter("x", KeyEvent.KEYCODE_X), letter("c", KeyEvent.KEYCODE_C),
                        letter("v", KeyEvent.KEYCODE_V), letter("b", KeyEvent.KEYCODE_B),
                        letter("n", KeyEvent.KEYCODE_N), letter("m", KeyEvent.KEYCODE_M),
                        key(",", "<", KeyEvent.KEYCODE_COMMA), key(".", ">", KeyEvent.KEYCODE_PERIOD),
                        key("/", "?", KeyEvent.KEYCODE_SLASH), key("↑", KeyEvent.KEYCODE_DPAD_UP)},
                {modifier("Alt", KeyboardPacket.MODIFIER_ALT), key("`", "~", KeyEvent.KEYCODE_GRAVE),
                        wide("Space", KeyEvent.KEYCODE_SPACE, 5), key("←", KeyEvent.KEYCODE_DPAD_LEFT),
                        key("↓", KeyEvent.KEYCODE_DPAD_DOWN), key("→", KeyEvent.KEYCODE_DPAD_RIGHT),
                        wide("Hide", KeyEvent.KEYCODE_UNKNOWN, 1.5f)}
        };
        float[][] weights = new float[rows.length][];
        buttons = new TextView[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            weights[r] = new float[rows[r].length];
            buttons[r] = new TextView[rows[r].length];
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, dp(compact ? 30 : 36)));
            for (int c = 0; c < rows[r].length; c++) {
                Key key = rows[r][c];
                weights[r][c] = key.weight;
                TextView button = new TextView(context);
                button.setTextColor(Color.WHITE);
                button.setTextSize(compact ? 12 : 14);
                button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                button.setGravity(Gravity.CENTER);
                button.setSingleLine();
                button.setPadding(dp(1), 0, dp(1), 0);
                // Keep the stream focused, so controller events continue through Game's input handler.
                button.setFocusable(false);
                button.setClickable(true);
                final int selectedRow = r;
                final int selectedColumn = c;
                button.setOnClickListener(v -> selectAndActivate(selectedRow, selectedColumn));
                LayoutParams layout = new LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight);
                layout.setMargins(dp(1), dp(1), dp(1), dp(1));
                row.addView(button, layout);
                buttons[r][c] = button;
            }
        }
        navigation = new StreamKeyboardNavigation(weights);
        navigation.select(2, 1);
        refreshKeys();
    }

    public void handleControllerInput(int buttonFlags) {
        int pressed = buttonFlags & ~lastControllerButtons;
        int previousNavigation = navigationButtons;
        lastControllerButtons = buttonFlags;
        navigationButtons = buttonFlags & NAVIGATION_MASK;

        if ((pressed & ControllerPacket.B_FLAG) != 0) {
            listener.onCloseRequested();
            return;
        }
        if (navigationButtons != previousNavigation) {
            handler.removeCallbacks(repeatNavigation);
            if (navigationButtons != 0) {
                moveSelection();
                handler.postDelayed(repeatNavigation, REPEAT_DELAY_MS);
            }
        }
        if ((pressed & ControllerPacket.A_FLAG) != 0) {
            activateSelection();
        }
    }

    public void resetInput() {
        handler.removeCallbacks(repeatNavigation);
        lastControllerButtons = 0;
        navigationButtons = 0;
        modifiers = 0;
        refreshKeys();
    }

    private void moveSelection() {
        int horizontal = ((navigationButtons & ControllerPacket.RIGHT_FLAG) != 0 ? 1 : 0) -
                ((navigationButtons & ControllerPacket.LEFT_FLAG) != 0 ? 1 : 0);
        int vertical = ((navigationButtons & ControllerPacket.DOWN_FLAG) != 0 ? 1 : 0) -
                ((navigationButtons & ControllerPacket.UP_FLAG) != 0 ? 1 : 0);
        navigation.move(horizontal, vertical);
        refreshKeys();
    }

    private void activateSelection() {
        Key key = rows[navigation.getRow()][navigation.getColumn()];
        if (key.modifier != 0) {
            modifiers ^= key.modifier;
        }
        else if (key.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            listener.onCloseRequested();
            return;
        }
        else {
            listener.onKeyActivated(key.keyCode, modifiers);
        }
        refreshKeys();
    }

    private void selectAndActivate(int row, int column) {
        navigation.select(row, column);
        activateSelection();
    }

    private void refreshKeys() {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                Key key = rows[r][c];
                TextView button = buttons[r][c];
                boolean selected = r == navigation.getRow() && c == navigation.getColumn();
                boolean active = key.modifier != 0 && (modifiers & key.modifier) != 0;
                String label = (modifiers & KeyboardPacket.MODIFIER_SHIFT) != 0 ? key.shiftedLabel : key.label;
                button.setText(active ? label + " •" : label);
                button.setContentDescription(("⌫".equals(label) ? "Backspace" : label) + (active ? " on" : ""));
                int color = selected ? Color.rgb(35, 108, 170) :
                        active ? Color.rgb(43, 100, 65) : Color.rgb(49, 54, 62);
                StateListDrawable background = new StateListDrawable();
                background.addState(new int[] {android.R.attr.state_pressed}, keyBackground(Color.rgb(63, 131, 183), true));
                background.addState(new int[0], keyBackground(color, selected));
                button.setBackground(background);
            }
        }
    }

    private GradientDrawable keyBackground(int color, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(4));
        drawable.setStroke(dp(1), selected ? Color.WHITE : Color.rgb(87, 94, 104));
        return drawable;
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(repeatNavigation);
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static Key key(String label, int keyCode) {
        return new Key(label, label, keyCode, 0, 1);
    }

    private static Key key(String label, String shiftedLabel, int keyCode) {
        return new Key(label, shiftedLabel, keyCode, 0, 1);
    }

    private static Key letter(String label, int keyCode) {
        return key(label, label.toUpperCase(java.util.Locale.ROOT), keyCode);
    }

    private static Key wide(String label, int keyCode, float weight) {
        return new Key(label, label, keyCode, 0, weight);
    }

    private static Key modifier(String label, int mask) {
        return new Key(label, label, KeyEvent.KEYCODE_UNKNOWN, mask, 1.5f);
    }

    private static final class Key {
        final String label;
        final String shiftedLabel;
        final int keyCode;
        final int modifier;
        final float weight;

        Key(String label, String shiftedLabel, int keyCode, int modifier, float weight) {
            this.label = label;
            this.shiftedLabel = shiftedLabel;
            this.keyCode = keyCode;
            this.modifier = modifier;
            this.weight = weight;
        }
    }
}
