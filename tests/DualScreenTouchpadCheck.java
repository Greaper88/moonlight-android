import com.limelight.ui.DualScreenTouchpad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DualScreenTouchpadCheck {
    private static int assertions;

    private static void check(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("Touchpad check " + assertions + " failed");
    }

    private static final class Harness implements DualScreenTouchpad.Listener, DualScreenTouchpad.Scheduler {
        final List<String> buttons = new ArrayList<>();
        final Map<Runnable, Long> timers = new LinkedHashMap<>();
        final DualScreenTouchpad pad = new DualScreenTouchpad(8, this, this);
        float movedX;
        float movedY;
        float scrolled;

        @Override
        public void onMove(float x, float y) { movedX += x; movedY += y; }
        @Override
        public void onScroll(float y) { scrolled += y; }
        @Override
        public void onButton(int button, boolean down) { buttons.add(button + (down ? "+" : "-")); }
        @Override
        public void postDelayed(Runnable runnable, long delay) { timers.put(runnable, delay); }
        @Override
        public void removeCallbacks(Runnable runnable) { timers.remove(runnable); }

        void finishTimers() {
            List<Runnable> pending = new ArrayList<>(timers.keySet());
            timers.clear();
            for (Runnable runnable : pending) runnable.run();
        }

        void move(int id, float x, float y) {
            pad.move(new int[] {id}, new float[] {x}, new float[] {y});
        }

        void moveTwo(int first, float x1, float y1, int second, float x2, float y2) {
            pad.move(new int[] {first, second}, new float[] {x1, x2}, new float[] {y1, y2});
        }
    }

    public static void main(String[] args) {
        Harness h = new Harness();
        h.pad.pointerDown(41, 50, 60, 10);
        h.pad.pointerUp(41, 100);
        check(h.buttons.toString().equals("[1+]"));
        check(h.timers.size() == 1 && h.timers.values().iterator().next() == 100);
        h.finishTimers();
        check(h.buttons.toString().equals("[1+, 1-]"));

        h = new Harness();
        h.pad.pointerDown(41, 50, 60, 10);
        h.move(41, 70, 35);
        h.move(41, 65, 40);
        h.pad.pointerUp(41, 100);
        check(h.movedX == 15 && h.movedY == -20);
        check(h.buttons.isEmpty());

        // Android pointer indices reorder; actual pointer IDs determine movement and centroids.
        h = new Harness();
        h.pad.pointerDown(41, 50, 60, 10);
        h.pad.pointerDown(7, 100, 60, 20);
        h.moveTwo(7, 100, 80, 41, 50, 80);
        check(h.movedX == 0 && h.movedY == 0 && h.scrolled == 20);
        h.moveTwo(41, 50, 90, 7, 100, 90);
        check(h.scrolled == 30);
        h.pad.pointerUp(41, 100);
        h.move(7, 200, 200);
        check(h.movedX == 0 && h.movedY == 0 && h.scrolled == 30);
        h.pad.pointerUp(7, 150);
        check(h.buttons.isEmpty());

        h = new Harness();
        h.pad.pointerDown(90, 50, 50, 0);
        h.pad.pointerDown(3, 80, 50, 30);
        h.moveTwo(3, 82, 51, 90, 51, 49);
        h.pad.pointerUp(3, 50);
        check(h.buttons.isEmpty());
        h.pad.pointerUp(90, 70);
        check(h.buttons.toString().equals("[3+]"));
        h.finishTimers();
        check(h.buttons.toString().equals("[3+, 3-]"));

        // Long presses, three-finger gestures, and moving away then returning are never taps.
        h = new Harness();
        h.pad.pointerDown(1, 0, 0, 0);
        h.pad.pointerUp(1, 251);
        check(h.buttons.isEmpty());
        h.pad.pointerDown(1, 0, 0, 300);
        h.pad.pointerDown(2, 10, 0, 310);
        h.pad.pointerDown(3, 20, 0, 320);
        h.pad.move(new int[] {3, 1, 2}, new float[] {20, 0, 10}, new float[] {20, 20, 20});
        h.pad.pointerUp(2, 330);
        h.pad.pointerUp(1, 340);
        h.pad.pointerUp(3, 350);
        check(h.buttons.isEmpty() && h.scrolled == 0 && h.movedY == 0);
        h.pad.pointerDown(1, 0, 0, 400);
        h.move(1, 30, 0);
        h.move(1, 0, 0);
        h.pad.pointerUp(1, 500);
        check(h.buttons.isEmpty());

        // Holding an explicit button supports a drag; taps while held must not release it.
        h = new Harness();
        h.pad.setButtonHeld(DualScreenTouchpad.LEFT_BUTTON, true);
        h.pad.pointerDown(27, 0, 0, 0);
        h.move(27, 30, -30);
        h.pad.pointerUp(27, 100);
        h.pad.click(DualScreenTouchpad.LEFT_BUTTON);
        h.finishTimers();
        check(h.movedX == 30 && h.movedY == -30 && h.buttons.toString().equals("[1+]"));
        h.pad.setButtonHeld(DualScreenTouchpad.LEFT_BUTTON, false);
        check(h.buttons.toString().equals("[1+, 1-]"));

        // Moving from a tap to a held button cancels the delayed release without interrupting drag.
        h = new Harness();
        h.pad.click(DualScreenTouchpad.LEFT_BUTTON);
        h.pad.setButtonHeld(DualScreenTouchpad.LEFT_BUTTON, true);
        check(h.timers.isEmpty() && h.buttons.toString().equals("[1+]"));
        h.finishTimers();
        check(h.buttons.toString().equals("[1+]"));
        h.pad.setButtonHeld(DualScreenTouchpad.LEFT_BUTTON, false);
        check(h.buttons.toString().equals("[1+, 1-]"));

        // Rapid taps produce distinct clicks instead of extending a single down event indefinitely.
        h = new Harness();
        h.pad.click(DualScreenTouchpad.RIGHT_BUTTON);
        h.pad.click(DualScreenTouchpad.RIGHT_BUTTON);
        check(h.buttons.toString().equals("[3+, 3-, 3+]"));
        check(h.timers.size() == 1);
        h.finishTimers();
        check(h.buttons.toString().equals("[3+, 3-, 3+, 3-]"));

        // Canceling immediately releases all locally owned buttons and cancels pending click-ups.
        h = new Harness();
        h.pad.click(DualScreenTouchpad.LEFT_BUTTON);
        h.pad.setButtonHeld(DualScreenTouchpad.RIGHT_BUTTON, true);
        h.pad.pointerDown(5, 0, 0, 0);
        h.pad.cancelInput();
        check(h.timers.isEmpty() && h.buttons.toString().equals("[1+, 3+, 1-, 3-]"));
        h.finishTimers();
        h.move(5, 10, 10);
        h.pad.pointerUp(5, 70);
        h.pad.cancelInput();
        check(h.buttons.size() == 4 && h.movedX == 0 && h.movedY == 0);

        // Lost or malformed pointer data fails closed and releases held input.
        h = new Harness();
        h.pad.setButtonHeld(DualScreenTouchpad.LEFT_BUTTON, true);
        h.pad.pointerDown(11, 0, 0, 0);
        h.move(22, 10, 10);
        h.pad.pointerUp(11, 100);
        check(h.buttons.toString().equals("[1+, 1-]") && h.movedX == 0);
        h.pad.pointerDown(11, Float.NaN, 0, 200);
        h.pad.pointerUp(11, 220);
        check(h.buttons.size() == 2);
        h.pad.pointerDown(11, 0, 0, 300);
        h.move(11, Float.POSITIVE_INFINITY, 10);
        h.pad.pointerUp(11, 400);
        check(h.buttons.size() == 2);

        System.out.println(assertions + " dual-screen touchpad checks passed");
    }
}
