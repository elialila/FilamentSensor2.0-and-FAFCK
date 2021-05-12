package filters;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;


public class FilterScale implements IStackFilter, IFilterObservable {

    private transient DoubleProperty scaleFactor;//0 - 1


    public FilterScale() {
        scaleFactor = new SimpleDoubleProperty();
    }

    public FilterScale(double scaleFactor) {
        this();
        setScaleFactor(scaleFactor);
    }


    public double getScaleFactor() {
        return scaleFactor.get();
    }

    public DoubleProperty scaleFactorProperty() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor.set(scaleFactor);
    }


    @Override
    public void run(ImagePlus imp) {
        final int newWidth = (int) (imp.getWidth() * getScaleFactor()), newHeight = (int) (imp.getHeight() * getScaleFactor());
        int nSlices = imp.getStackSize();
        if (nSlices > 1) {
            ImageStack stack1 = imp.getStack();
            ImageStack stack2 = new ImageStack(newWidth, newHeight);
            ImageProcessor ip1, ip2;
            for (int i = 1; i <= nSlices; i++) {
                ip1 = stack1.getProcessor(i);
                String label = stack1.getSliceLabel(i);
                //ip1.setInterpolationMethod(method);
                ip2 = ip1.resize(newWidth, newHeight, true);
                if (ip2 != null)
                    stack2.addSlice(label, ip2);
            }
            imp.setStack(stack2);
        } else {
            imp.setProcessor(imp.getProcessor().resize(newWidth, newHeight, true));
        }
    }

    @Override
    public void addListener(ChangeListener<Object> listener) {
        scaleFactorProperty().addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<Object> listener) {
        scaleFactorProperty().removeListener(listener);
    }
}
