package filters;


import ij.process.ImageProcessor;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import util.Annotations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * for detailed explanation(transient, writeObject) see FilterCrossCorrelation
 */
@Annotations.FilterUI()
public class FilterLaPlace implements IImageFilter, IFilterPrecalc, IFilterObservable {

    public static final int MaskTypeA = 0;
    public static final int MaskTypeB = 1;

    @Annotations.FilterUIField(type = Annotations.FilterUIType.slider, label = "Factor")
    @Annotations.Min(0)
    @Annotations.Max(5)
    @Annotations.Default("core.settings.Pre.lpfac")
    private transient DoubleProperty factor;

    @Annotations.FilterUIField(type = Annotations.FilterUIType.combobox, label = "Neighborhood")
    @Annotations.Values("filters.supplier.LaPlaceSupplier")//has to be implemented somehow
    @Annotations.Default("core.settings.Pre.lpmask")
    private transient IntegerProperty maskType;

    private float[] mask = null;

    public FilterLaPlace() {
        factor = new SimpleDoubleProperty();
        maskType = new SimpleIntegerProperty();
    }

    public FilterLaPlace(int type, double factor) {
        this();
        setFactor(factor);
        setMaskType(type);

    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeDouble(getFactor());
        s.writeInt(getMaskType());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        setFactor(s.readDouble());
        setMaskType(s.readInt());
        // set values in the same order as writeObject()
    }

    public double getFactor() {
        return factor.get();
    }

    public DoubleProperty factorProperty() {
        return factor;
    }

    public void setFactor(double factor) {
        this.factor.set(factor);
    }

    public int getMaskType() {
        return maskType.get();
    }

    public IntegerProperty maskTypeProperty() {
        return maskType;
    }

    public void setMaskType(int maskType) {
        this.maskType.set(maskType);
    }

    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor image) {
        if (mask == null) return;
        image.convolve(mask, 3, 3);
    }


    @Override
    public String toString() {
        return this.getClass().getName() + ":" + getFactor() + "," + getMaskType();
    }


    @Override
    public void preCalc() {
        double[] mFactors;
        switch (getMaskType()) {
            case MaskTypeA:
                mFactors = new double[]{1, getFactor(), 0};
                break;
            case MaskTypeB:
                mFactors = new double[]{1, 0, getFactor()};
                break;
            default:
                throw new IllegalArgumentException("FilterLaPlace - illegal MaskType");
        }
        float a = (float) -mFactors[2], b = (float) (-mFactors[2] - mFactors[1]),
                c = (float) (mFactors[0] + 4 * mFactors[1] + 8 * mFactors[2]);
        mask = new float[]{a, b, a, b, c, b, a, b, a};
    }

    public float[] getMask() {
        return mask;
    }

    public void setMask(float[] mask) {
        this.mask = mask;
    }


    @Override
    public void addListener(ChangeListener<Object> listener) {
        factorProperty().addListener(listener);
        maskTypeProperty().addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<Object> listener) {
        factorProperty().removeListener(listener);
        maskTypeProperty().removeListener(listener);
    }
}
