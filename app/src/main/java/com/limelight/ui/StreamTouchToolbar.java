package com.limelight.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.R;

import java.util.Collections;

/** Optional in-stream edge controls that leave the stream and its keyboard in the same window. */
public final class StreamTouchToolbar extends LinearLayout {
    private final Runnable toggleKeyboard;
    private final TextView handle;
    private final LinearLayout panel;
    private final TextView keyboardButton;
    private final StreamTouchToolbarGesture gesture;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean keyboardVisible;
    private boolean disposed;

    /** Add at the safe left edge with WRAP_CONTENT sizing and keep above the streaming keyboard. */
    public StreamTouchToolbar(Context context, Runnable toggleKeyboard) {
        super(context);
        if (toggleKeyboard == null) {
            throw new IllegalArgumentException("A keyboard callback is required");
        }
        this.toggleKeyboard = toggleKeyboard;
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gesture = new StreamTouchToolbarGesture(Math.max(1, touchSlop), Math.max(dp(32), touchSlop));
        setOrientation(HORIZONTAL);
        // This physical left-edge control opens toward the stream in every UI language.
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(false);
        // Do not make the outer layout clickable: its transparent corners should reach the stream.

        handle = new ToolbarButton(context);
        handle.setTextSize(30);
        handle.setPadding(0, 0, 0, 0);
        handle.setBackground(buttonBackground(Color.argb(225, 31, 36, 44)));
        handle.setOnTouchListener(this::handleTouch);
        addView(handle, new LayoutParams(dp(48), dp(64)));

        panel = new LinearLayout(context);
        panel.setOrientation(VERTICAL);
        panel.setPadding(dp(5), dp(5), dp(5), dp(5));
        panel.setBackground(background(Color.argb(245, 24, 27, 32)));
        panel.setClickable(true);
        panel.setFocusable(false);
        LayoutParams panelLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        panelLayout.leftMargin = dp(4);
        addView(panel, panelLayout);

        handle.setOnClickListener(v -> {
            if (!disposed) {
                if (panel.getVisibility() == VISIBLE) {
                    collapse();
                }
                else {
                    expand();
                }
            }
        });

        keyboardButton = actionButton(context);
        keyboardButton.setOnClickListener(v -> {
            if (!disposed) {
                collapse();
                this.toggleKeyboard.run();
            }
        });
        panel.addView(keyboardButton, new LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)));

        TextView collapseButton = actionButton(context);
        collapseButton.setText(R.string.grimm_touch_toolbar_hide_controls);
        collapseButton.setContentDescription(getResources().getString(R.string.grimm_touch_toolbar_hide_controls_description));
        collapseButton.setOnClickListener(v -> collapse());
        LayoutParams collapseLayout = new LayoutParams(LayoutParams.MATCH_PARENT, dp(48));
        collapseLayout.topMargin = dp(4);
        panel.addView(collapseButton, collapseLayout);

        setKeyboardVisible(false);
        collapse();
    }

    public void setKeyboardVisible(boolean visible) {
        keyboardVisible = visible;
        keyboardButton.setText(visible ? R.string.grimm_touch_toolbar_hide_keyboard :
                R.string.grimm_touch_toolbar_show_keyboard);
        keyboardButton.setContentDescription(getResources().getString(visible ?
                R.string.grimm_touch_toolbar_hide_keyboard_description :
                R.string.grimm_touch_toolbar_show_keyboard_description));
        updateHandle();
    }

    /** Close the pull-out panel while leaving the edge handle available. */
    public void collapse() {
        cancelGesture();
        panel.setVisibility(GONE);
        updateHandle();
    }

    /** Stop accepting actions when the stream ends. This view does not own timers or a window. */
    public void dispose() {
        disposed = true;
        collapse();
        clearGestureExclusion();
    }

    private void expand() {
        if (disposed) {
            return;
        }
        panel.setVisibility(VISIBLE);
        updateHandle();
    }

    private void updateHandle() {
        boolean expanded = panel.getVisibility() == VISIBLE;
        handle.setText(expanded ? "‹" : "›");
        int description = expanded ? R.string.grimm_touch_toolbar_hide_controls_description :
                keyboardVisible ? R.string.grimm_touch_toolbar_open_controls_keyboard_visible :
                R.string.grimm_touch_toolbar_open_controls;
        handle.setContentDescription(getResources().getString(description));
    }

    private boolean handleTouch(View view, MotionEvent event) {
        if (disposed) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                gesture.begin(event.getX(), event.getY());
                handle.setPressed(true);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    cancelGesture();
                }
                else {
                    gesture.move(event.getX(pointerIndex), event.getY(pointerIndex));
                    handle.setPressed(event.getX(pointerIndex) >= 0 &&
                            event.getX(pointerIndex) < handle.getWidth() &&
                            event.getY(pointerIndex) >= 0 && event.getY(pointerIndex) < handle.getHeight());
                }
                break;
            case MotionEvent.ACTION_UP:
                int result = activePointerId == event.getPointerId(0) ?
                        gesture.finish(event.getX(), event.getY()) : StreamTouchToolbarGesture.NONE;
                cancelGesture();
                if (result == StreamTouchToolbarGesture.TAP) {
                    view.performClick();
                }
                else if (result == StreamTouchToolbarGesture.SWIPE_RIGHT) {
                    expand();
                }
                else if (result == StreamTouchToolbarGesture.SWIPE_LEFT) {
                    collapse();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // Multiple fingers must not turn an interrupted drag into an accidental toggle.
                cancelGesture();
                break;
            default:
                break;
        }
        return true;
    }

    private void cancelGesture() {
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        gesture.cancel();
        handle.setPressed(false);
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !disposed) {
            // Exclude only the 48x64dp handle, leaving Android Back available along the rest of the edge.
            handle.setSystemGestureExclusionRects(Collections.singletonList(
                    new Rect(0, 0, handle.getWidth(), handle.getHeight())));
        }
    }

    private void clearGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handle.setSystemGestureExclusionRects(Collections.emptyList());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelGesture();
        clearGestureExclusion();
        super.onDetachedFromWindow();
    }

    private TextView actionButton(Context context) {
        TextView button = new ToolbarButton(context);
        button.setTextSize(14);
        button.setMinWidth(dp(136));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(buttonBackground(Color.rgb(49, 54, 62)));
        return button;
    }

    private StateListDrawable buttonBackground(int color) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[] {android.R.attr.state_pressed}, background(Color.rgb(35, 108, 170)));
        drawable.addState(new int[0], background(color));
        return drawable;
    }

    private GradientDrawable background(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), Color.rgb(87, 94, 104));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class ToolbarButton extends TextView {
        ToolbarButton(Context context) {
            super(context);
            setGravity(Gravity.CENTER);
            setTextColor(Color.WHITE);
            setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            setSingleLine();
            setFocusable(false);
            setClickable(true);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        @Override
        public CharSequence getAccessibilityClassName() {
            return Button.class.getName();
        }
    }
}
