package filters;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import util.Annotations;
import util.Annotations.Nullable;
import core.Calc;
import ij.process.ImageProcessor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import core.image.IBinaryImage;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * for detailed explanation(transient, writeObject) see FilterCrossCorrelation
 */
@Annotations.FilterUI()
public class FilterLineGauss implements IImageFilterAreaDependant, IFilterPrecalc, IFilterObservable {

    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "Sigma")
    @Annotations.Min(0)
    @Annotations.Max(15)
    @Annotations.Default("core.settings.Pre.line_sigma")
    private transient DoubleProperty sigma;

    @Annotations.FilterUIField(type = Annotations.FilterUIType.checkbox, label = "Area(set) or ExtArea(not set)")
    private transient BooleanProperty areaOrExtArea;


    private int range;
    private double[][] mask;
    private Point[][] lines;

    public FilterLineGauss() {
        sigma = new SimpleDoubleProperty();
        areaOrExtArea = new SimpleBooleanProperty(false);
    }

    public FilterLineGauss(double sigma) {
        this();
        setSigma(sigma);
        preCalc();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeDouble(getSigma());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        setSigma(s.readDouble());
        // set values in the same order as writeObject()
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public double[][] getMask() {
        return mask;
    }

    public void setMask(double[][] mask) {
        this.mask = mask;
    }

    public Point[][] getLines() {
        return lines;
    }

    public void setLines(Point[][] lines) {
        this.lines = lines;
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


    public void preCalc() {
        final double m_sigma = getSigma();
        range = (int) Math.ceil(3 * m_sigma);
        mask = Calc.gaussianQuarterMask(range, m_sigma);
        lines = Calc.latticeInit(range);
    }


    @Override
    public void run(ImageProcessor image, @Nullable IBinaryImage m_cell_area) {
        final int width = image.getWidth(), height = image.getHeight();
        int[][] img = image.getIntArray();
        int[][] imgExtended = new int[width + 2 * getRange()][height + 2 * getRange()];
        for (int i = range; i < width + range; i++) {
            for (int j = range; j < height + range; j++) {
                imgExtended[i][j] = img[i - range][j - range];
            }
        }
        int tmp[][] = new int[width][height];
        for (int i = 0; i < img.length; i++) {
            System.arraycopy(img[i], 0, tmp[i], 0, img[i].length);
        }
        // In case cell area is given as null, apply filter everywhere.
        //previous:x=range;x<width-range - analog mit y
        //copied image to larger array, now none of the original image is cropped
        for (int x = range; x < width + range; x++) {
            for (int y = range; y < height + range; y++) {
                if (m_cell_area == null || m_cell_area.getPixel(x - range, y - range)) {
                    double max_ori_mean = -1;
                    for (int ori = 0; ori < 4 * range; ori++) {
                        double mean = imgExtended[x][y] * mask[0][0], gauss_sum = mask[0][0];
                        for (int dist = 1; dist <= range; dist++) {
                            int dx = lines[ori][dist].x, dy = lines[ori][dist].y,
                                    mx = Math.abs(dx), my = Math.abs(dy);

                            mean += (imgExtended[x + dx][y + dy] + imgExtended[x - dx][y - dy]) * mask[mx][my];
                            gauss_sum += 2 * mask[mx][my];
                        }
                        mean /= gauss_sum;
                        if (mean > max_ori_mean) {
                            max_ori_mean = mean;
                        }
                    }
                    tmp[x - range][y - range] = (max_ori_mean >= 0 ? (int) max_ori_mean : imgExtended[x][y]);
                }
            }
        }
        image.setIntArray(tmp);
    }

    @Override
    public boolean isAreaOrExtArea() {
        return areaOrExtArea.get();
    }

    public BooleanProperty areaOrExtAreaProperty() {
        return areaOrExtArea;
    }

    public void setAreaOrExtArea(boolean areaOrExtArea) {
        this.areaOrExtArea.set(areaOrExtArea);
    }

    @Override
    public void addListener(ChangeListener<Object> listener) {
        sigmaProperty().addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<Object> listener) {
        sigmaProperty().removeListener(listener);
    }
}
