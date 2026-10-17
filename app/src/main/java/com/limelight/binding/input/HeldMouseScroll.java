package com.limelight.binding.input;

/** Scroll ticks for a tap or hold, independent of Android's input and callback timing. */
public final class HeldMouseScroll {
    private static final long REPEAT_DELAY_MS = 350;
    private static final long MAX_CATCH_UP_MS = 100;
    private static final double RAMP_SECONDS = 1.5;
    private static final double INITIAL_LINES_PER_SECOND = 5;
    private static final double MAX_LINES_PER_SECOND = 30;

    private int direction;
    private long startedAt;
    private long lastUpdatedAt;
    private boolean repeating;
    private double pendingLines;

    /** Controller trigger bytes carry unsigned values from 0 to 255. */
    public static boolean isTriggerPressed(byte trigger) {
        return (trigger & 0xFF) > 64;
    }

    public byte update(boolean up, boolean down, long now) {
        int requestedDirection = up == down ? 0 : up ? 1 : -1;
        if (requestedDirection == 0) {
            reset();
            return 0;
        }
        if (requestedDirection != direction || now < lastUpdatedAt) {
            direction = requestedDirection;
            startedAt = lastUpdatedAt = now;
            repeating = false;
            pendingLines = 0;
            return (byte)direction;
        }

        long heldMs = now - startedAt;
        if (heldMs < REPEAT_DELAY_MS) {
            lastUpdatedAt = now;
            return 0;
        }
        if (!repeating) {
            repeating = true;
            lastUpdatedAt = now;
            return (byte)direction;
        }

        // Avoid a burst of accumulated scrolling after a delayed callback or app pause.
        long elapsedMs = Math.min(MAX_CATCH_UP_MS, now - lastUpdatedAt);
        double rampEnd = (heldMs - REPEAT_DELAY_MS) / 1000.0;
        double rampStart = Math.max(0, rampEnd - elapsedMs / 1000.0);
        pendingLines += integratedLines(rampEnd) - integratedLines(rampStart);
        lastUpdatedAt = now;
        int lines = (int)(pendingLines + 1e-9);
        pendingLines -= lines;
        return (byte)(direction * lines);
    }

    private static double integratedLines(double seconds) {
        double ramp = Math.min(seconds, RAMP_SECONDS);
        return INITIAL_LINES_PER_SECOND * ramp
                + (MAX_LINES_PER_SECOND - INITIAL_LINES_PER_SECOND) * ramp * ramp
                    / (2 * RAMP_SECONDS)
                + MAX_LINES_PER_SECOND * Math.max(0, seconds - RAMP_SECONDS);
    }

    public void reset() {
        direction = 0;
        startedAt = lastUpdatedAt = 0;
        repeating = false;
        pendingLines = 0;
    }
}
