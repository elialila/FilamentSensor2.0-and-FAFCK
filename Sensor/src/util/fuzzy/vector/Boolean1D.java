package util.fuzzy.vector;

import util.fuzzy.Fuzzy1D;

public class Boolean1D implements Fuzzy1D {

    private final boolean[] data;

    public Boolean1D(boolean[] data) {
        this.data = data;
    }

    public double get(double idx) {
        double weightA = idx - ((int) idx);
        double weightB = 1 - weightA;
        boolean first = data[(int) Math.floor(idx)];
        boolean second = data[(int) Math.ceil(idx)];

        //results here are probabilities if both are true -> result=true
        //if both are false -> result=false
        //if one is true the other false it depends on the weights and the threshold

        //which means if XOR == true weight stuff is needed
        if (first ^ second) {
            double maxWeight = Math.max(weightA, weightB);

            if (first) {
                return weightB;//chance for the value to be true: 1-weightA = weightB
            } else {
                return weightA;
            }
        } else {
            return (first) ? 1d : 0d;//if both are true, return 100% true, otherwise 0%
        }

    }

}
