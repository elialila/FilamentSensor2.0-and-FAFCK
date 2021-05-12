package filters;

import ij.process.ImageProcessor;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class FilterManualThreshold implements IImageFilter {

    private IntegerProperty threshold;

    public FilterManualThreshold() {
        threshold = new SimpleIntegerProperty();
    }

    public int getThreshold() {
        return threshold.get();
    }

    public IntegerProperty thresholdProperty() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold.set(threshold);
    }

    @Override
    public boolean forceParallel() {
        return true;
    }

    @Override
    public void run(ImageProcessor image) {
        image.threshold(getThreshold());

    }
}
