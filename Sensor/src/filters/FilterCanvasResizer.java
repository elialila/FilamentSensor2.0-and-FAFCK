package filters;

import ij.*;
import ij.gui.*;
import ij.process.ImageProcessor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class FilterCanvasResizer implements IStackFilter {

    private transient IntegerProperty width;
    private transient IntegerProperty height;
    private transient BooleanProperty zeroFill;

    public FilterCanvasResizer() {
        width = new SimpleIntegerProperty();
        height = new SimpleIntegerProperty();
        zeroFill = new SimpleBooleanProperty();
    }

    public FilterCanvasResizer(int width, int height) {
        this();
        setWidth(width);
        setHeight(height);
        setZeroFill(true);
    }

    public boolean isZeroFill() {
        return zeroFill.get();
    }

    public BooleanProperty zeroFillProperty() {
        return zeroFill;
    }

    public void setZeroFill(boolean zeroFill) {
        this.zeroFill.set(zeroFill);
    }

    public int getWidth() {
        return width.get();
    }

    public IntegerProperty widthProperty() {
        return width;
    }

    public void setWidth(int width) {
        this.width.set(width);
    }

    public int getHeight() {
        return height.get();
    }

    public IntegerProperty heightProperty() {
        return height;
    }

    public void setHeight(int height) {
        this.height.set(height);
    }

    public ImageStack expandStack(ImageStack stackOld, int wNew, int hNew, int xOff, int yOff) {
        int nFrames = stackOld.getSize();
        ImageProcessor ipOld = stackOld.getProcessor(1);
        java.awt.Color colorBack = Toolbar.getBackgroundColor();

        ImageStack stackNew = new ImageStack(wNew, hNew, stackOld.getColorModel());
        ImageProcessor ipNew;

        for (int i = 1; i <= nFrames; i++) {
            IJ.showProgress((double) i / nFrames);
            ipNew = ipOld.createProcessor(wNew, hNew);
            if (isZeroFill())
                ipNew.setValue(0.0);
            else
                ipNew.setColor(colorBack);
            ipNew.fill();
            ipNew.insert(stackOld.getProcessor(i), xOff, yOff);
            stackNew.addSlice(stackOld.getSliceLabel(i), ipNew);
        }
        return stackNew;
    }

    public ImageProcessor expandImage(ImageProcessor ipOld, int wNew, int hNew, int xOff, int yOff) {
        ImageProcessor ipNew = ipOld.createProcessor(wNew, hNew);
        if (isZeroFill())
            ipNew.setValue(0.0);
        else
            ipNew.setColor(Toolbar.getBackgroundColor());
        ipNew.fill();
        ipNew.insert(ipOld, xOff, yOff);
        return ipNew;
    }

    @Override
    public void run(ImagePlus imp) {
        int wOld, hOld, wNew, hNew;
        boolean fIsStack = false;

        wOld = imp.getWidth();
        hOld = imp.getHeight();

        ImageStack stackOld = imp.getStack();
        if ((stackOld != null) && (stackOld.getSize() > 1))
            fIsStack = true;

        wNew = getWidth();
        hNew = getHeight();
        int iPos = 0;//top left
        Prefs.set("resizer.zero", isZeroFill());

        int xOff, yOff;
        int xC = (wNew - wOld) / 2;    // offset for centered
        int xR = (wNew - wOld);        // offset for right
        int yC = (hNew - hOld) / 2;    // offset for centered
        int yB = (hNew - hOld);        // offset for bottom

        switch (iPos) {
            case 0:    // TL
                xOff = 0;
                yOff = 0;
                break;
            case 1:    // TC
                xOff = xC;
                yOff = 0;
                break;
            case 2:    // TR
                xOff = xR;
                yOff = 0;
                break;
            case 3: // CL
                xOff = 0;
                yOff = yC;
                break;
            case 4: // C
                xOff = xC;
                yOff = yC;
                break;
            case 5:    // CR
                xOff = xR;
                yOff = yC;
                break;
            case 6: // BL
                xOff = 0;
                yOff = yB;
                break;
            case 7: // BC
                xOff = xC;
                yOff = yB;
                break;
            case 8: // BR
                xOff = xR;
                yOff = yB;
                break;
            default: // center
                xOff = xC;
                yOff = yC;
                break;
        }

        if (fIsStack) {
            ImageStack stackNew = expandStack(stackOld, wNew, hNew, xOff, yOff);
            imp.setStack(null, stackNew);
        } else {
            ImageProcessor newIP = expandImage(imp.getProcessor(), wNew, hNew, xOff, yOff);
            imp.setProcessor(null, newIP);
        }
        Overlay overlay = imp.getOverlay();
        if (overlay != null)
            overlay.translate(xOff, yOff);
    }
}
