import com.limelight.ui.StreamTouchToolbarGesture;

public final class StreamTouchToolbarGestureCheck {
    private static int assertions;

    private static void check(boolean result) {
        assertions++;
        if (!result) throw new AssertionError("Gesture check " + assertions + " failed");
    }

    public static void main(String[] args) {
        StreamTouchToolbarGesture gesture = new StreamTouchToolbarGesture(8, 32);
        check(gesture.finish(0, 0) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        check(gesture.finish(12, 32) == StreamTouchToolbarGesture.TAP);
        gesture.begin(12, 32);
        check(gesture.finish(19, 39) == StreamTouchToolbarGesture.TAP);
        gesture.begin(12, 32);
        check(gesture.finish(60, 32) == StreamTouchToolbarGesture.SWIPE_RIGHT);
        gesture.begin(12, 32);
        check(gesture.finish(-24, 38) == StreamTouchToolbarGesture.SWIPE_LEFT);
        gesture.begin(12, 32);
        check(gesture.finish(20, 80) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        check(gesture.finish(52, 72) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        check(gesture.finish(35, 32) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        gesture.move(50, 32);
        check(gesture.finish(12, 32) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        gesture.cancel();
        check(gesture.finish(60, 32) == StreamTouchToolbarGesture.NONE);
        gesture.begin(12, 32);
        gesture.move(64, 32);
        gesture.cancel();
        check(gesture.finish(64, 32) == StreamTouchToolbarGesture.NONE);
        gesture.begin(0, 0);
        check(gesture.finish(32, 0) == StreamTouchToolbarGesture.SWIPE_RIGHT);
        check(gesture.finish(32, 0) == StreamTouchToolbarGesture.NONE);
        gesture.begin(0, 0);
        check(gesture.finish(-32, 0) == StreamTouchToolbarGesture.SWIPE_LEFT);
        invalid(0, 32);
        invalid(8, 7);
        invalid(Float.NaN, 32);
        invalid(8, Float.POSITIVE_INFINITY);
        System.out.println(assertions + " touch toolbar gesture checks passed");
    }

    private static void invalid(float slop, float threshold) {
        try {
            new StreamTouchToolbarGesture(slop, threshold);
            throw new AssertionError("Invalid gesture thresholds were accepted");
        }
        catch (IllegalArgumentException expected) {
            assertions++;
        }
    }
}
