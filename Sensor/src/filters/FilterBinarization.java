package filters;

import core.Calc;
import ij.process.ImageProcessor;
import javafx.beans.property.*;
import util.ArrayCopy;

import java.awt.*;

public class FilterBinarization implements IImageFilter, IFilterPrecalc {
    public static final String methodArea = "area";
    public static final String methodRod = "directions";


    public static final int HIGH = 255;
    public static final int LOW = 0;


    private IntegerProperty minMean;
    private DoubleProperty sigma;
    private DoubleProperty significance;
    private StringProperty method;

    private transient BooleanProperty changed;


    private transient int mean_range;
    private transient int gauss_range;
    private transient int step;
    private transient double sign;//adapted significance to current method (rod or area)


    private transient double[][] gauss;
    private transient double[] weights;
    private transient Point[][] lines;

    public FilterBinarization() {
        changed = new SimpleBooleanProperty();
        minMean = new SimpleIntegerProperty();
        sigma = new SimpleDoubleProperty();
        significance = new SimpleDoubleProperty();
        method = new SimpleStringProperty();

        minMeanProperty().addListener((observable, oldValue, newValue) -> changed.setValue(true));
        sigmaProperty().addListener((observable, oldValue, newValue) -> changed.setValue(true));
        significanceProperty().addListener((observable, oldValue, newValue) -> changed.setValue(true));
        methodProperty().addListener((observable, oldValue, newValue) -> changed.setValue(true));

    }


    public FilterBinarization(int minMean, double sigma, double significance, String method) {
        this();
        setMinMean(minMean);
        setSigma(sigma);
        setSignificance(significance);
        setMethod(method);


    }

    public BooleanProperty changedProperty() {
        return changed;
    }


    @Override
    public void preCalc() {
        sign = getSignificance();
        if (getMethod().equals(methodArea)) {
            sign = (Math.pow(getSignificance(), 2));
        }

        mean_range = (int) (Math.ceil(2 * getSigma()));
        gauss_range = (int) (Math.ceil(3 * getSigma()));
        step = (2 * mean_range + 1) * (2 * mean_range + 1) * getMinMean();

        gauss = Calc.gaussianMask(gauss_range, getSigma());

        weights = new double[256];

        if (methodArea.equals(getMethod())) {
            for (int i = 0; i < weights.length; i++) {
                if (i < 30) {
                    weights[i] = (double) i * i;
                } else {
                    weights[i] = (4. * (-i + 36 * Math.sqrt(i) - 100)) / 9.;
                    weights[i] *= weights[i];
                }
            }
        } else if (methodRod.equals(getMethod())) {
            lines = Calc.latticeInit(gauss_range);
            for (int i = 0; i < weights.length; i++) {
                if (i < 30) {
                    weights[i] = 1. / (double) i;
                } else {
                    weights[i] = 9. / (4. * (-i + 36 * Math.sqrt(i) - 100));
                }
            }
        }

    }


    public int[][] combinedMethod(int[][] image) {


        final int width = image.length,
                height = image[0].length;
        int[][] out = new int[width][height];

        for (int i = 0; i < gauss_range; i++) {
            for (int j = 0; j < height; j++) {
                out[i][j] = HIGH;
                out[width - 1 - i][j] = HIGH;
            }
        }
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < gauss_range; j++) {
                out[i][j] = HIGH;
                out[i][height - 1 - j] = HIGH;
            }
        }
        int[][] squares = null;
        if (methodArea.equals(getMethod())) {
            squares = new int[width][height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    squares[i][j] = image[i][j] * image[i][j];
                }
            }
        }


        for (int x = gauss_range; x < width - gauss_range; x++) {
            int bright = 0;
            for (int y = gauss_range; y < height - gauss_range; y++) {

                // Neighborhood mean thresholding
                if (y == gauss_range) {
                    bright = 0;
                    for (int a = -mean_range; a <= mean_range; a++) {
                        for (int b = -mean_range; b <= mean_range; b++) {
                            bright += image[x + a][y + b];
                        }
                    }
                } else {
                    for (int a = -mean_range; a <= mean_range; a++) {
                        bright += (image[x + a][y + mean_range] - image[x + a][y - mean_range - 1]);
                    }
                }

                if (bright < step) {
                    out[x][y] = HIGH;
                } else {
                    // Gaussian adaptive means
                    double gauss_mean = 0.0;
                    double sum_gauss = 0.0;

                    for (int a = -gauss_range; a <= gauss_range; a++) {
                        for (int b = -gauss_range; b <= gauss_range; b++) {
                            int i = x + a;
                            int j = y + b;
                            gauss_mean += (gauss[a + gauss_range][b + gauss_range] * image[i][j]);
                            sum_gauss += gauss[a + gauss_range][b + gauss_range];
                        }
                    }

                    gauss_mean /= sum_gauss;
                    if (image[x][y] <= gauss_mean) {
                        out[x][y] = HIGH;
                    } else {
                        // Statistical area filter
                        if (methodArea.equals(getMethod()))
                            out[x][y] = (area(x, y, squares, sum_gauss, gauss_mean)) ? HIGH : LOW;
                            // Statistical rod filter
                        else if (methodRod.equals(getMethod())) out[x][y] = (rod(image, x, y)) ? HIGH : LOW;
                    }
                }
            }
        }

        return out;
    }


    private boolean area(int x, int y, int[][] squares, double sum_gauss, double gauss_mean) {
        double gauss_squares = 0;
        for (int a = -gauss_range; a <= gauss_range; a++) {
            for (int b = -gauss_range; b <= gauss_range; b++) {
                int i = x + a;
                int j = y + b;

                gauss_squares += (squares[i][j] * gauss[a + gauss_range][b + gauss_range]);
            }
        }
        return (10000.0 * gauss_squares / sum_gauss <=
                10000.0 * gauss_mean * gauss_mean +
                        weights[(int) gauss_mean] * sign);

    }


    private boolean rod(int[][] image, int x, int y) {
        double mean = 0;
        double standard_deviation = 0;

        for (int ori = 0; ori < 4 * gauss_range; ori++) {
            double rod_mean = image[x][y];

            for (int dist = 1; dist <= gauss_range; dist++) {
                int dx = lines[ori][dist].x,
                        dy = lines[ori][dist].y;

                rod_mean += (image[x + dx][y + dy] + image[x - dx][y - dy]);
            }

            rod_mean /= (double) (2 * gauss_range + 1);
            standard_deviation += rod_mean * rod_mean;
            mean += rod_mean;
        }
        mean /= (double) (4 * gauss_range);
        standard_deviation = Math.sqrt(standard_deviation / (double) (4 * gauss_range) -
                mean * mean);

        return (100.0 * standard_deviation * weights[(int) mean] < sign);


    }


    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor image) {
        int[][] input = image.getIntArray();
        image.setIntArray(combinedMethod(ArrayCopy.copy(input)));
    }


    public int getMinMean() {
        return minMean.get();
    }

    public IntegerProperty minMeanProperty() {
        return minMean;
    }

    public void setMinMean(int minMean) {
        this.minMean.set(minMean);
    }

    public double getSigma() {
        return sigma.get();
    }

    public DoubleProperty sigmaProperty() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma.set(sigma);
    }

    public double getSignificance() {
        return significance.get();
    }

    public DoubleProperty significanceProperty() {
        return significance;
    }

    public void setSignificance(double significance) {
        this.significance.set(significance);
    }

    public String getMethod() {
        return method.get();
    }

    public StringProperty methodProperty() {
        return method;
    }

    public void setMethod(String method) {
        this.method.set(method);
    }

    @Override
    public String toString() {
        return "FilterBinarization{" +
                "minMean=" + minMean +
                ", sigma=" + sigma +
                ", significance=" + significance +
                ", method=" + method +
                '}';
    }
}
