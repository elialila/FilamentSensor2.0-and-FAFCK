package filters;

import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class FilterAutomaticThreshold implements IImageFilter {

    private ObjectProperty<AutoThresholder.Method> method;

    public FilterAutomaticThreshold() {
        method = new SimpleObjectProperty<>();
    }

    public AutoThresholder.Method getMethod() {
        return method.get();
    }

    public ObjectProperty<AutoThresholder.Method> methodProperty() {
        return method;
    }

    public void setMethod(AutoThresholder.Method method) {
        this.method.set(method);
    }

    @Override
    public boolean forceParallel() {
        return true;
    }

    @Override
    public void run(ImageProcessor image) {
        AutoThresholder thresholder = new AutoThresholder();
        image.threshold(thresholder.getThreshold(getMethod(), image.getHistogram()));
    }
}
