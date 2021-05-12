package util.fuzzy.matrix;

import util.fuzzy.Fuzzy2D;

public class Int2D implements Fuzzy2D {

    private int[][] data;

    public Int2D(int[][] data) {
        this.data = data;
    }

    private double get(int[] arr, double idx) {
        double weightA = idx - ((int) idx);
        double weightB = 1 - weightA;
        double dFirst = arr[(int) Math.floor(idx)];
        double dSecond = arr[(int) Math.ceil(idx)];
        return (dFirst * weightA + dSecond * weightB);
    }


    @Override
    public double get(double x, double y) {
        double weightXA = x - ((int) x);
        double weightXB = 1 - weightXA;
        int[] first = data[(int) Math.floor(x)];
        int[] second = data[(int) Math.ceil(x)];

        double vFirst = get(first, y) * weightXB;
        double vSecond = get(second, y) * weightXA;

        return vFirst + vSecond;
    }
}
