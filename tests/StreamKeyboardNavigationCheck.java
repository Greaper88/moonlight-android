import com.limelight.ui.StreamKeyboardNavigation;

public class StreamKeyboardNavigationCheck {
    private static int assertions;

    private static void check(boolean result) {
        assertions++;
        if (!result) throw new AssertionError("Check " + assertions + " failed");
    }

    private static void invalid(float[][] weights) {
        try {
            new StreamKeyboardNavigation(weights);
            throw new AssertionError("Invalid layout was accepted");
        }
        catch (IllegalArgumentException expected) {
            assertions++;
        }
    }

    public static void main(String[] arguments) {
        StreamKeyboardNavigation navigation = new StreamKeyboardNavigation(
                new float[][] {{1, 1, 1, 1}, {1, 2, 1}, {1}});
        navigation.move(-1, 0);
        check(navigation.getRow() == 0 && navigation.getColumn() == 3);
        navigation.move(1, 0);
        check(navigation.getColumn() == 0);
        navigation.select(0, 2);
        navigation.move(0, 1);
        check(navigation.getRow() == 1 && navigation.getColumn() == 1);
        navigation.move(0, 1);
        check(navigation.getRow() == 2 && navigation.getColumn() == 0);
        navigation.move(1, 0);
        check(navigation.getColumn() == 0);
        navigation.move(0, 1);
        check(navigation.getRow() == 0 && navigation.getColumn() == 1);
        navigation.move(0, -1);
        check(navigation.getRow() == 2 && navigation.getColumn() == 0);
        navigation.move(0, 0);
        check(navigation.getRow() == 2 && navigation.getColumn() == 0);
        int[] rowSizes = {14, 14, 13, 12, 7};
        float[][] keyboard = new float[rowSizes.length][];
        for (int r = 0; r < keyboard.length; r++) {
            keyboard[r] = new float[rowSizes[r]];
            java.util.Arrays.fill(keyboard[r], 1);
        }
        navigation = new StreamKeyboardNavigation(keyboard);
        for (int r = 0; r < rowSizes.length; r++) {
            navigation.select(r, 0);
            for (int c = 0; c < rowSizes[r]; c++) {
                check(navigation.getColumn() == c);
                navigation.move(1, 0);
            }
            check(navigation.getColumn() == 0);
        }
        invalid(new float[][] {});
        invalid(new float[][] {{}});
        invalid(new float[][] {{0}});
        invalid(new float[][] {{-1}});
        invalid(new float[][] {{Float.NaN}});
        invalid(new float[][] {{Float.POSITIVE_INFINITY}});
        System.out.println(assertions + " keyboard navigation checks passed");
    }
}
