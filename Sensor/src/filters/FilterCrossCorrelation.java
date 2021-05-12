package filters;

import core.Calc;
import core.Const;
import ij.process.ImageProcessor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import util.Annotations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Needs custom Serialization (javafx Properties aren't serializeable
 * Default Constructor,Getters and Setters of attributes are needed for XMLEncoder/XMLDecoder
 * XMLEncoder/XMLDecoder are not dependant on custom serialization
 * but if ObjectOutputStream/ObjectInputStream is used -> custom serialization is needed
 * ObjectOutputStream/InputStream does not need Getter/Setter.
 */
@Annotations.FilterUI()
public class FilterCrossCorrelation implements IImageFilter, IFilterPrecalc, IFilterObservable {

    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "Zero(%)")
    @Annotations.Min(0)
    @Annotations.Max(100)
    @Annotations.Default("core.settings.Pre.corr_zero")
    private transient DoubleProperty zeroCrossing;//javafx Properties aren't serializable -> transient: this way the serializer will ignore it

    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "Size")
    @Annotations.Min(0)
    @Annotations.Max(15)
    @Annotations.NumberFormat("Integer")
    @Annotations.Default("core.settings.Pre.corr_mask_size")
    private transient DoubleProperty maskSize;


    private double[][][] m_masks;
    public static final String TYPE = "X";


    private double[] factor1;
    private double[] factor2;

    private int range_horizontal;
    private int range_vertical;
    private double area;


    public FilterCrossCorrelation() {
        zeroCrossing = new SimpleDoubleProperty();
        maskSize = new SimpleDoubleProperty();
    }

    public FilterCrossCorrelation(double zeroCrossing, double maskSize) {
        this();
        setMaskSize(maskSize);
        setZeroCrossing(zeroCrossing);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeDouble(getZeroCrossing());
        s.writeDouble(getMaskSize());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        setZeroCrossing(s.readDouble());
        setMaskSize(s.readDouble());
        // set values in the same order as writeObject()
    }


    public double getZeroCrossing() {
        return zeroCrossing.get();
    }

    public DoubleProperty zeroCrossingProperty() {
        return zeroCrossing;
    }

    public void setZeroCrossing(double zeroCrossing) {
        this.zeroCrossing.set(zeroCrossing);
    }

    public double getMaskSize() {
        return maskSize.get();
    }

    public DoubleProperty maskSizeProperty() {
        return maskSize;
    }

    public void setMaskSize(double maskSize) {
        this.maskSize.set(maskSize);
    }


    public void preCalc() throws IllegalArgumentException {
        m_masks = FilterCrossCorrelation.makeMasks(1, (int) getMaskSize());

        range_horizontal = (m_masks[0].length - 1) / 2;
        range_vertical = (m_masks[0][0].length - 1) / 2;
        area = (2 * range_horizontal + 1) * (2 * range_vertical + 1);


        // Precalculate prefactors from masks to save time
        factor1 = new double[m_masks.length];
        factor2 = new double[m_masks.length];
        for (int m = 0; m < m_masks.length; m++) {
            double sum = 0.0, square_sum = 0.0;
            for (int a = 0; a <= 2 * range_horizontal; a++) {
                for (int b = 0; b <= 2 * range_vertical; b++) {
                    sum += m_masks[m][a][b];
                    square_sum += m_masks[m][a][b] * m_masks[m][a][b];
                }
            }
            factor1[m] = area / Math.sqrt(area * square_sum - sum * sum);
            factor2[m] = sum / Math.sqrt(area * square_sum - sum * sum);
        }

    }


    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor imp) {


        if (imp == null || m_masks == null || imp.getWidth() <= 0 || imp.getHeight() <= 0 ||
                m_masks.length <= 0 || m_masks[0].length <= 0 || m_masks[0][0].length <= 0 ||
                imp.getWidth() < m_masks[0].length || imp.getHeight() < m_masks[0][0].length ||
                m_masks[0].length % 2 == 0 || m_masks[0][0].length % 2 == 0) {
            return;
        }

        final int width = imp.getWidth(),
                height = imp.getHeight(),
                n_masks = m_masks.length;


        final int[][] image = imp.getIntArray();

        //****************Copy Array****************************************************
        int[][] out = new int[image.length][];
        for (int i = 0; i < image.length; i++) {
            int[] aMatrix = image[i];
            int aLength = aMatrix.length;
            out[i] = new int[aLength];
            System.arraycopy(aMatrix, 0, out[i], 0, aLength);
        }
        //******************************************************************************

        // Precalculate matrix of squares to save time
        double[][] squares = new double[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                squares[i][j] = image[i][j] * image[i][j];
            }
        }


        // Edges of image remain unchanged
        /*for (int i = 0; i < width; i++) {
            for (int j = 0; j < range_vertical; j++) {
                out[i][j] = image[i][j];
                out[i][height - j - 1] = image[i][height - j - 1];
            }
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < range_horizontal; i++) {
                out[i][j] = image[i][j];
                out[width - i - 1][j] = image[width - i - 1][j];
            }
        }*/


        // Calculate the maximum cross correlation for each point.
        double total_min = Double.MAX_VALUE;
        double total_max = Double.MIN_VALUE;
        double first_sum = 0.0;
        double first_square_sum = 0.0;
        for (int a = 0; a <= 2 * range_horizontal; a++) {
            for (int b = 0; b <= 2 * range_vertical; b++) {
                first_sum += image[a][b];
                first_square_sum += squares[a][b];
            }
        }

        for (int i = range_horizontal; i < width - range_horizontal; i++) {
            if (i > range_horizontal) {
                for (int b = 0; b <= 2 * range_vertical; b++) {
                    first_sum += (image[i + range_horizontal][b] -
                            image[i - range_horizontal - 1][b]);
                    first_square_sum += (squares[i + range_horizontal][b] -
                            squares[i - range_horizontal - 1][b]);
                }
            }
            double window_sum = first_sum;
            double window_square_sum = first_square_sum;

            for (int j = range_vertical; j < height - range_vertical; j++) {
                if (j > range_vertical) {
                    for (int a = -range_horizontal; a <= range_horizontal; a++) {
                        window_sum += (image[i + a][j + range_vertical] -
                                image[i + a][j - range_vertical - 1]);
                        window_square_sum += (squares[i + a][j + range_vertical] -
                                squares[i + a][j - range_vertical - 1]);
                    }
                }

                double max = Double.MIN_VALUE;
                if (window_square_sum == 0.0) {
                    out[i][j] = 0;
                    continue;
                }

                double factor = 1 / Math.sqrt(area * window_square_sum - (double) window_sum * window_sum);
                for (int m = 0; m < n_masks; m++) {
                    double cross_sum = 0.0;
                    for (int a = -range_horizontal; a <= range_horizontal; a++) {
                        for (int b = -range_vertical; b <= range_vertical; b++) {
                            cross_sum += image[i + a][j + b] *
                                    m_masks[m][range_horizontal + a][range_vertical + b];
                        }
                    }
                    double correlation = factor * (factor1[m] * cross_sum - factor2[m] * window_sum);
                    if (correlation > max) {
                        max = correlation;
                    }
                }
                if (max < total_min) {
                    total_min = max;
                }
                if (max > total_max) {
                    total_max = max;
                }

                out[i][j] = (int) (Const.M * max);
            }
        }

        total_min += getZeroCrossing() * (total_max + total_min);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                out[i][j] = (int) (255 * (out[i][j] / Const.MF - total_min) / (total_max - total_min));
                if (out[i][j] < 0) {
                    out[i][j] = 0;
                }
            }
        }

        imp.setIntArray(out);

    }


    public static double[][][] makeMasks(int widths, int size) throws IllegalArgumentException {
        if (size <= 0 || widths <= 0) {
            throw new IllegalArgumentException("size||widths==0");
        }

        int range = size + widths;
        double[][][] temp = new double[4 * size * widths][2 * range + 1][2 * range + 1];
        double sigma = 0.5;
        double[][] gauss = Calc.gaussianMask(widths, sigma);

        for (int w = 0; w < widths; w++) {
            for (int dir = 0; dir < size; dir++) {
                double slope = (double) dir / (double) size;

                for (int x = -range; x <= range; x++) {
                    temp[dir + 4 * w * size][range + x][range + (int) Math.round(slope * x)] = 1;
                }
                temp[dir + 4 * w * size] = convolve(convolve(temp[dir + 4 * w * size], Const.CIRCLE[w + 1]), gauss);
            }

            for (int dir = size; dir < 3 * size; dir++) {
                double epols = (2 * (double) size - dir) / size;
                for (int y = -range; y <= range; y++) {
                    temp[dir + 4 * w * size][range + (int) Math.round(epols * y)][range + y] = 1;
                }
                temp[dir + 4 * w * size] = convolve(convolve(temp[dir + 4 * w * size], Const.CIRCLE[w + 1]), gauss);
            }

            for (int dir = 3 * size; dir < 4 * size; dir++) {
                double slope = (dir - 4 * (double) size) / size;
                for (int x = -range; x <= range; x++) {
                    temp[dir + 4 * w * size][range + x][range + (int) Math.round(slope * x)] = 1;
                }
                temp[dir + 4 * w * size] = convolve(convolve(temp[dir + 4 * w * size], Const.CIRCLE[w + 1]), gauss);
            }
        }
        double[][][] out = new double[4 * size * widths][2 * size + 1][2 * size + 1];
        for (int n = 0; n < 4 * size * widths; n++) {
            for (int x = 0; x <= 2 * size; x++) {
                for (int y = 0; y <= 2 * size; y++) {
                    out[n][x][y] = temp[n][x + widths][y + widths];
                }
            }
        }
        return out;
    }

    private static double[][] convolve(double[][] image, double[][] mask) throws IllegalArgumentException {
        if (image.length <= 0 || image[0].length <= 0 || mask.length <= 0 || mask[0].length <= 0 ||
                image.length < mask.length || image[0].length < mask[0].length ||
                mask.length % 2 == 0 || mask[0].length % 2 == 0) {
            throw new IllegalArgumentException("FilterCrossCorrelation.convole() params wrong");
        }

        int width = image.length;
        int height = image[0].length;
        int range_horizontal = (mask.length - 1) / 2;
        int range_vertical = (mask[0].length - 1) / 2;

        double out[][] = new double[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < range_vertical; j++) {
                out[i][j] = image[i][j];
                out[i][height - j - 1] = image[i][height - j - 1];
            }
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < range_horizontal; i++) {
                out[i][j] = image[i][j];
                out[height - i - 1][j] = image[height - i - 1][j];
            }
        }
        for (int i = range_horizontal; i < width - range_horizontal; i++) {
            for (int j = range_vertical; j < height - range_vertical; j++) {
                for (int a = -range_horizontal; a <= range_horizontal; a++) {
                    for (int b = -range_vertical; b <= range_vertical; b++) {
                        out[i][j] += mask[a + range_horizontal][b + range_vertical] * image[i + a][j + b];
                    }
                }
            }
        }
        return out;
    }


    public double[][][] getM_masks() {
        return m_masks;
    }

    public void setM_masks(double[][][] m_masks) {
        this.m_masks = m_masks;
    }

    public double[] getFactor1() {
        return factor1;
    }

    public void setFactor1(double[] factor1) {
        this.factor1 = factor1;
    }

    public double[] getFactor2() {
        return factor2;
    }

    public void setFactor2(double[] factor2) {
        this.factor2 = factor2;
    }

    public int getRange_horizontal() {
        return range_horizontal;
    }

    public void setRange_horizontal(int range_horizontal) {
        this.range_horizontal = range_horizontal;
    }

    public int getRange_vertical() {
        return range_vertical;
    }

    public void setRange_vertical(int range_vertical) {
        this.range_vertical = range_vertical;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }


    @Override
    public void addListener(ChangeListener<Object> listener) {
        maskSizeProperty().addListener(listener);
        zeroCrossingProperty().addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<Object> listener) {
        maskSizeProperty().removeListener(listener);
        zeroCrossingProperty().removeListener(listener);
    }
}
