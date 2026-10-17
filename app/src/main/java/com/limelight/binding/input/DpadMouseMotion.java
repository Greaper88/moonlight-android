package com.limelight.binding.input;

import com.limelight.nvstream.input.ControllerPacket;

/** Pixel motion per 50 ms tick, with a gentle start and normalized diagonals. */
public final class DpadMouseMotion {
    private int lastX;
    private int lastY;
    private long startedAt;
    private short x;
    private short y;

    public void update(int buttons, long now) {
        int horizontal = ((buttons & ControllerPacket.RIGHT_FLAG) != 0 ? 1 : 0)
                - ((buttons & ControllerPacket.LEFT_FLAG) != 0 ? 1 : 0);
        int vertical = ((buttons & ControllerPacket.DOWN_FLAG) != 0 ? 1 : 0)
                - ((buttons & ControllerPacket.UP_FLAG) != 0 ? 1 : 0);
        if (horizontal != lastX || vertical != lastY) {
            startedAt = now;
        }
        lastX = horizontal;
        lastY = vertical;
        double speed = 2 + 22 * Math.min(1, Math.max(0, now - startedAt) / 1000.0);
        if (horizontal != 0 && vertical != 0) {
            speed /= Math.sqrt(2);
        }
        x = (short)Math.round(horizontal * speed);
        y = (short)Math.round(vertical * speed);
    }

    public short getX() { return x; }
    public short getY() { return y; }

    public void reset() {
        lastX = lastY = x = y = 0;
        startedAt = 0;
    }
}
