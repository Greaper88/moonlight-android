package com.limelight.ui;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.nvstream.NvConnection;
import com.limelight.preferences.PreferenceConfiguration;

/** Local controls on another display, sharing the existing streaming connection and input owner. */
public final class DualScreenControlsPresentation extends Presentation {
    private final NvConnection connection;
    private final PreferenceConfiguration preferences;
    private final StreamKeyboard.Listener keyListener;
    private final Runnable toggleKeyboard;
    private final Runnable statsVisibilityChanged;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private StreamKeyboard keyboard;
    private KeyboardContainer keyboardContainer;
    private TextView keyboardButton;
    private TextView statsButton;
    private TextView statsTextView;
    private ScrollView statsPanel;
    private TextView touchpadView;
    private LinearLayout mouseButtons;
    private MouseButtonView leftButton;
    private MouseButtonView rightButton;
    private DualScreenTouchpad touchpad;
    private boolean keyboardVisible;
    private boolean statsVisible;
    private boolean disposed;
    private String statsText;
    private float pendingX;
    private float pendingY;
    private float pendingScroll;
    private int controlsHeight;

    public DualScreenControlsPresentation(Activity owner, Display display, NvConnection connection,
                                          PreferenceConfiguration preferences,
                                          StreamKeyboard.Listener keyListener, Runnable toggleKeyboard,
                                          Runnable statsVisibilityChanged) {
        super(owner, display, android.R.style.Theme_Material_NoActionBar);
        if (connection == null || preferences == null || keyListener == null || toggleKeyboard == null) {
            throw new IllegalArgumentException("Connection, preferences and keyboard callbacks are required");
        }
        this.connection = connection;
        this.preferences = preferences;
        this.keyListener = keyListener;
        this.toggleKeyboard = toggleKeyboard;
        this.statsVisibilityChanged = statsVisibilityChanged;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0;
            window.setAttributes(attributes);
            window.setBackgroundDrawable(new ColorDrawable(Color.rgb(17, 20, 25)));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // A Presentation's context contains the target display's density and configuration.
        Context context = getContext();
        controlsHeight = context.getResources().getDisplayMetrics().heightPixels;
        LinearLayout root = new LinearLayout(context) {
            @Override
            protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                super.onSizeChanged(width, height, oldWidth, oldHeight);
                if (height != oldHeight) {
                    cancelInput();
                    controlsHeight = height;
                    updateControlsLayout();
                }
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFocusable(false);
        root.setBackgroundColor(Color.rgb(17, 20, 25));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(8), 0);
        statsButton = button(context);
        statsButton.setOnClickListener(view -> {
            if (disposed || !isShowing()) return;
            // Release a drag or delayed tap before the same space becomes local-only content.
            cancelInput();
            statsVisible = !statsVisible;
            updateStatsVisibility();
            if (statsVisibilityChanged != null) statsVisibilityChanged.run();
        });
        header.addView(statsButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)));
        TextView title = text(context, 16);
        title.setText(R.string.grimm_dual_screen_controls_title);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(dp(8), 0, dp(8), 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        keyboardButton = button(context);
        keyboardButton.setOnClickListener(view -> {
            if (!disposed && isShowing()) this.toggleKeyboard.run();
        });
        header.addView(keyboardButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)));
        root.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        touchpad = new DualScreenTouchpad(Math.max(1,
                ViewConfiguration.get(context).getScaledTouchSlop()), new DualScreenTouchpad.Listener() {
            @Override
            public void onMove(float deltaX, float deltaY) {
                sendMove(deltaX, deltaY);
            }

            @Override
            public void onScroll(float deltaY) {
                pendingScroll += deltaY / getContext().getResources().getDisplayMetrics().density * 5;
                short amount = boundedShort(pendingScroll);
                if (amount != 0) {
                    connection.sendMouseHighResScroll(amount);
                    pendingScroll -= amount;
                }
            }

            @Override
            public void onButton(int button, boolean pressed) {
                if (pressed) connection.sendMouseButtonDown((byte) button);
                else connection.sendMouseButtonUp((byte) button);
            }
        }, new DualScreenTouchpad.Scheduler() {
            @Override
            public void postDelayed(Runnable action, long delayMs) {
                handler.postDelayed(action, delayMs);
            }

            @Override
            public void removeCallbacks(Runnable action) {
                handler.removeCallbacks(action);
            }
        });

        touchpadView = text(context, 14);
        touchpadView.setText(getContext().getString(R.string.grimm_dual_screen_touchpad_title) +
                "\n\n" + getContext().getString(R.string.grimm_dual_screen_touchpad_hint));
        touchpadView.setContentDescription(getContext().getString(R.string.grimm_dual_screen_touchpad_description));
        touchpadView.setPadding(dp(12), dp(8), dp(12), dp(8));
        touchpadView.setBackground(background(Color.rgb(27, 32, 39)));
        touchpadView.setOnTouchListener(this::onTouchpadEvent);
        FrameLayout contentArea = new FrameLayout(context);
        contentArea.setFocusable(false);
        LinearLayout.LayoutParams padLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        padLayout.setMargins(dp(8), dp(4), dp(8), dp(4));
        contentArea.addView(touchpadView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // This panel handles its own scrolling. It never forwards touches to the host touchpad,
        // and cannot take controller or keyboard focus from the streaming Activity.
        statsPanel = new ScrollView(context);
        statsPanel.setFocusable(false);
        statsPanel.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        statsPanel.setFillViewport(true);
        statsPanel.setBackground(background(Color.rgb(27, 32, 39)));
        statsTextView = text(context, 14);
        statsTextView.setGravity(Gravity.TOP | Gravity.START);
        statsTextView.setPadding(dp(12), dp(10), dp(12), dp(10));
        statsTextView.setTextIsSelectable(false);
        statsPanel.addView(statsTextView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        contentArea.addView(statsPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(contentArea, padLayout);

        mouseButtons = new LinearLayout(context);
        mouseButtons.setOrientation(LinearLayout.HORIZONTAL);
        mouseButtons.setPadding(dp(6), 0, dp(6), dp(4));
        leftButton = new MouseButtonView(context, DualScreenTouchpad.LEFT_BUTTON);
        leftButton.setText(R.string.grimm_dual_screen_left_click);
        leftButton.setContentDescription(getContext().getString(R.string.grimm_dual_screen_left_click_description));
        rightButton = new MouseButtonView(context, DualScreenTouchpad.RIGHT_BUTTON);
        rightButton.setText(R.string.grimm_dual_screen_right_click);
        rightButton.setContentDescription(getContext().getString(R.string.grimm_dual_screen_right_click_description));
        for (MouseButtonView button : new MouseButtonView[] {leftButton, rightButton}) {
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, dp(44), 1);
            layout.setMargins(dp(2), 0, dp(2), 0);
            mouseButtons.addView(button, layout);
        }
        root.addView(mouseButtons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        keyboard = new StreamKeyboard(context, keyListener, preferences.oskBumperSinglePressLock);
        keyboardContainer = new KeyboardContainer(context);
        keyboardContainer.addView(keyboard, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(keyboardContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        updateKeyboardVisibility();
        setStatsText(statsText);
        updateStatsVisibility();
    }

    /** Available after show() creates the presentation; controller input stays with the owner Activity. */
    public StreamKeyboard getKeyboard() {
        return keyboard;
    }

    public boolean isKeyboardVisible() {
        return keyboardVisible;
    }

    public boolean isStatsVisible() {
        return statsVisible;
    }

    /** Update the local readout without changing its visibility or scroll position. */
    public void setStatsText(String text) {
        if (disposed) return;
        statsText = text;
        if (statsTextView != null) {
            CharSequence displayText = text == null || text.isEmpty() ?
                    getContext().getString(R.string.grimm_dual_screen_stats_loading) : text;
            if (!TextUtils.equals(statsTextView.getText(), displayText)) {
                statsTextView.setText(displayText);
            }
        }
    }

    public void setKeyboardVisible(boolean visible) {
        if (disposed) return;
        if (keyboardVisible != visible) cancelInput();
        keyboardVisible = visible;
        updateKeyboardVisibility();
    }

    private void updateKeyboardVisibility() {
        if (keyboardContainer == null) return;
        keyboardContainer.setVisibility(keyboardVisible ? View.VISIBLE : View.GONE);
        keyboard.setVisibility(keyboardVisible ? View.VISIBLE : View.GONE);
        keyboardButton.setText(keyboardVisible ? R.string.grimm_dual_screen_hide_keyboard :
                R.string.grimm_dual_screen_show_keyboard);
        updateControlsLayout();
    }

    private void updateStatsVisibility() {
        if (statsPanel == null || touchpadView == null) return;
        statsPanel.setVisibility(statsVisible ? View.VISIBLE : View.GONE);
        touchpadView.setVisibility(statsVisible ? View.GONE : View.VISIBLE);
        statsButton.setText(statsVisible ? R.string.grimm_dual_screen_hide_stats :
                R.string.grimm_dual_screen_show_stats);
        updateControlsLayout();
    }

    private void updateControlsLayout() {
        if (keyboardContainer == null || mouseButtons == null) return;
        // On shorter displays the pad's tap gestures remain available, while hiding this row
        // gives the complete keyboard usable keys instead of shrinking it to a narrow strip.
        boolean showMouseButtons = !statsVisible && (!keyboardVisible || controlsHeight >= dp(360));
        mouseButtons.setVisibility(showMouseButtons ? View.VISIBLE : View.GONE);
        touchpadView.setText(!showMouseButtons ?
                getContext().getString(R.string.grimm_dual_screen_touchpad_title) :
                getContext().getString(R.string.grimm_dual_screen_touchpad_title) + "\n\n" +
                        getContext().getString(R.string.grimm_dual_screen_touchpad_hint));
        int available = Math.max(1, controlsHeight - dp(52) - (showMouseButtons ? dp(48) : 0));
        int reservedPad = Math.min(dp(statsVisible ? 120 : 96), Math.round(available * 0.25f));
        keyboardContainer.setMaximumHeight(Math.max(1,
                Math.min(Math.round(controlsHeight * 0.7f), available - reservedPad)));
    }

    /** Call before pausing, replacing the controls display, or ending a stream. */
    public void cancelInput() {
        if (touchpad != null) touchpad.cancelInput();
        if (leftButton != null) leftButton.resetTouch();
        if (rightButton != null) rightButton.resetTouch();
        if (keyboard != null) keyboard.resetInput();
        pendingX = pendingY = pendingScroll = 0;
    }

    public void dispose() {
        disposed = true;
        cancelInput();
        dismiss();
    }

    @Override
    public void dismiss() {
        // Android can dismiss a Presentation itself when its display is removed.
        cancelInput();
        super.dismiss();
    }

    @Override
    protected void onStop() {
        cancelInput();
        super.onStop();
    }

    private boolean onTouchpadEvent(View view, MotionEvent event) {
        if (disposed || !isShowing() || statsVisible) return true;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN) {
            touchpad.pointerDown(event.getPointerId(index), event.getX(index), event.getY(index), event.getEventTime());
            view.getParent().requestDisallowInterceptTouchEvent(true);
        }
        else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE ||
                ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) &&
                        (event.getFlags() & MotionEvent.FLAG_CANCELED) != 0)) {
            cancelInput();
            view.getParent().requestDisallowInterceptTouchEvent(false);
        }
        else {
            int count = event.getPointerCount();
            int[] ids = new int[count];
            float[] x = new float[count];
            float[] y = new float[count];
            for (int i = 0; i < count; i++) {
                ids[i] = event.getPointerId(i);
                x[i] = event.getX(i);
                y[i] = event.getY(i);
            }
            touchpad.move(ids, x, y);
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                touchpad.pointerDown(event.getPointerId(index), event.getX(index), event.getY(index), event.getEventTime());
            }
            else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                touchpad.pointerUp(event.getPointerId(index), event.getEventTime());
                if (action == MotionEvent.ACTION_UP) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
        }
        return true;
    }

    private void sendMove(float deltaX, float deltaY) {
        // Keep sensitivity steady when showing the keyboard changes the pad's height.
        float factor = 1280f / Math.max(1, touchpadView.getWidth());
        pendingX += deltaX * factor;
        pendingY += deltaY * factor;
        short x = boundedShort(pendingX);
        short y = boundedShort(pendingY);
        if (x != 0 || y != 0) {
            if (preferences.absoluteMouseMode) {
                connection.sendMouseMoveAsMousePosition(x, y, (short) 1280, (short) 720);
            }
            else {
                connection.sendMouseMove(x, y);
            }
            pendingX -= x;
            pendingY -= y;
        }
    }

    private static short boundedShort(float value) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value)));
    }

    private TextView text(Context context, int size) {
        TextView view = new TextView(context);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        view.setFocusable(false);
        return view;
    }

    private TextView button(Context context) {
        TextView view = text(context, 14);
        styleButton(view);
        return view;
    }

    private void styleButton(TextView view) {
        view.setTextColor(Color.WHITE);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setFocusable(false);
        view.setClickable(true);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {android.R.attr.state_pressed}, background(Color.rgb(35, 108, 170)));
        background.addState(new int[0], background(Color.rgb(49, 54, 62)));
        view.setBackground(background);
    }

    private GradientDrawable background(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    private final class MouseButtonView extends TextView {
        private final int mouseButton;
        private int activePointer = MotionEvent.INVALID_POINTER_ID;
        private boolean completingTouch;

        MouseButtonView(Context context, int mouseButton) {
            super(context);
            this.mouseButton = mouseButton;
            styleButton(this);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (disposed || !isShowing() || statsVisible) return true;
            if ((event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                touchpad.setButtonHeld(mouseButton, false);
                resetTouch();
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    activePointer = event.getPointerId(0);
                    setPressed(true);
                    touchpad.setButtonHeld(mouseButton, true);
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    if (event.getPointerId(event.getActionIndex()) == activePointer) {
                        touchpad.setButtonHeld(mouseButton, false);
                        resetTouch();
                        completingTouch = true;
                        performClick();
                        completingTouch = false;
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    touchpad.setButtonHeld(mouseButton, false);
                    resetTouch();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            // Accessibility clicks have no down/up touch events, so generate a full tap here.
            if (!completingTouch && !disposed && !statsVisible && isShowing()) touchpad.click(mouseButton);
            return true;
        }

        void resetTouch() {
            activePointer = MotionEvent.INVALID_POINTER_ID;
            setPressed(false);
        }
    }

    /** Fit the complete existing keyboard while reserving space for the touchpad on small displays. */
    private final class KeyboardContainer extends FrameLayout {
        private int maximumHeight;

        KeyboardContainer(Context context) {
            super(context);
            maximumHeight = Math.round(context.getResources().getDisplayMetrics().heightPixels * 0.6f);
            setClipChildren(true);
            setFocusable(false);
        }

        void setMaximumHeight(int height) {
            if (maximumHeight != height) {
                maximumHeight = height;
                requestLayout();
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            keyboard.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int naturalHeight = keyboard.getMeasuredHeight();
            float scale = Math.min(1, maximumHeight / (float) Math.max(1, naturalHeight));
            keyboard.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width / scale), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(naturalHeight, View.MeasureSpec.EXACTLY));
            keyboard.setPivotX(0);
            keyboard.setPivotY(0);
            keyboard.setScaleX(scale);
            keyboard.setScaleY(scale);
            setMeasuredDimension(width, Math.round(naturalHeight * scale));
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            keyboard.layout(0, 0, keyboard.getMeasuredWidth(), keyboard.getMeasuredHeight());
        }
    }
}
