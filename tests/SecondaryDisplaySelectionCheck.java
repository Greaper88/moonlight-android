import com.limelight.ui.SecondaryDisplaySelection;
import com.limelight.ui.SecondaryDisplaySelection.Candidate;

public final class SecondaryDisplaySelectionCheck {
    private static int assertions;

    private static Candidate usable(int id) {
        return new Candidate(id, "Panel " + id, true, true, true);
    }

    private static void selected(String scenario, int expected, int current, int preferred,
                                 Candidate... candidates) {
        assertions++;
        int actual = SecondaryDisplaySelection.choose(current, preferred, candidates);
        if (actual != expected) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        selected("No display connected", -1, 0, -1);
        selected("Unavailable display enumeration", -1, 0, -1, (Candidate[]) null);
        selected("Primary display is not a control display", -1, 0, 0, usable(0));
        selected("Secondary display need not have ID 1", 42, 0, -1, usable(0), usable(42));
        selected("Current activity can be on a non-default display", 0, 42, -1, usable(42), usable(0));
        selected("Auto mode keeps platform priority", 9, 0, -1, usable(9), usable(2));
        selected("Explicit preference overrides platform priority", 2, 0, 2, usable(9), usable(2));
        selected("Removed preferred display falls back", 9, 0, 2, usable(9));
        selected("Current display cannot be preferred", 9, 2, 2, usable(2), usable(9));
        selected("Invalid preferred display falls back", 9, 0, 2,
                new Candidate(2, "Removed panel", false, true, true), usable(9));
        selected("Powered-off preferred display falls back", 9, 0, 2,
                new Candidate(2, "Sleeping panel", true, false, true), usable(9));
        selected("Non-presentation preferred display falls back", 9, 0, 2,
                new Candidate(2, "Unsupported panel", true, true, false), usable(9));
        selected("Placeholder display cannot be selected", 9, 0, 2,
                new Candidate(2, "HiddenDisplay", true, true, true), usable(9));
        selected("Screen recording display cannot be selected", 9, 0, 2,
                new Candidate(2, "WebRTC_ScreenCapture", true, true, true), usable(9));
        selected("All unavailable displays yield none", -1, 0, -1,
                new Candidate(2, "Sleeping panel", true, false, true),
                new Candidate(9, "Removed panel", false, true, true));
        selected("Missing names do not hide a usable display", 2, 0, -1,
                new Candidate(2, null, true, true, true));
        selected("Missing display records do not block fallback", 9, 0, -1, null, usable(9));
        selected("Negative IDs cannot collide with none sentinel", 9, 0, -1, usable(-1), usable(9));
        selected("Duplicate IDs do not change automatic selection", 9, 0, -1,
                usable(9), usable(9), usable(2));
        selected("Duplicate preferred IDs still select preference", 2, 0, 2,
                usable(9), usable(2), usable(2));
        selected("Stale duplicate does not hide a valid display record", 2, 0, 2,
                new Candidate(2, "Removed panel", false, true, true), usable(9), usable(2));

        Candidate[] active = {usable(0), usable(42)};
        selected("Display is selected before sleep", 42, 0, 42, active);
        active[1] = new Candidate(42, "Panel 42", true, false, true);
        selected("Sleeping display is released", -1, 0, 42, active);
        active[1] = usable(42);
        selected("Display is selectable again after wake", 42, 0, 42, active);

        System.out.println(assertions + " secondary display selection checks passed");
    }
}
