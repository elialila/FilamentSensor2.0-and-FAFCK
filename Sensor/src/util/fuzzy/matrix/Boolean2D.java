package util.fuzzy.matrix;

import util.fuzzy.Fuzzy2D;

public class Boolean2D implements Fuzzy2D {

    private boolean[][] data;

    public Boolean2D(boolean[][] data) {
        this.data = data;
    }


    private double get(boolean[] array, double idx) {
        double weightA = idx - ((int) idx);
        double weightB = 1 - weightA;
        boolean first = array[(int) Math.floor(idx)];
        boolean second = array[(int) Math.ceil(idx)];
        //results here are probabilities if both are true -> result=true
        //if both are false -> result=false
        //if one is true the other false it depends on the weights and the threshold
        //which means if XOR == true weight stuff is needed
        if (first ^ second) {
            if (first) {
                return weightB;//chance for the value to be true: 1-weightA = weightB
            } else {
                return weightA;
            }
        } else {
            return (first) ? 1d : 0d;//if both are true, return 100% true, otherwise 0%
        }

    }


    public double get(double x, double y) {
        double weightXA = x - ((int) x);
        double weightXB = 1 - weightXA;
        boolean[] first = data[(int) Math.floor(x)];
        boolean[] second = data[(int) Math.ceil(x)];

        double probFirst = get(first, y) * weightXB;
        double probSecond = get(second, y) * weightXA;

        return probFirst + probSecond;
    }

}
