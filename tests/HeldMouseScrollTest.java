import com.limelight.binding.input.HeldMouseScroll;

public class HeldMouseScrollTest {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static int scrollFor(HeldMouseScroll scroll, boolean up, boolean down, int from, int to) {
        int lines = 0;
        for (int now = from; now <= to; now += 50) {
            lines += scroll.update(up, down, now);
        }
        return lines;
    }

    public static void main(String[] args) {
        check(!HeldMouseScroll.isTriggerPressed((byte)0), "neutral trigger does not scroll");
        check(!HeldMouseScroll.isTriggerPressed((byte)64), "quarter-press threshold ignores small trigger motion");
        check(HeldMouseScroll.isTriggerPressed((byte)65), "trigger crosses threshold after quarter press");
        check(HeldMouseScroll.isTriggerPressed((byte)128), "negative signed byte still represents a held trigger");
        check(HeldMouseScroll.isTriggerPressed((byte)255), "digital trigger's full press scrolls");
        HeldMouseScroll scroll = new HeldMouseScroll();
        check(scroll.update(false, false, 0) == 0, "idle input never scrolls");
        check(scroll.update(true, false, 0) == 1, "tap immediately scrolls one line up");
        check(scroll.update(true, false, 0) == 0, "duplicate reports do not repeat a tap");
        check(scroll.update(true, false, 349) == 0, "repeat waits the full delay");
        check(scroll.update(false, false, 349) == 0, "short tap release never adds scrolling");
        check(scroll.update(false, true, 400) == -1, "right side scrolls one line down");
        check(scroll.update(false, true, 749) == 0, "downward repeat also waits");
        check(scroll.update(false, true, 750) == -1, "held input starts repeating at 350 ms");
        check(scroll.update(false, true, 750) == 0, "duplicate timer tick is harmless");
        check(scroll.update(false, false, 760) == 0, "release cancels repeating immediately");
        check(scroll.update(false, false, 5000) == 0, "release leaves no pending repeat");

        scroll.reset();
        scroll.update(true, false, 0);
        scrollFor(scroll, true, false, 50, 350);
        int early = scrollFor(scroll, true, false, 400, 850);
        int later = scrollFor(scroll, true, false, 900, 1350);
        int nearCap = scrollFor(scroll, true, false, 1400, 1850);
        int capped = scrollFor(scroll, true, false, 1900, 2850);
        int stillCapped = scrollFor(scroll, true, false, 2900, 3850);
        check(early >= 3 && early <= 5, "initial repeats remain gentle");
        check(later > early && nearCap > later, "holding progressively accelerates scrolling");
        check(capped == 30 && stillCapped == 30, "long holds cap at 30 lines per second");
        check(scroll.update(false, true, 3860) == -1, "direction change immediately reverses one line");
        check(scroll.update(false, true, 4209) == 0, "direction change restarts repeat delay");
        check(scroll.update(true, true, 4210) == 0, "opposing inputs cancel scrolling");
        check(scroll.update(true, true, 10000) == 0, "opposing holds cannot accumulate repeats");
        check(scroll.update(true, false, 10050) == 1, "leaving opposition starts a fresh tap");
        check(scroll.update(true, false, 10399) == 0, "leaving opposition restarts repeat delay");

        scroll.reset();
        scroll.update(true, false, 0);
        scroll.update(true, false, 350);
        check(scroll.update(true, false, 10000) <= 3, "delayed callbacks cannot cause a large burst");
        scroll.reset();
        check(scroll.update(false, false, 11000) == 0, "reset removes pending motion");
        check(scroll.update(false, true, 11001) == -1, "reset starts the next hold at tap speed");
        check(scroll.update(false, true, 11350) == 0, "reset clears old acceleration and delay");

        HeldMouseScroll frequent = new HeldMouseScroll();
        HeldMouseScroll timerOnly = new HeldMouseScroll();
        int frequentLines = frequent.update(true, false, 0);
        int timerLines = timerOnly.update(true, false, 0);
        for (int now = 10; now <= 5000; now += 10) {
            frequentLines += frequent.update(true, false, now);
            if (now % 50 == 0) timerLines += timerOnly.update(true, false, now);
        }
        check(frequentLines == timerLines, "extra controller reports do not change hold speed");
        System.out.println("PASS: " + checks + " held mouse scrolling checks");
    }
}
