package core;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2014 Benjamin Eltzner
 *
 * FilamentSensor is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * or see <http://www.gnu.org/licenses/>.
 */

import util.Annotations.NotNull;
import core.image.BinaryImage;
import core.image.IBinaryImage;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Calc {

    private Calc() {
    } // No instances. This is a function container.

    public static long orientation(Point a, Point b) {
        if (b.y == a.y) {
            return 0;
        }
        if (b.x == a.x) {
            return 90 * Const.M;
        }

        long ang = Math.round(Const.M * (180 - Math.atan2(b.y - a.y, b.x - a.x) / Const.RAD));
        return ang < 0 ? 180 * Const.M + ang : ang % (180 * Const.M);
    }

    public static long distanceM(Point a, Point b) {
        return Math.round(Const.M * Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)));
    }

    /**
     * Mask of points (x,y) that satisfy x^2 + y^2 < ceil(0.25 * (d^2 + d))
     * which is equivalent to x^2 + y^2 < 0.25 * (d^2 + d).
     */
    public static boolean[][] circleMask(int diameter) {
        int range = diameter / 2;
        boolean[][] mask = new boolean[2 * range + 1][2 * range + 1];
        for (int x = 0; x <= 2 * range; x++) {
            for (int y = 0; y <= 2 * range; y++) {
                mask[x][y] = (x - range) * (x - range) + (y - range) * (y - range) <
                        (diameter * diameter + diameter + 3) / 4;
            }
        }
        return mask;
    }

    public static BitSet[] circleMaskBitSet(int diameter) {
        int range = diameter / 2;
        BitSet[] mask = new BitSet[2 * range + 1];
        for (int x = 0; x <= 2 * range; x++) {
            mask[x] = new BitSet(2 * range + 1);
            for (int y = 0; y <= 2 * range; y++) {
                if ((x - range) * (x - range) + (y - range) * (y - range) < (diameter * diameter + diameter + 3) / 4)
                    mask[x].set(y);
            }
        }
        return mask;
    }

    /**
     * Mask of points (x,y) that satisfy x^2 + y^2 < ceil(0.25 * (d^2 + d))
     * which is equivalent to x^2 + y^2 < 0.25 * (d^2 + d).
     */
    public static IBinaryImage circleMaskBinary(int diameter) {
        int range = diameter / 2;
        IBinaryImage binaryImage = new BinaryImage(2 * range + 1, 2 * range + 1);
        for (int x = 0; x <= 2 * range; x++) {
            for (int y = 0; y <= 2 * range; y++) {
                if ((x - range) * (x - range) + (y - range) * (y - range) < (diameter * diameter + diameter + 3) / 4)
                    binaryImage.setPixel(x, y);
            }
        }
        return binaryImage;
    }


    public static IBinaryImage nextPixelsMask() {
        BinaryImage cross = new BinaryImage(3, 3);
        cross.setPixel(1, 0);
        cross.setPixel(1, 1);
        cross.setPixel(1, 2);
        cross.setPixel(0, 1);
        cross.setPixel(2, 1);
        return cross;
    }




    public static double[][] gaussianMask(int range, double phi, double sigma_x, double sigma_y) {
        double sum = 0.0;

        double[] rotated_variance =
                {
                        Math.cos(phi) * Math.cos(phi) / (sigma_x * sigma_x)
                                + Math.sin(phi) * Math.sin(phi) / (sigma_y * sigma_y),

                        Math.sin(phi) * Math.cos(phi) * (1 / (sigma_x * sigma_x) - 1 / (sigma_y * sigma_y)),

                        Math.sin(phi) * Math.sin(phi) / (sigma_x * sigma_x)
                                + Math.cos(phi) * Math.cos(phi) / (sigma_y * sigma_y)
                };

        double[][] mask = new double[2 * range + 1][2 * range + 1];
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                double gauss = Math.exp(-(dx * dx * rotated_variance[0]
                        + 2 * dx * dy * rotated_variance[1]
                        + dy * dy * rotated_variance[2]) / 2.0);
                mask[range + dx][range + dy] = gauss;
                sum += gauss;
            }
        }

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                mask[range + dx][range + dy] /= sum;
            }
        }
        return mask;
    }

    public static double[][] gaussianMask(int range, double sigma) {
        return gaussianMask(range, 0, sigma, sigma);
    }

    public static double[][] gaussianQuarterMask(int range, double phi, double sigma_x, double sigma_y) {
        double sum = 0.0;

        double[] rotated_variance =
                {
                        Math.cos(phi) * Math.cos(phi) / (sigma_x * sigma_x)
                                + Math.sin(phi) * Math.sin(phi) / (sigma_y * sigma_y),

                        Math.sin(phi) * Math.cos(phi) * (1 / (sigma_x * sigma_x) - 1 / (sigma_y * sigma_y)),

                        Math.sin(phi) * Math.sin(phi) / (sigma_x * sigma_x)
                                + Math.cos(phi) * Math.cos(phi) / (sigma_y * sigma_y)
                };

        double[][] mask = new double[range + 1][range + 1];
        for (int dx = 0; dx <= range; dx++) {
            for (int dy = 0; dy <= range; dy++) {
                double gauss = Math.exp(-(dx * dx * rotated_variance[0]
                        + 2 * dx * dy * rotated_variance[1]
                        + dy * dy * rotated_variance[2]) / 2.0);
                mask[dx][dy] = gauss;
                sum += gauss;
            }
        }

        for (int dx = 0; dx <= range; dx++) {
            for (int dy = 0; dy <= range; dy++) {
                mask[dx][dy] /= sum;
            }
        }
        return mask;
    }

    public static double[][] gaussianQuarterMask(int range, double sigma) {
        return gaussianQuarterMask(range, 0, sigma, sigma);
    }

    /**
     * Return a one-dimensional Gaussian mask where the sum of all
     * entries is 1.
     */
    public static double[] gaussian1d(int range, double sigma) {
        double sum = 0.0;
        final double factor = -1.0 / (2.0 * sigma * sigma);

        double[] mask = new double[2 * range + 1];
        for (int dx = -range; dx <= range; dx++) {
            double gauss = Math.exp(factor * (dx * dx));
            mask[range + dx] = gauss;
            sum += gauss;
        }

        for (int dx = -range; dx <= range; dx++) {
            mask[range + dx] /= sum;
        }
        return mask;
    }

    /**
     * Returns a one-dimensional Gaussian mask whose center is scaled
     * to the given value "scale".
     */
    public static double[] gaussian1d(final int range, final double sigma,
                                      final double scale, final double step) {
        final double factor = -1.0 / (2.0 * sigma * sigma);

        double[] mask = new double[2 * range + 1];
        for (int i = -range; i <= range; i++) {
            double dx = i * step;
            mask[range + i] = Math.exp(factor * (dx * dx)) * scale;
        }
        return mask;
    }

    /**
     * Return a one-dimensional mask of minus the derivative of a Gaussian
     * where the sum of all entries of the original gaussian is 1. The
     * inverse sign is chosen such that applying this mask to an array it
     * acts as the derivative of the array smeared by a Gaussian.
     */
    public static double[] gaussianDerivative(int range, final double sigma, final double step) {
        final double factor = -1.0 / (2.0 * sigma * sigma);
        double sum = 0.0;

        double[] mask = new double[2 * range + 1];
        for (int i = -range; i <= range; i++) {
            double dx = i * step;
            double gauss = Math.exp(factor * (dx * dx));
            mask[range + i] = -2.0 * factor * dx * gauss;
            sum += gauss;
        }

        for (int dx = -range; dx <= range; dx++) {
            mask[range + dx] /= sum;
        }
        return mask;
    }

    public static double[] gaussianDerivative2(int range, final double sigma, final double step) {
        final double factor = -1.0 / (sigma * sigma),
                factor2 = factor * factor;
        double sum = 0.0;

        double[] mask = new double[2 * range + 1];
        for (int i = -range; i <= range; i++) {
            double dx = i * step;
            double gauss = Math.exp(0.5 * factor * (dx * dx));
            mask[range + i] = (factor + factor2 * dx * dx) * gauss;
            sum += gauss;
        }

        for (int dx = -range; dx <= range; dx++) {
            mask[range + dx] /= sum;
        }
        return mask;
    }

    public static long pow(int base, int exp) {
        long out = 1;
        for (int i = 0; i < exp; i++) {
            out *= base;
        }
        return out;
    }

    /**
     * Invert a monotonic function. If the input function is not monotonic
     * in the suitable sense, the result is undefined.
     *
     * @param function A monotonically growing function.
     * @param guess    A guess for the values of the inverse function.
     * @param cutoff   The maximum result of the inverse function.
     * @param growing  This states whether the input function is growing
     *                 or shrinking.
     * @return Inverse Function
     */
    public static Function<Double, Double> invert(final Function<Double, Double> function, final double guess,
                                                  final double cutoff, final boolean growing) {
        final double PRECISION = 1e-15;
        final double cutoff_abs = Math.abs(cutoff);
        final double sign = (growing ? 1.0 : -1.0);
        return y -> {
            double step = cutoff_abs,
                    test = 0.0,
                    out = guess;
            while (step > PRECISION) {
                double diff = 1.0;
                while (diff > 0) {
                    out = test;
                    test = out + step;
                    if (test == out) {
                        break;
                    }
                    diff = sign * (y - function.apply(test));
                    if (test > cutoff_abs) {
                        return cutoff_abs;
                    }
                }
                test = out;
                step *= 0.5;
            }
            if (out == guess) {
                step = -cutoff_abs;
                test = 0.0;
                while (-step > PRECISION) {
                    double diff = 1.0;
                    while (diff > 0) {
                        out = test;
                        test = out + step;
                        if (test == out) {
                            break;
                        }
                        diff = sign * (function.apply(test) - y);
                        if (test < -cutoff_abs) {
                            return -cutoff_abs;
                        }
                    }
                    test = out;
                    step *= 0.5;
                }
            }
            return out;
        };
    }

    /**
     * Implementation of the probit function with double precision following
     * <p>
     * "Algorithm AS 241: The Percentage Points of the Normal Distribution"
     * Michael J. Wichura
     * Journal of the Royal Statistical Society. Series C (Applied Statistics)
     * Vol. 37, No. 3 (1988), pp. 477-484
     * <p>
     * The function calculates values of approximating polynomials with
     * pre-calculated coefficients. It is not optimized for performance
     * but for source code readability.
     */
    public static double probit(final double x) throws IllegalArgumentException {
        // Coefficients for x close to 0.5
        final double[] a1 = new double[]{3.3871328727963666080e0,
                1.3314166789178437745e+2,
                1.9715909503065514427e+3,
                1.3731693765509461125e+4,
                4.5921953931549871457e+4,
                6.7265770927008700853e+4,
                3.3430575583588128105e+4,
                2.5090809287301226727e+3},
                a2 = new double[]{1.0,
                        4.2313330701600911252e+1,
                        6.8718700749205790830e+2,
                        5.3941960214247511077e+3,
                        2.1213794301586595867e+4,
                        3.9307895800092710610e+4,
                        2.8729085735721942674e+4,
                        5.2264952788528545610e+3},
                // Coefficients for x in intermediate region.
                b1 = new double[]{1.42343711074968357734e0,
                        4.63033784615654529590e0,
                        5.76949722146069140550e0,
                        3.64784832476320460504e0,
                        1.27045825245236838258e0,
                        2.41780725177450611770e-1,
                        2.27238449892691845833e-2,
                        7.74545014278341407640e-4},
                b2 = new double[]{1.0,
                        2.05319162663775882187e0,
                        1.67638483018380384940e0,
                        6.89767334985100004550e-1,
                        1.48103976427480074590e-1,
                        1.51986665636164571966e-2,
                        5.47593808499534494600e-4,
                        1.05075007164441684324e-9},
                // Coefficients for x near 0 or 1.
                c1 = new double[]{6.65790464350110377720e0,
                        5.46378491116411436990e0,
                        1.78482653991729133580e0,
                        2.96560571828504891230e-1,
                        2.65321895265761230930e-2,
                        1.24266094738807843860e-3,
                        2.71155556874348757815e-5,
                        2.01033439929228813265e-7},
                c2 = new double[]{1.0,
                        5.99832206555887937690e-1,
                        1.36929880922735805310e-1,
                        1.48753612908506148525e-2,
                        7.86869131145613259100e-4,
                        1.84631831751005468180e-5,
                        1.42151175831644588870e-7,
                        2.04426310338993978564e-15};
        // Other constants
        final double split1 = 0.425, split2 = 5.0, const1 = 0.180625, const2 = 1.6,
                y = x - 0.5;

        // Determine region and calculate polynomial values.
        double z, out;
        if (Math.abs(y) <= split1) {
            z = const1 - y * y;
            return y * polynomial(a1, z) / polynomial(a2, z);
        } else {
            if (y < 0) {
                z = x;
            } else {
                z = 1.0 - x;
            }
            if (z <= 0.0) {
                throw new IllegalArgumentException("" + x);
            }

            z = Math.sqrt(-Math.log(z));
            if (z <= split2) {
                z -= const2;
                out = polynomial(b1, z) / polynomial(b2, z);
            } else {
                z -= split2;
                out = polynomial(c1, z) / polynomial(c2, z);
            }
            if (y < 0.0) {
                return -out;
            }
        }
        return out;
    }

    /**
     * An implementation of the Jacobi method. Returns the
     * matrix of Eigenvalues and the matrix of Eigenvectors.
     */
    public static double[][][] diagonalize(double[][] matrix) {
        double[][] diag = copy(matrix);
        double[][] vectors = new double[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            vectors[i][i] = 1;
        }

        while (true) {
            // Find entry farthest away from 0.
            double max = 0;
            int a = -1, b = -1;   // row and column number for "maximum".
            for (int i = 0; i < diag.length; i++) {
                for (int j = i + 1; j < diag.length; j++) {
                    if (Math.abs(diag[i][j]) > max) {
                        max = Math.abs(diag[i][j]);
                        a = i;
                        b = j;
                    }
                }
            }

            if (a < 0) {
                break;
            }

            double[][] rotation = rotate2x2(new double[][]{{diag[a][a], diag[a][b]},
                    {diag[b][a], diag[b][b]}});
            if (Math.abs(rotation[0][1]) < 1e-15) {
                break;
            }
            diag = adjoin(diag, rotation, a, b);
            vectors = multiply(vectors, rotation, a, b, false);
        }
        return new double[][][]{diag, vectors};
    }

    public static boolean[][] invert(boolean[][] binary_image) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0) {
            System.err.println("FilamentSensor CellData.invert: Image must exist.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length;

        boolean[][] out = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out[x][y] = !binary_image[x][y];
            }
        }
        return out;
    }

    public static boolean[][] dilate(boolean[][] binary_image, boolean[][] mask) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask == null || mask.length % 2 == 0 || mask[0].length != mask.length) {
            System.err.println("FilamentSensor CellData.dilate: " +
                    "Image and mask must exist. Mask must be quadratic " +
                    "and its side length must be an odd number of pixels.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length,
                range = mask.length / 2;

        boolean[][] out = new boolean[width][height];

        // Dilation
        for (int x = 0; x < width; x++) {
            if (x == range) {
                x = width - range;
            }
            for (int y = 0; y < height; y++) {
                if (binary_image[x][y]) {
                    int x_min = -Math.min(x, range),
                            x_max = Math.min(width - x - 1, range),
                            y_min = -Math.min(y, range),
                            y_max = Math.min(height - y - 1, range);

                    for (int a = x_min; a <= x_max; a++) {
                        for (int b = y_min; b <= y_max; b++) {
                            out[x + a][y + b] = mask[a + range][b + range] || out[x + a][y + b];
                        }
                    }
                }
            }
        }

        for (int y = 0; y < height; y++) {
            if (y == range) {
                y = height - range;
            }
            for (int x = 0; x < width; x++) {

                if (binary_image[x][y]) {
                    int x_min = -Math.min(x, range),
                            x_max = Math.min(width - x - 1, range),
                            y_min = -Math.min(y, range),
                            y_max = Math.min(height - y - 1, range);

                    for (int a = x_min; a <= x_max; a++) {
                        for (int b = y_min; b <= y_max; b++) {
                            out[x + a][y + b] = mask[a + range][b + range] || out[x + a][y + b];
                        }
                    }
                }
            }
        }
        for (int x = range; x < width - range; x++) {
            for (int y = range; y < height - range; y++) {
                if (binary_image[x][y]) {
                    if (!binary_image[x - 1][y] || !binary_image[x + 1][y] ||
                            !binary_image[x][y - 1] || !binary_image[x][y + 1]) {
                        for (int a = -range; a <= range; a++) {
                            for (int b = -range; b <= range; b++) {
                                out[x + a][y + b] = mask[a + range][b + range] || out[x + a][y + b];
                            }
                        }
                    } else {
                        out[x][y] = true;
                    }
                }
            }
        }
        return out;
    }

    public static boolean[][] close(boolean[][] binary_image, boolean[][] mask) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask == null || mask.length % 2 == 0 || mask[0].length != mask.length) {
            System.err.println("FilamentSensor Calc.close: " +
                    "Image and mask must exist. Mask must be quadratic " +
                    "and its side length must be an odd number of pixels.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length,
                range = mask.length / 2,
                width_plus = width + 2 * range,
                height_plus = height + 2 * range;

        boolean[][] tmp = new boolean[width_plus][height_plus];

        // Dilation
        for (int x = 0; x < width; x++) {
            if (binary_image[x][0]) {
                for (int a = 0; a <= 2 * range; a++) {
                    for (int b = 0; b <= 2 * range; b++) {
                        tmp[x + a][b] = mask[a][b] || tmp[x + a][b];
                    }
                }
            }
            if (binary_image[x][height - 1]) {
                for (int a = 0; a <= 2 * range; a++) {
                    for (int b = 0; b <= 2 * range; b++) {
                        tmp[x + a][height - 1 + b] = mask[a][b] || tmp[x + a][height - 1 + b];
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (binary_image[0][y]) {
                for (int a = 0; a <= 2 * range; a++) {
                    for (int b = 0; b <= 2 * range; b++) {
                        tmp[a][y + b] = mask[a][b] || tmp[a][y + b];
                    }
                }
            }
            if (binary_image[width - 1][y]) {
                for (int a = 0; a <= 2 * range; a++) {
                    for (int b = 0; b <= 2 * range; b++) {
                        tmp[width - 1 + a][y + b] = mask[a][b] || tmp[width - 1 + a][y + b];
                    }
                }
            }
        }

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (binary_image[x][y]) {
                    if (!binary_image[x - 1][y] || !binary_image[x + 1][y] ||
                            !binary_image[x][y - 1] || !binary_image[x][y + 1]) {
                        for (int a = 0; a <= 2 * range; a++) {
                            for (int b = 0; b <= 2 * range; b++) {
                                tmp[x + a][y + b] = mask[a][b] || tmp[x + a][y + b];
                            }
                        }
                    } else {
                        tmp[x + range + 1][y + range + 1] = true;
                    }
                }
            }
        }

        // Copy dilated image to original
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                binary_image[x][y] = tmp[x + range][y + range];
            }
        }

        // Erosion
        for (int x = 0; x < width_plus; x++) {
            if (x == 2 * range) {
                x = width;
            }
            for (int y = 0; y < height_plus; y++) {
                // Left and right boundary
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width_plus - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height_plus - 1 && tmp[x][y + 1]))) {
                    for (int a = 0; a <= 2 * range; a++) {
                        if (x - a >= 0 && x - a < width) {
                            for (int b = 0; b <= 2 * range; b++) {
                                if (y - b >= 0 && y - b < height && mask[a][b]) {
                                    binary_image[x - a][y - b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int y = 0; y < height_plus; y++) {
            if (y == 2 * range) {
                y = height;
            }
            for (int x = 0; x < width_plus; x++) {
                // Upper and lower boundary
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width_plus - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height_plus - 1 && tmp[x][y + 1]))) {
                    for (int b = 0; b <= 2 * range; b++) {
                        if (y - b >= 0 && y - b < height) {
                            for (int a = 0; a <= 2 * range; a++) {
                                if (x - a >= 0 && x - a < width && mask[a][b]) {
                                    binary_image[x - a][y - b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Interior
        for (int x = 2 * range; x < width; x++) {
            for (int y = 2 * range; y < height; y++) {
                if (!tmp[x][y] &&
                        (tmp[x - 1][y] || tmp[x + 1][y] || tmp[x][y - 1] || tmp[x][y + 1])) {
                    for (int a = 0; a <= 2 * range; a++) {
                        for (int b = 0; b <= 2 * range; b++) {
                            binary_image[x - a][y - b] = binary_image[x - a][y - b] && !mask[a][b];
                        }
                    }
                }
            }
        }
        return binary_image;
    }

    /**
     * Boundaries of the image are done separately because there we have to check
     * for every mask pixel if we are still in the image and these checks cost time.
     */
    public static boolean[][] dilateAndErode(boolean[][] binary_image, boolean[][] mask1, boolean[][] mask2) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask1 == null || mask1.length % 2 == 0 || mask1[0].length != mask1.length ||
                mask2 == null || mask2.length % 2 == 0 || mask2[0].length != mask2.length) {
            System.err.println("FilamentSensor Calc.dilate_and_erode: " +
                    "Image and masks must exist. Masks must be quadratic " +
                    "and their side lengths must be an odd number of pixels.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length,
                range1 = mask1.length / 2,
                range2 = mask2.length / 2,
                width_plus = width + 2 * range1,
                height_plus = height + 2 * range1;

        boolean[][] tmp = new boolean[width_plus][height_plus];

        // Dilation
        for (int x = 0; x < width; x++) {
            if (binary_image[x][0]) {
                for (int a = 0; a <= 2 * range1; a++) {
                    for (int b = 0; b <= 2 * range1; b++) {
                        tmp[x + a][b] = mask1[a][b] || tmp[x + a][b];
                    }
                }
            }
            if (binary_image[x][height - 1]) {
                for (int a = 0; a <= 2 * range1; a++) {
                    for (int b = 0; b <= 2 * range1; b++) {
                        tmp[x + a][height - 1 + b] = mask1[a][b] || tmp[x + a][height - 1 + b];
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (binary_image[0][y]) {
                for (int a = 0; a <= 2 * range1; a++) {
                    for (int b = 0; b <= 2 * range1; b++) {
                        tmp[a][y + b] = mask1[a][b] || tmp[a][y + b];
                    }
                }
            }
            if (binary_image[width - 1][y]) {
                for (int a = 0; a <= 2 * range1; a++) {
                    for (int b = 0; b <= 2 * range1; b++) {
                        tmp[width - 1 + a][y + b] = mask1[a][b] || tmp[width - 1 + a][y + b];
                    }
                }
            }
        }

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (binary_image[x][y]) {
                    if (!binary_image[x - 1][y] || !binary_image[x + 1][y] ||
                            !binary_image[x][y - 1] || !binary_image[x][y + 1]) {
                        for (int a = 0; a <= 2 * range1; a++) {
                            for (int b = 0; b <= 2 * range1; b++) {
                                tmp[x + a][y + b] = mask1[a][b] || tmp[x + a][y + b];
                            }
                        }
                    } else {
                        tmp[x + range1 + 1][y + range1 + 1] = true;
                    }
                }
            }
        }

        // Copy dilated image to original
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                binary_image[x][y] = tmp[x + range1][y + range1];
            }
        }

        // Erosion
        int d = range1 - range2,
                xy_lo = range1 + range2,
                x_hi = width + d,
                y_hi = height + d;
        if (d < 0) {
            // Left and right boundary
            for (int x = 0; x < -d; x++) {
                for (int y = 0; y < height; y++) {
                    binary_image[x][y] = false;
                    binary_image[width - 1 - x][y] = false;
                }
            }
            // Upper and lower boundary
            for (int y = 0; y < -d; y++) {
                for (int x = 0; x < width; x++) {
                    binary_image[x][y] = false;
                    binary_image[x][height - 1 - y] = false;
                }
            }
        }
        for (int x = 0; x < width_plus; x++) {
            if (x == xy_lo) {
                x = x_hi;
            }
            for (int y = 0; y < height_plus; y++) {
                // Left and right boundary
                // If point is black but has a white neighbor, blacken everything inside the mask.
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width_plus - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height_plus - 1 && tmp[x][y + 1]))) {
                    int xd = x - d, yd = y - d;
                    for (int a = 0; a <= 2 * range2; a++) {
                        if (xd - a >= 0 && xd - a < width) {
                            for (int b = 0; b <= 2 * range2; b++) {
                                if (yd - b >= 0 && yd - b < height && mask2[a][b]) {
                                    binary_image[xd - a][yd - b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int y = 0; y < height_plus; y++) {
            if (y == xy_lo) {//possible infinite loop
                y = y_hi;
            }
            for (int x = 0; x < width_plus; x++) {
                // Upper and lower boundary
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width_plus - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height_plus - 1 && tmp[x][y + 1]))) {
                    int xd = x - d, yd = y - d;
                    for (int b = 0; b <= 2 * range2; b++) {
                        if (yd - b >= 0 && yd - b < height) {
                            for (int a = 0; a <= 2 * range2; a++) {
                                if (xd - a >= 0 && xd - a < width && mask2[a][b]) {
                                    binary_image[xd - a][yd - b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Interior
        for (int x = xy_lo; x < x_hi; x++) {
            for (int y = xy_lo; y < y_hi; y++) {
                if (!tmp[x][y] &&
                        (tmp[x - 1][y] || tmp[x + 1][y] || tmp[x][y - 1] || tmp[x][y + 1])) {
                    int xd = x - d, yd = y - d;
                    for (int a = 0; a <= 2 * range2; a++) {
                        for (int b = 0; b <= 2 * range2; b++) {
                            binary_image[xd - a][yd - b] = binary_image[xd - a][yd - b] && !mask2[a][b];
                        }
                    }
                }
            }
        }
        return binary_image;
    }

    public static boolean[][] open(boolean[][] binary_image, boolean[][] mask) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask == null || mask.length % 2 == 0 || mask[0].length != mask.length) {
            System.err.println("FilamentSensor Calc.open: " +
                    "Image and mask must exist. Mask must be quadratic " +
                    "and its side length must be an odd number of pixels.");
            return null;
        }

        return dilate(erode(binary_image, mask), mask);
    }

    public static boolean[][] erode(boolean[][] binary_image, boolean[][] mask) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask == null || mask.length % 2 == 0 || mask[0].length != mask.length) {
            System.err.println("FilamentSensor Calc.erode: " +
                    "Image and mask must exist. Mask must be quadratic " +
                    "and its side length must be an odd number of pixels.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length,
                range = mask.length / 2;

        boolean[][] tmp = new boolean[width][height], out = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tmp[x][y] = binary_image[x][y];
                out[x][y] = binary_image[x][y];
            }
        }

        for (int x = 0; x < width; x++) {
            if (x == 2 * range) {
                x = width - range;
            }
            for (int y = 0; y < height; y++) {
                // Left and right boundary
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height - 1 && tmp[x][y + 1]))) {
                    for (int a = 0; a <= 2 * range; a++) {
                        int x_a = x + a - range;
                        if (x_a >= 0 && x_a < width) {
                            for (int b = 0; b <= 2 * range; b++) {
                                int y_b = y + b - range;
                                if (y_b >= 0 && y_b < height && mask[a][b]) {
                                    out[x_a][y_b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (y == range) {
                y = height - range;
            }
            for (int x = 0; x < width; x++) {
                // Upper and lower boundary
                if (!tmp[x][y] &&
                        ((x > 0 && tmp[x - 1][y]) || (x < width - 1 && tmp[x + 1][y]) ||
                                (y > 0 && tmp[x][y - 1]) || (y < height - 1 && tmp[x][y + 1]))) {
                    for (int b = 0; b <= 2 * range; b++) {
                        int y_b = y + b - range;
                        if (y_b >= 0 && y_b < height) {
                            for (int a = 0; a <= 2 * range; a++) {
                                int x_a = x + a - range;
                                if (x_a >= 0 && x_a < width && mask[a][b]) {
                                    out[x_a][y_b] = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Interior
        for (int x = range; x < width - range; x++) {
            for (int y = range; y < height - range; y++) {
                if (!tmp[x][y] &&
                        (tmp[x - 1][y] || tmp[x + 1][y] || tmp[x][y - 1] || tmp[x][y + 1])) {
                    for (int a = 0; a <= 2 * range; a++) {
                        for (int b = 0; b <= 2 * range; b++) {
                            int x_a = x + a - range, y_b = y + b - range;
                            out[x_a][y_b] = out[x_a][y_b] && !mask[a][b];
                        }
                    }
                }
            }
        }
        return out;
    }

    public static boolean[][] grow(boolean[][] binary_image, boolean[][] mask, int times) {
        if (binary_image == null || binary_image.length == 0 || binary_image[0].length == 0 ||
                mask == null || mask.length != binary_image.length ||
                mask[0].length != binary_image[0].length) {
            System.err.println("FilamentSensor CellData.grow: " +
                    "Image and mask must exist and be of equal shape.");
            return null;
        }

        final int width = binary_image.length,
                height = binary_image[0].length;

        List<Point> boundary = new ArrayList<>();
        boolean[][] out = new boolean[width][height];

        // Find boundary and copy image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (binary_image[x][y]) {
                    out[x][y] = true;
                    if ((x > 0 && !binary_image[x - 1][y]) ||
                            (x < width - 1 && !binary_image[x + 1][y]) ||
                            (y > 0 && !binary_image[x][y - 1]) ||
                            (y < height - 1 && !binary_image[x][y + 1])) {
                        boundary.add(new Point(x, y));
                    }
                }
            }
        }

        for (int i = 0; i < times; i++) {
            grow4(out, boundary, mask);
            grow8(out, boundary, mask);
        }
        return out;
    }

    public static int[][] downsize(int[][] image, int factor) {
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("FilamentSensor CellData.downsize: " +
                    "Image must exist and be non-empty.");
            return null;
        }

        final int width = image.length / factor,
                height = image[0].length / factor,
                area = factor * factor;

        int[][] out = new int[width][height];

        for (int x = 0; x < width; x++) {
            int x0 = x * factor;
            for (int y = 0; y < height; y++) {
                int y0 = y * factor;
                double mean = 0;
                for (int dx = 0; dx < factor; dx++) {
                    for (int dy = 0; dy < factor; dy++) {
                        mean += image[x0 + dx][y0 + dy];
                    }
                }
                out[x][y] = (int) Math.round(mean / area);
            }
        }
        return out;
    }

    public static int[][] upsize(int[][] image, int factor, int new_width, int new_height) {
        if (factor == 1) {
            return image;
        }
        if ((image == null) || (image.length == 0) || (image[0].length == 0)) {
            System.err.println("FilamentSensor CellData.upsize: " +
                    "Image must exist and be non-empty.");
            return null;
        }
        if ((new_width < image.length * factor) || (new_height < image[0].length * factor)) {
            System.err.println("FilamentSensor CellData.upsize: " +
                    "Invalid image properties: " +
                    new_width + " < " + image.length * factor +
                    "? " + new_height + " < " + image[0].length * factor + "?");
            return null;
        }

        final int width = image.length,
                height = image[0].length;

        int[][] out = new int[new_width][new_height];

        for (int x = 0; x < width; x++) {
            int x0 = x * factor;
            for (int y = 0; y < height; y++) {
                int y0 = y * factor;
                for (int dx = 0; dx < factor; dx++) {
                    for (int dy = 0; dy < factor; dy++) {
                        out[x0 + dx][y0 + dy] = image[x][y];
                    }
                }
            }
        }
        return out;
    }

    private static void grow4(boolean[][] binary_image, List<Point> boundary, boolean[][] mask) {
        final int width = binary_image.length,
                height = binary_image[0].length;

        for (int i = boundary.size() - 1; i > -1; i--) {
            Point p = boundary.get(i);
            boundary.remove(p);
            int x = p.x, y = p.y;
            if (x > 0 && !binary_image[x - 1][y] && mask[x - 1][y]) {
                binary_image[x - 1][y] = true;
                boundary.add(new Point(x - 1, y));
            }
            if (x < width - 1 && !binary_image[x + 1][y] && mask[x + 1][y]) {
                binary_image[x + 1][y] = true;
                boundary.add(new Point(x + 1, y));
            }
            if (y > 0 && !binary_image[x][y - 1] && mask[x][y - 1]) {
                binary_image[x][y - 1] = true;
                boundary.add(new Point(x, y - 1));
            }
            if (y < height - 1 && !binary_image[x][y + 1] && mask[x][y + 1]) {
                binary_image[x][y + 1] = true;
                boundary.add(new Point(x, y + 1));
            }
        }
    }

    private static void grow8(boolean[][] binary_image, List<Point> boundary, boolean[][] mask) {
        final int width = binary_image.length,
                height = binary_image[0].length;

        for (int i = boundary.size() - 1; i > -1; i--) {
            Point p = boundary.get(i);
            boundary.remove(p);
            int x = p.x, y = p.y;
            if (x > 0 && !binary_image[x - 1][y] && mask[x - 1][y]) {
                binary_image[x - 1][y] = true;
                boundary.add(new Point(x - 1, y));
            }
            if (x > 0 && y > 0 && !binary_image[x - 1][y - 1] && mask[x - 1][y - 1]) {
                binary_image[x - 1][y] = true;
                boundary.add(new Point(x - 1, y));
            }
            if (x > 0 && y < height - 1 && !binary_image[x - 1][y + 1] && mask[x - 1][y + 1]) {
                binary_image[x - 1][y] = true;
                boundary.add(new Point(x - 1, y));
            }
            if (x < width - 1 && !binary_image[x + 1][y] && mask[x + 1][y]) {
                binary_image[x + 1][y] = true;
                boundary.add(new Point(x + 1, y));
            }
            if (x < width - 1 && y > 0 && !binary_image[x + 1][y - 1] && mask[x + 1][y - 1]) {
                binary_image[x + 1][y] = true;
                boundary.add(new Point(x + 1, y));
            }
            if (x < width - 1 && y < height - 1 && !binary_image[x + 1][y + 1] && mask[x + 1][y + 1]) {
                binary_image[x + 1][y] = true;
                boundary.add(new Point(x + 1, y));
            }
            if (y > 0 && !binary_image[x][y - 1] && mask[x][y - 1]) {
                binary_image[x][y - 1] = true;
                boundary.add(new Point(x, y - 1));
            }
            if (y < height - 1 && !binary_image[x][y + 1] && mask[x][y + 1]) {
                binary_image[x][y + 1] = true;
                boundary.add(new Point(x, y + 1));
            }
        }
    }

    private static double polynomial(final double[] coefficients, final double argument) {
        final int n = coefficients.length - 1;
        double out = coefficients[n];
        for (int i = n - 1; i >= 0; i--) {
            out *= argument;
            out += coefficients[i];
        }
        return out;
    }

    private static double[][] rotate2x2(double[][] matrix_2x2) {
        double tmp = (matrix_2x2[1][1] - matrix_2x2[0][0]) / (2 * matrix_2x2[0][1]);
        double y = tmp + Math.sqrt(1 + tmp * tmp);
        double x = 1 / Math.sqrt(1 + y * y);
        y *= x;
        return new double[][]{{x, -y}, {y, x}};
    }

    private static double[][] adjoin(double[][] matrix, double[][] matrix_2x2, int a, int b) {
        double[][] ad_matrix_2x2 = new double[][]{{matrix_2x2[0][0], matrix_2x2[1][0]},
                {matrix_2x2[0][1], matrix_2x2[1][1]}};
        return multiply(multiply(matrix, ad_matrix_2x2, a, b, true), matrix_2x2, a, b, false);
    }

    private static double[][] multiply(double[][] matrix, double[][] matrix_2x2, int a, int b, boolean sparse_left) {
        double[][] out = copy(matrix);
        for (int i = 0; i < matrix.length; i++) {
            if (sparse_left) {
                out[a][i] = matrix_2x2[0][0] * matrix[a][i] + matrix_2x2[0][1] * matrix[b][i];
                out[b][i] = matrix_2x2[1][0] * matrix[a][i] + matrix_2x2[1][1] * matrix[b][i];
            } else {
                out[i][a] = matrix[i][a] * matrix_2x2[0][0] + matrix[i][b] * matrix_2x2[1][0];
                out[i][b] = matrix[i][a] * matrix_2x2[0][1] + matrix[i][b] * matrix_2x2[1][1];
            }
        }
        return out;
    }

    private static double[][] copy(double[][] matrix) {
        double[][] copy = new double[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                copy[i][j] = matrix[i][j];
            }
        }
        return copy;
    }


    public static int[] getRange(@NotNull final int[][] image) {
        Objects.requireNonNull(image);
        final int[] out = new int[2];

        int max = 0;
        int min = Integer.MAX_VALUE;
        for (final int[] column : image) {
            for (final int pixel : column) {
                if (pixel > max) {
                    max = pixel;
                }
                if (pixel < min) {
                    min = pixel;
                }
            }
        }

        out[0] = min; // black
        out[1] = max; // white
        return out;
    }

    public static final boolean[][] maskCross = new boolean[][]{new boolean[]{false, true, false}, new boolean[]{true, true, true}, new boolean[]{false, true, false}};
    public static final boolean[][] maskSquare = new boolean[][]{new boolean[]{true, true, true}, new boolean[]{true, true, true}, new boolean[]{true, true, true}};


    public static List<IBinaryImage> largestObjectNew(@NotNull IBinaryImage binaryImage, int minimalArea, boolean[][] mask) {
        if (mask == null || mask.length < 3 || mask[0].length < 3)
            throw new IllegalArgumentException("mask has to be a boolean[3][3] minimum");
        boolean[][] bI = binaryImage.toBoolean();
        final int width = binaryImage.getWidth(), height = binaryImage.getHeight();
        Map<Point2D, Set<Point2D>> clusters = new HashMap<>();
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                if (bI[x][y]) {
                    Point2D p = new Point2D.Double(x, y);
                    int x0 = x - 1, x1 = x + 1;
                    int y0 = y - 1, y1 = y + 1;

                    clusters.computeIfAbsent(p, k -> new HashSet<>());
                    clusters.get(p).add(p);
                    //column0
                    if (x0 >= 0 && y0 >= 0 && bI[x0][y0] && mask[0][0]) {
                        //cell set
                        Point2D p0 = new Point2D.Double(x0, y0);
                        clusters.get(p).add(p0);
                        //top left of p (no need to add clusters.get(p0).add(p);
                    }
                    if (x0 >= 0 && bI[x0][y] && mask[0][1]) {
                        //cell set
                        Point2D p1 = new Point2D.Double(x0, y);
                        clusters.get(p).add(p1);
                        //left of p (no need to add clusters.get(p1).add(p);
                    }
                    if (x0 >= 0 && y1 < height && bI[x0][y1] && mask[0][2]) {
                        //cell set
                        Point2D p2 = new Point2D.Double(x0, y1);
                        clusters.get(p).add(p2);
                        //bottom left of p
                        clusters.put(p2, clusters.get(p));
                    }

                    //column1
                    if (y0 >= 0 && bI[x][y0] && mask[1][0]) {
                        //cell set
                        Point2D p3 = new Point2D.Double(x, y0);
                        clusters.get(p).add(p3);
                        //top of p(no need to add clusters.get(p3).add(p);
                    }
                    //x and y is not here because that is the current point
                    if (y1 < height && bI[x][y1] && mask[1][2]) {
                        //cell set
                        Point2D p5 = new Point2D.Double(x, y1);
                        clusters.get(p).add(p5);
                        //bottom of p
                        clusters.put(p5, clusters.get(p));
                    }

                    //column 2
                    if (x1 < width && y0 >= 0 && bI[x1][y0] && mask[2][0]) {
                        //cell set
                        Point2D p6 = new Point2D.Double(x1, y0);
                        clusters.get(p).add(p6);
                        //top right of p
                        clusters.put(p6, clusters.get(p));

                    }
                    if (x1 < width && bI[x1][y] && mask[2][1]) {
                        //cell set
                        Point2D p7 = new Point2D.Double(x1, y);
                        clusters.get(p).add(p7);
                        //right of p
                        clusters.put(p7, clusters.get(p));
                    }
                    if (x1 < width && y1 < height && bI[x1][y1] && mask[2][2]) {
                        //cell set
                        Point2D p8 = new Point2D.Double(x1, y1);
                        clusters.get(p).add(p8);
                        //right bottom of p
                        clusters.put(p8, clusters.get(p));
                    }


                }


            }


        List<IBinaryImage> shapes = clusters.values().stream().distinct().filter(points -> points.size() > minimalArea).map(points -> {
            IBinaryImage tmp = new BinaryImage(width, height);
            points.forEach(p -> tmp.setPixel((int) p.getX(), (int) p.getY()));
            return tmp;
        }).sorted(Comparator.comparingInt(IBinaryImage::getPixelSetCount)).collect(Collectors.toList());
        Collections.reverse(shapes);
        return shapes;
    }


    /**
     * creates a binary image for each object (sorted list, starting with largest area)
     *
     * @param binary_image binary image which should be split into areas
     * @param minimalArea  minimal area, everything smaller is not in the result
     * @return returns a sorted(by area, starting with largest area) list of BinaryImage's which represent objects in the input binary image
     * @throws NullPointerException on input==null || width==0 || height==0
     */
    public static List<IBinaryImage> largestObject(@NotNull IBinaryImage binary_image, int minimalArea) {

        Objects.requireNonNull(binary_image);
        if (binary_image.getWidth() == 0 || binary_image.getHeight() == 0)
            throw new IllegalArgumentException("Calc::largestObject binaryImage.size()==0");

        final int width = binary_image.getWidth();
        final int height = binary_image.getHeight();

        int[][] map = new int[width][height];
        Map<Integer, List<Point>> fragments = new HashMap<>();
        int index = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (binary_image.getPixel(x, y)) {
                    if (x > 0 && binary_image.getPixel(x - 1, y)) {
                        int label = map[x][y] = map[x - 1][y];
                        fragments.get(map[x][y]).add(new Point(x, y));
                        if (y > 0 && label != map[x][y - 1] && fragments.containsKey(map[x][y - 1])) {
                            java.util.List<Point> to_dissolve = fragments.remove(map[x][y - 1]);
                            for (Point p : to_dissolve) {
                                map[p.x][p.y] = label;
                            }
                            fragments.get(map[x][y]).addAll(to_dissolve);
                        }
                    } else if (y > 0 && binary_image.getPixel(x, y - 1)) {
                        map[x][y] = map[x][y - 1];
                        fragments.get(map[x][y]).add(new Point(x, y));
                    } else {
                        fragments.put(++index, new ArrayList<>());
                        map[x][y] = index;
                        fragments.get(map[x][y]).add(new Point(x, y));
                    }
                }
            }
        }

        int largest_fragment_size = 0;
        for (java.util.List<Point> fragment : fragments.values()) {
            if (largest_fragment_size < fragment.size()) {
                largest_fragment_size = fragment.size();
            }
        }


        //create binary image for each object
        List<IBinaryImage> shapes = fragments.values().stream().filter(points -> points.size() > minimalArea).map(points -> {
            IBinaryImage tmp = new BinaryImage(binary_image.getWidth(), binary_image.getHeight());
            points.forEach(p -> tmp.setPixel(p.x, p.y));
            return tmp;
        }).sorted(Comparator.comparingInt(IBinaryImage::getPixelSetCount)).collect(Collectors.toList());
        Collections.reverse(shapes);
        /*for (java.util.List<Point> fragment : fragments.values()) {
            if (fragment.size() < largest_fragment_size) {
                for (Point p : fragment) {
                    binary_image.clearPixel(p.x, p.y);
                }
            }
        }*/

        return shapes;
    }


    public static Point[][] latticeInit(int radius) {
        Point lines[][] = new Point[4 * radius][radius + 1];
        for (int direction = 0; direction < radius; direction++) {
            double slope = (double) direction / (double) radius;

            for (int x = 0; x <= radius; x++) {
                lines[direction][x] = new Point(x, (int) Math.round(slope * x));
            }
        }
        for (int direction = radius; direction < 3 * radius; direction++) {
            double epols = (2 * (double) radius - direction) / radius;
            for (int y = 0; y <= radius; y++) {
                lines[direction][y] = new Point((int) Math.round(epols * y), y);
            }
        }
        for (int direction = 3 * radius; direction < 4 * radius; direction++) {
            double slope = (direction - 4 * (double) radius) / radius;
            for (int x = 0; x <= radius; x++) {
                lines[direction][x] = new Point(x, (int) Math.round(slope * x));
            }
        }
        return lines;
    }


}
