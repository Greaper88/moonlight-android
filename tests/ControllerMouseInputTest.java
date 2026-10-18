import com.limelight.binding.input.ControllerMouseInput;
import com.limelight.binding.input.DpadMouseMotion;
import com.limelight.binding.input.HeldMouseScroll;
import com.limelight.nvstream.input.ControllerPacket;

public final class ControllerMouseInputTest {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static byte scroll(HeldMouseScroll scroll, int buttons, int leftTrigger,
                               int rightTrigger, boolean keyboardVisible, long now) {
        return scroll.update(ControllerMouseInput.scrollUp(buttons, (byte)leftTrigger, keyboardVisible),
                ControllerMouseInput.scrollDown(buttons, (byte)rightTrigger, keyboardVisible), now);
    }

    public static void main(String[] args) {
        int keyboardButtons = ControllerPacket.UP_FLAG | ControllerPacket.DOWN_FLAG |
                ControllerPacket.LEFT_FLAG | ControllerPacket.RIGHT_FLAG |
                ControllerPacket.A_FLAG | ControllerPacket.B_FLAG |
                ControllerPacket.X_FLAG | ControllerPacket.Y_FLAG |
                ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG;
        check(ControllerMouseInput.buttonsForMouse(keyboardButtons, true) == 0,
                "OSK D-pad, typing, close, and modifier controls cannot become mouse buttons or directions");
        check(ControllerMouseInput.buttonsForMouse(keyboardButtons, false) == keyboardButtons,
                "closed keyboard preserves ordinary mouse button routing");
        check(!ControllerMouseInput.scrollUp(ControllerPacket.LB_FLAG, (byte)0, true),
                "Shift bumper cannot scroll up while OSK is open");
        check(!ControllerMouseInput.scrollDown(ControllerPacket.RB_FLAG, (byte)0, true),
                "Fn bumper cannot scroll down while OSK is open");
        check(ControllerMouseInput.scrollUp(ControllerPacket.LB_FLAG, (byte)0, false),
                "closed keyboard retains left bumper scrolling");
        check(ControllerMouseInput.scrollDown(ControllerPacket.RB_FLAG, (byte)0, false),
                "closed keyboard retains right bumper scrolling");
        for (boolean keyboardVisible : new boolean[] {false, true}) {
            check(ControllerMouseInput.scrollUp(0, (byte)255, keyboardVisible),
                    "left trigger scroll accepts unsigned full range in either mode");
            check(ControllerMouseInput.scrollDown(0, (byte)128, keyboardVisible),
                    "right trigger scroll accepts unsigned mid range in either mode");
            check(!ControllerMouseInput.scrollUp(0, (byte)64, keyboardVisible) &&
                    !ControllerMouseInput.scrollDown(0, (byte)64, keyboardVisible),
                    "trigger threshold is unchanged in either mode");
        }

        DpadMouseMotion motion = new DpadMouseMotion();
        motion.update(ControllerMouseInput.buttonsForMouse(ControllerPacket.RIGHT_FLAG, false), 0);
        check(motion.getX() > 0, "D-pad moves the pointer with keyboard closed");
        motion.update(ControllerMouseInput.buttonsForMouse(ControllerPacket.RIGHT_FLAG, true), 50);
        check(motion.getX() == 0 && motion.getY() == 0,
                "opening keyboard cancels a buffered D-pad movement");

        HeldMouseScroll scrolling = new HeldMouseScroll();
        check(scroll(scrolling, ControllerPacket.LB_FLAG, 0, 0, false, 0) == 1,
                "mouse-mode bumper begins scrolling normally");
        check(scroll(scrolling, ControllerPacket.LB_FLAG, 0, 0, true, 50) == 0,
                "taking a held bumper for the OSK immediately stops its scroll");
        check(scroll(scrolling, ControllerPacket.LB_FLAG, 0, 0, true, 1000) == 0,
                "held OSK bumper does not restart scrolling on timer ticks");
        check(scroll(scrolling, ControllerPacket.LB_FLAG, 255, 0, true, 1100) == 1,
                "fresh left trigger starts scrolling while Shift bumper belongs to OSK");
        check(scroll(scrolling, ControllerPacket.RB_FLAG, 255, 0, true, 1150) == 0,
                "Fn bumper cannot oppose or reverse left-trigger scrolling");
        check(scroll(scrolling, 0, 255, 0, true, 1450) == 1,
                "held trigger continues after the initial repeat delay with OSK open");
        check(scroll(scrolling, 0, 255, 255, true, 1500) == 0,
                "opposed triggers cancel scrolling with OSK open");
        check(scroll(scrolling, ControllerPacket.LB_FLAG, 0, 255, true, 1600) == -1,
                "Shift bumper cannot oppose or reverse right-trigger scrolling");
        check(scroll(scrolling, ControllerPacket.LB_FLAG | ControllerPacket.RB_FLAG,
                0, 0, true, 1700) == 0,
                "trigger release stops scrolling even when both OSK modifiers are held");
        scrolling.reset();
        check(scroll(scrolling, 0, 0, 0, false, 1750) == 0,
                "visibility/lifecycle reset leaves no delayed scroll to emit");
        System.out.println("PASS: " + checks + " keyboard and mouse routing checks");
    }
}
