package filters;

import ij.process.ImageProcessor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static util.Annotations.*;

/**
 * for detailed explanation(transient, writeObject) see FilterCrossCorrelation
 */
@FilterUI()
public class FilterGauss implements IImageFilter, IFilterObservable {

    @FilterUIField(type = FilterUIType.slider, label = "Sigma")
    @Min(0)
    @Max(15)
    @Default("core.settings.Pre.fsigma")
    private transient DoubleProperty sigma;


    public FilterGauss() {
        sigma = new SimpleDoubleProperty();
    }

    public FilterGauss(double sigma) {
        this();
        setSigma(sigma);
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


    public double getSigma() {
        return sigma.get();
    }

    public DoubleProperty sigmaProperty() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma.set(sigma);
    }

    @Override
    public boolean forceParallel() {
        return true;
    }

    @Override
    public void run(ImageProcessor image) {
        image.blurGaussian(getSigma());
    }


    @Override
    public String toString() {
        return this.getClass().getName() + ":" + getSigma();
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
