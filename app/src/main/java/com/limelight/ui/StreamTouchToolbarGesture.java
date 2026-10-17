package com.limelight.ui;

/** Resolves the edge handle's tap and horizontal swipe without firing on cancelled drags. */
public final class StreamTouchToolbarGesture {
    public static final int NONE = 0;
    public static final int TAP = 1;
    public static final int SWIPE_RIGHT = 2;
    public static final int SWIPE_LEFT = 3;

    private final float touchSlop;
    private final float swipeThreshold;
    private float startX;
    private float startY;
    private boolean active;
    private boolean movedBeyondTapSlop;

    public StreamTouchToolbarGesture(float touchSlop, float swipeThreshold) {
        if (!(touchSlop > 0) || Float.isNaN(touchSlop) || Float.isInfinite(touchSlop) ||
                !(swipeThreshold >= touchSlop) || Float.isNaN(swipeThreshold) || Float.isInfinite(swipeThreshold)) {
            throw new IllegalArgumentException("Gesture thresholds must be finite and positive");
        }
        this.touchSlop = touchSlop;
        this.swipeThreshold = swipeThreshold;
    }

    public void begin(float x, float y) {
        startX = x;
        startY = y;
        active = true;
        movedBeyondTapSlop = false;
    }

    public void move(float x, float y) {
        if (active && (Math.abs(x - startX) > touchSlop || Math.abs(y - startY) > touchSlop)) {
            movedBeyondTapSlop = true;
        }
    }

    public int finish(float x, float y) {
        if (!active) {
            return NONE;
        }
        move(x, y);
        active = false;
        if (!movedBeyondTapSlop) {
            return TAP;
        }
        float horizontal = x - startX;
        float vertical = y - startY;
        if (Math.abs(horizontal) >= swipeThreshold &&
                Math.abs(horizontal) > Math.abs(vertical) * 1.5f) {
            return horizontal > 0 ? SWIPE_RIGHT : SWIPE_LEFT;
        }
        return NONE;
    }

    public void cancel() {
        active = false;
    }
}
