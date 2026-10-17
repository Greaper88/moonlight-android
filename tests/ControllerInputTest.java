import com.limelight.binding.input.SelectShortcutState;
import com.limelight.binding.input.DpadMouseMotion;
import com.limelight.nvstream.input.ControllerPacket;

public class ControllerInputTest {
    private static int checks;
    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        int select = ControllerPacket.BACK_FLAG;
        SelectShortcutState s = new SelectShortcutState();
        check(s.update(select, 0) == 0, "Select down is deferred");
        check(s.nextDeadline() == 750, "hold has a timer even without input repeats");
        check(s.update(select, 749) == 0 && !s.takeToggleRequest(), "hold does not fire early");
        check(s.update(select, 750) == 0 && s.takeToggleRequest(), "hold toggles at threshold without host Select");
        s.update(select, 1000);
        check(!s.takeToggleRequest(), "held Select toggles only once");
        check(s.update(0, 1200) == 0 && !s.takeToggleRequest(), "hold release never sends Select");
        check(s.update(select, 1300) == 0, "second hold starts cleanly");
        s.update(select, 2050);
        check(s.takeToggleRequest(), "second hold can close keyboard");
        s.update(0, 2100);
        s.reset();
        s.update(select, 10);
        check(s.update(0, 100) == select, "short press becomes host Select");
        check(s.update(0, 124) == select, "short pulse lasts minimum 25ms");
        check(s.update(0, 125) == 0, "short pulse releases on timer");
        check(!s.takeToggleRequest(), "short press never toggles keyboard");
        s.reset();
        s.update(select, 0);
        check(s.update(0, 750) == 0 && s.takeToggleRequest(), "late release recognizes hold without timer tick");
        s.reset();
        s.update(select, 0);
        int chord = select | ControllerPacket.LB_FLAG;
        check(s.update(chord, 200) == chord, "Select chord passes through");
        s.update(chord, 900);
        check(!s.takeToggleRequest(), "chord cannot open OSK");
        s.update(ControllerPacket.LB_FLAG, 1000);
        check(s.update(0, 1100) == 0, "chord release creates no extra Select pulse");
        s.reset();
        s.update(select, 0);
        check(s.update(ControllerPacket.SPECIAL_BUTTON_FLAG, 100) == ControllerPacket.SPECIAL_BUTTON_FLAG,
              "remapped Start+Select combo remains intact");
        check(s.update(0, 200) == 0 && !s.takeToggleRequest(), "remapped combo has no ghost Select");
        s.reset();
        s.update(select, 0);
        s.reset();
        check(s.update(0, 900) == 0 && !s.takeToggleRequest(), "destroy/reset cancels pending hold");
        s.update(select, 1000);
        s.suppressUntilRelease();
        check(s.update(select, 2000) == 0 && !s.takeToggleRequest(), "overlay transition cancels pending hold");
        check(s.update(0, 2100) == 0 && !s.takeToggleRequest(), "suppressed Select release cannot leak");
        s.update(select, 2200);
        s.update(0, 2250);
        s.suppressUntilRelease();
        check(s.update(0, 2251) == 0, "overlay transition cancels pending tap pulse");
        s.update(select, 2400);
        s.update(select, 3150);
        check(s.takeToggleRequest(), "Select re-arms after suppressed release");
        DpadMouseMotion m = new DpadMouseMotion();
        m.update(ControllerPacket.RIGHT_FLAG, 0);
        check(m.getX() == 2 && m.getY() == 0, "Dpad starts gently");
        m.update(ControllerPacket.RIGHT_FLAG, 500);
        check(m.getX() == 13, "held Dpad accelerates");
        m.update(ControllerPacket.RIGHT_FLAG, 2000);
        check(m.getX() == 24, "Dpad speed is capped");
        m.update(ControllerPacket.LEFT_FLAG, 2010);
        check(m.getX() == -2, "direction change resets acceleration");
        m.update(0, 2100);
        check(m.getX() == 0 && m.getY() == 0, "release stops movement");
        m.update(ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG, 2200);
        check(m.getX() == 0 && m.getY() == 0, "opposite directions cancel");
        m.update(ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG, 3000);
        m.update(ControllerPacket.UP_FLAG | ControllerPacket.RIGHT_FLAG, 4000);
        check(m.getX() > 0 && m.getY() < 0, "diagonals move in both axes");
        check(Math.abs(Math.hypot(m.getX(), m.getY()) - 24) < 1, "diagonal speed is normalized");
        m.reset();
        m.update(ControllerPacket.DOWN_FLAG, 6000);
        check(m.getY() == 2 && m.getX() == 0, "reset clears stale motion");
        System.out.println("PASS: " + checks + " controller shortcut and Dpad movement checks");
    }
}
