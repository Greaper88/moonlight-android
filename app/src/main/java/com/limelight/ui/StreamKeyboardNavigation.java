package com.limelight.ui;

/** Keeps controller selection aligned when moving between keyboard rows of different widths. */
public final class StreamKeyboardNavigation {
    private final double[][] centers;
    private int row;
    private int column;

    public StreamKeyboardNavigation(float[][] weights) {
        if (weights.length == 0) {
            throw new IllegalArgumentException("A keyboard needs at least one row");
        }
        centers = new double[weights.length][];
        for (int r = 0; r < weights.length; r++) {
            if (weights[r].length == 0) {
                throw new IllegalArgumentException("Keyboard rows must not be empty");
            }
            double total = 0;
            for (float weight : weights[r]) {
                if (weight <= 0 || Float.isNaN(weight) || Float.isInfinite(weight)) {
                    throw new IllegalArgumentException("Key weights must be finite and positive");
                }
                total += weight;
            }
            centers[r] = new double[weights[r].length];
            double offset = 0;
            for (int c = 0; c < weights[r].length; c++) {
                centers[r][c] = (offset + weights[r][c] / 2.0) / total;
                offset += weights[r][c];
            }
        }
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void select(int selectedRow, int selectedColumn) {
        if (selectedRow < 0 || selectedRow >= centers.length ||
                selectedColumn < 0 || selectedColumn >= centers[selectedRow].length) {
            throw new IllegalArgumentException("Invalid keyboard selection");
        }
        row = selectedRow;
        column = selectedColumn;
    }

    public void move(int horizontal, int vertical) {
        if (vertical != 0) {
            double center = centers[row][column];
            row = (row + (vertical > 0 ? 1 : -1) + centers.length) % centers.length;
            column = 0;
            for (int c = 1; c < centers[row].length; c++) {
                if (Math.abs(centers[row][c] - center) < Math.abs(centers[row][column] - center)) {
                    column = c;
                }
            }
        }
        if (horizontal != 0) {
            column = (column + (horizontal > 0 ? 1 : -1) + centers[row].length) % centers[row].length;
        }
    }
}
