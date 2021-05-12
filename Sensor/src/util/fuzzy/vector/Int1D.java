package util.fuzzy.vector;

import util.fuzzy.Fuzzy1D;

public class Int1D implements Fuzzy1D {

    private final int[] data;

    public Int1D(int[] data) {
        this.data = data;
    }

    public double get(double idx) {
        double weightA = idx - ((int) idx);
        double weightB = 1 - weightA;
        double dFirst = data[(int) Math.floor(idx)];
        double dSecond = data[(int) Math.ceil(idx)];
        return (dFirst * weightA + dSecond * weightB);
    }

}
