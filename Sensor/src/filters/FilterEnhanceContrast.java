package filters;

import ij.process.ImageProcessor;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import util.Annotations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * for detailed explanation(transient, writeObject) see FilterCrossCorrelation
 */
@Annotations.FilterUI()
public class FilterEnhanceContrast implements IImageFilter, IFilterObservable {
    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "Black Value")
    @Annotations.Min(0)
    @Annotations.Max(255)
    @Annotations.NumberFormat("Integer")
    @Annotations.Default("0")
    private transient IntegerProperty autoBlack;
    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "White Value")
    @Annotations.Min(0)
    @Annotations.Max(255)
    @Annotations.NumberFormat("Integer")
    @Annotations.Default("255")
    private transient IntegerProperty autoWhite;


    public FilterEnhanceContrast() {
        autoBlack = new SimpleIntegerProperty(-1);
        autoWhite = new SimpleIntegerProperty(-1);
    }

    public FilterEnhanceContrast(int autoBlack, int autoWhite) {
        this();
        if (autoBlack > 0 && autoWhite > 0) {
            setAutoWhite(autoWhite);
            setAutoBlack(autoBlack);
        }
    }


    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(getAutoBlack());
        s.writeInt(getAutoWhite());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        setAutoBlack(s.readInt());
        setAutoWhite(s.readInt());
        // set values in the same order as writeObject()
    }


    public int getAutoBlack() {
        return autoBlack.get();
    }

    public IntegerProperty autoBlackProperty() {
        return autoBlack;
    }

    public void setAutoBlack(int autoBlack) {
        this.autoBlack.set(autoBlack);
    }

    public int getAutoWhite() {
        return autoWhite.get();
    }

    public IntegerProperty autoWhiteProperty() {
        return autoWhite;
    }

    public void setAutoWhite(int autoWhite) {
        this.autoWhite.set(autoWhite);
    }

    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor image) {
        //calculate auto black/white
        int aBlack = (getAutoBlack() == -1) ? (int) image.getMin() : getAutoBlack();
        int aWhite = (getAutoWhite() == -1) ? (int) image.getMax() : getAutoWhite();

        int[][] img = image.getIntArray();
        HistogramMethodsLegacy contrast = new HistogramMethodsLegacy(img, aBlack, aWhite);
        final int m_auto_black = contrast.black();
        final int m_auto_white = contrast.white();

        for (int w = 0; w < image.getWidth(); w++) {
            for (int h = 0; h < image.getHeight(); h++) {
                double bw_scale = 255 / ((double) (m_auto_white - m_auto_black));
                int pixel = img[w][h];
                pixel = (int) Math.round(bw_scale * (pixel - m_auto_black));
                img[w][h] = (pixel < 0 ? 0 : (pixel > 255) ? 255 : pixel);
            }
        }
        image.setIntArray(img);
    }


    @Override
    public String toString() {
        return this.getClass().getName() + ":" + getAutoBlack() + "," + getAutoWhite();
    }


    @Override
    public void addListener(ChangeListener<Object> listener) {
        autoBlackProperty().addListener(listener);
        autoWhiteProperty().addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<Object> listener) {
        autoBlackProperty().removeListener(listener);
        autoWhiteProperty().removeListener(listener);
    }
}
