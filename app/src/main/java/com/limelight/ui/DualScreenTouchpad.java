package com.limelight.ui;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pointer-ID based relative touchpad state, independent of Android for gesture/lifecycle tests. */
public final class DualScreenTouchpad {
    public static final int LEFT_BUTTON = 1;
    public static final int RIGHT_BUTTON = 3;
    private static final long TAP_TIMEOUT_MS = 250;
    private static final long CLICK_DURATION_MS = 100;

    public interface Listener {
        void onMove(float deltaX, float deltaY);
        void onScroll(float deltaY);
        void onButton(int button, boolean pressed);
    }

    public interface Scheduler {
        void postDelayed(Runnable action, long delayMs);
        void removeCallbacks(Runnable action);
    }

    private final Listener listener;
    private final Scheduler scheduler;
    private final float tapSlop;
    private final Map<Integer, Pointer> pointers = new LinkedHashMap<>();
    private final Button left = new Button(LEFT_BUTTON);
    private final Button right = new Button(RIGHT_BUTTON);
    private long gestureStart;
    private int maxPointers;
    private boolean tapEligible;

    public DualScreenTouchpad(float tapSlop, Listener listener, Scheduler scheduler) {
        if (!(tapSlop > 0) || Float.isInfinite(tapSlop) || listener == null || scheduler == null) {
            throw new IllegalArgumentException("Valid touch slop, listener and scheduler are required");
        }
        this.tapSlop = tapSlop;
        this.listener = listener;
        this.scheduler = scheduler;
    }

    public void pointerDown(int id, float x, float y, long time) {
        if (!isFinite(x) || !isFinite(y)) {
            cancelInput();
            return;
        }
        if (pointers.containsKey(id)) {
            cancelInput();
        }
        if (pointers.isEmpty()) {
            gestureStart = time;
            maxPointers = 0;
            tapEligible = true;
        }
        pointers.put(id, new Pointer(x, y));
        maxPointers = Math.max(maxPointers, pointers.size());
        if (maxPointers > 2) {
            tapEligible = false;
        }
    }

    /** Supply every currently tracked pointer, in any order, including the pointer being lifted. */
    public void move(int[] ids, float[] x, float[] y) {
        if (ids.length != x.length || ids.length != y.length) {
            throw new IllegalArgumentException("Pointer coordinates must have matching lengths");
        }
        if (pointers.isEmpty()) {
            return;
        }
        float deltaX = 0;
        float deltaY = 0;
        for (Map.Entry<Integer, Pointer> entry : pointers.entrySet()) {
            int index = indexOf(ids, entry.getKey());
            if (index < 0 || !isFinite(x[index]) || !isFinite(y[index])) {
                cancelInput();
                return;
            }
            Pointer pointer = entry.getValue();
            float dx = x[index] - pointer.x;
            float dy = y[index] - pointer.y;
            pointer.distance += Math.hypot(dx, dy);
            if (Math.abs(x[index] - pointer.originX) > tapSlop ||
                    Math.abs(y[index] - pointer.originY) > tapSlop ||
                    pointer.distance > tapSlop * 1.5f) {
                tapEligible = false;
            }
            deltaX += dx;
            deltaY += dy;
        }
        for (Map.Entry<Integer, Pointer> entry : pointers.entrySet()) {
            int index = indexOf(ids, entry.getKey());
            entry.getValue().x = x[index];
            entry.getValue().y = y[index];
        }
        if (maxPointers == 1 && pointers.size() == 1 && (deltaX != 0 || deltaY != 0)) {
            listener.onMove(deltaX, deltaY);
        }
        else if (maxPointers == 2 && pointers.size() == 2 && !tapEligible && deltaY != 0) {
            listener.onScroll(deltaY / 2);
        }
        // Once a multi-finger gesture begins, lifting one finger must never jump the pointer.
    }

    public void pointerUp(int id, long time) {
        if (pointers.remove(id) == null) {
            cancelInput();
            return;
        }
        if (pointers.isEmpty()) {
            if (tapEligible && time >= gestureStart && time - gestureStart <= TAP_TIMEOUT_MS) {
                click(maxPointers == 2 ? RIGHT_BUTTON : LEFT_BUTTON);
            }
            tapEligible = false;
        }
    }

    /** Explicit mouse buttons can be held with one finger while another moves on the pad. */
    public void setButtonHeld(int button, boolean held) {
        Button state = button(button);
        state.held = held;
        if (held) {
            // Transfer an outstanding tap to the held button without a spurious up event.
            scheduler.removeCallbacks(state.releaseTap);
            state.tapPressed = false;
        }
        state.update();
    }

    public void click(int button) {
        Button state = button(button);
        if (state.held) {
            return;
        }
        scheduler.removeCallbacks(state.releaseTap);
        if (state.tapPressed) {
            state.tapPressed = false;
            state.update();
        }
        state.tapPressed = true;
        state.update();
        scheduler.postDelayed(state.releaseTap, CLICK_DURATION_MS);
    }

    /** Cancel timers and release every button this pad owns, including delayed tap releases. */
    public void cancelInput() {
        pointers.clear();
        tapEligible = false;
        maxPointers = 0;
        left.cancel();
        right.cancel();
    }

    private Button button(int button) {
        if (button == LEFT_BUTTON) return left;
        if (button == RIGHT_BUTTON) return right;
        throw new IllegalArgumentException("Unsupported touchpad button");
    }

    private static int indexOf(int[] ids, int id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) return i;
        }
        return -1;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static final class Pointer {
        final float originX;
        final float originY;
        float x;
        float y;
        float distance;

        Pointer(float x, float y) {
            this.x = originX = x;
            this.y = originY = y;
        }
    }

    private final class Button {
        final int button;
        boolean held;
        boolean tapPressed;
        boolean sentPressed;
        final Runnable releaseTap = () -> {
            tapPressed = false;
            update();
        };

        Button(int button) {
            this.button = button;
        }

        void update() {
            boolean pressed = held || tapPressed;
            if (pressed != sentPressed) {
                listener.onButton(button, pressed);
                sentPressed = pressed;
            }
        }

        void cancel() {
            scheduler.removeCallbacks(releaseTap);
            held = tapPressed = false;
            update();
        }
    }
}
