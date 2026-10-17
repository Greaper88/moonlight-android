package com.limelight.ui;

/** Selects a usable control display without depending on Android display IDs or ordering. */
public final class SecondaryDisplaySelection {
    public static final int NONE = -1;

    public static final class Candidate {
        public final int id;
        public final String name;
        public final boolean valid;
        public final boolean on;
        public final boolean presentation;

        public Candidate(int id, String name, boolean valid, boolean on, boolean presentation) {
            this.id = id;
            this.name = name;
            this.valid = valid;
            this.on = on;
            this.presentation = presentation;
        }
    }

    private SecondaryDisplaySelection() {
    }

    public static int choose(int currentDisplayId, int preferredDisplayId, Candidate[] candidates) {
        int fallback = NONE;
        if (candidates == null) {
            return NONE;
        }

        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.id < 0 || candidate.id == currentDisplayId ||
                    !candidate.valid || !candidate.on || !candidate.presentation ||
                    "HiddenDisplay".equals(candidate.name) ||
                    "WebRTC_ScreenCapture".equals(candidate.name)) {
                continue;
            }
            if (candidate.id == preferredDisplayId) {
                return candidate.id;
            }
            if (fallback == NONE) {
                fallback = candidate.id;
            }
        }
        return fallback;
    }
}
