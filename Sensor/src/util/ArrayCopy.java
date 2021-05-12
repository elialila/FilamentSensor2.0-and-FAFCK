package util;

public class ArrayCopy {


    public static int[][] copy(int[][] input) {
        int[][] copy = new int[input.length][];
        for (int i = 0; i < input.length; i++) {
            copy[i] = input[i].clone();
        }
        return copy;
    }


}
