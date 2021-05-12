package filters;

import util.Annotations.NotNull;
import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.process.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import core.image.ImageWrapper;
import util.Annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * This class is based on ImageJ ContrastAdjuster
 */
public class ContrastAdjuster {

    private static final int AUTO_THRESHOLD = 5000;

    private int sliderRange = 256;

    private int previousImageID;
    private int previousType;
    private int previousSlice = 1;

    private double min, max;
    private double defaultMin, defaultMax;
    private int contrast, brightness;
    private boolean RGBImage;

    private IntegerProperty minSlider,
            maxSlider,
            contrastSlider,
            brightnessSlider;


    private int autoThreshold;

    private int channels = 7; // RGB


    private ImageWrapper wrapper;


    public ContrastAdjuster() {
        minSlider = new SimpleIntegerProperty();
        maxSlider = new SimpleIntegerProperty();
        contrastSlider = new SimpleIntegerProperty();
        brightnessSlider = new SimpleIntegerProperty();
    }

    public ContrastAdjuster(ImageWrapper wrapper) {
        this();
        this.wrapper = wrapper;
        setup(wrapper);
    }

    public void setup(ImageWrapper wrapper) {
        ImagePlus imp = wrapper.getImage();
        Roi roi = imp.getRoi();
        if (roi != null) roi.endPaste();
        ImageProcessor ip = imp.getProcessor();
        int type = imp.getType();
        int slice = imp.getCurrentSlice();
        RGBImage = type == ImagePlus.COLOR_RGB;
        if (imp.getID() != previousImageID || type != previousType || slice != previousSlice)
            setupNewImage(imp, ip);
        previousImageID = imp.getID();
        previousType = type;
        previousSlice = slice;
    }

    private void setupNewImage(ImagePlus imp, ImageProcessor ip) {
        Undo.reset();
        boolean newRGBImage = RGBImage && !((ColorProcessor) ip).caSnapshot();
        if (newRGBImage) {
            ip.snapshot();
            ((ColorProcessor) ip).caSnapshot(true);
        }
        double min2 = imp.getDisplayRangeMin();
        double max2 = imp.getDisplayRangeMax();
        if (newRGBImage) {
            min2 = 0.0;
            max2 = 255.0;
        }
        int bitDepth = imp.getBitDepth();
        if (bitDepth == 16 || bitDepth == 32) {
            imp.resetDisplayRange();
            defaultMin = imp.getDisplayRangeMin();
            defaultMax = imp.getDisplayRangeMax();
        } else {
            defaultMin = 0;
            defaultMax = 255;
        }
        setMinAndMax(imp, min2, max2);
        min = imp.getDisplayRangeMin();
        max = imp.getDisplayRangeMax();


        int valueRange = (int) (defaultMax - defaultMin);
        int newSliderRange = valueRange;
        if (newSliderRange > 640 && newSliderRange < 1280)
            newSliderRange /= 2;
        else if (newSliderRange >= 1280)
            newSliderRange /= 5;
        if (newSliderRange < 256) newSliderRange = 256;
        if (newSliderRange > 1024) newSliderRange = 1024;
        double displayRange = max - min;
        if (valueRange >= 1280 && valueRange != 0 && displayRange / valueRange < 0.25)
            newSliderRange *= 1.6666;

        if (newSliderRange != sliderRange) {
            sliderRange = newSliderRange;
            updateScrollBars(null, true);
        } else
            updateScrollBars(null, false);
        autoThreshold = 0;
    }

    private void setMinAndMax(ImagePlus imp, double min, double max) {
        boolean rgb = imp.getType() == ImagePlus.COLOR_RGB;
        if (channels != 7 && rgb)
            imp.setDisplayRange(min, max, channels);
        else
            imp.setDisplayRange(min, max);
    }


    //has to be changed -> remove scrollbar add integer properties instead
    private void updateScrollBars(IntegerProperty sb, boolean newRange) {
        if (sb == null || sb != contrastSlider) {
            double mid = sliderRange / 2;
            double c = ((defaultMax - defaultMin) / (max - min)) * mid;
            if (c > mid)
                c = sliderRange - ((max - min) / (defaultMax - defaultMin)) * mid;
            contrast = (int) c;
            if (contrastSlider != null) {
                if (newRange) {
                    //update range...?
                    //contrastSlider.setValues(contrast, 1, 0,  sliderRange);
                }
                contrastSlider.setValue(contrast);
            }
        }
        if (sb == null || sb != brightnessSlider) {
            double level = min + (max - min) / 2.0;
            double normalizedLevel = 1.0 - (level - defaultMin) / (defaultMax - defaultMin);
            brightness = (int) (normalizedLevel * sliderRange);
            if (newRange) {
                //brightnessSlider.setValues(brightness, 1, 0, sliderRange);
            }
            brightnessSlider.setValue(brightness);
        }
        if (minSlider != null && (sb == null || sb != minSlider)) {
            if (newRange) {
                //minSlider.setValues(scaleDown(min), 1, 0, sliderRange);
            }
            minSlider.setValue(scaleDown(min));
        }
        if (maxSlider != null && (sb == null || sb != maxSlider)) {
            if (newRange) {
                //    maxSlider.setValues(scaleDown(max), 1, 0, sliderRange);
            }
            maxSlider.setValue(scaleDown(max));
        }
    }

    private int scaleDown(double v) {
        if (v < defaultMin) v = defaultMin;
        if (v > defaultMax) v = defaultMax;
        return (int) ((v - defaultMin) * (sliderRange - 1.0) / (defaultMax - defaultMin));
    }

    /**
     * Restore image outside non-rectangular roi.
     */
    private void doMasking(ImagePlus imp, ImageProcessor ip) {
        ImageProcessor mask = imp.getMask();
        if (mask != null) {
            Rectangle r = ip.getRoi();
            if (mask.getWidth() != r.width || mask.getHeight() != r.height) {
                ip.setRoi(imp.getRoi());
                mask = ip.getMask();
            }
            ip.reset(mask);
        }
    }

    private void adjustMin(ImagePlus imp, ImageProcessor ip, double minvalue) {
        min = defaultMin + minvalue * (defaultMax - defaultMin) / (sliderRange - 1.0);
        if (max > defaultMax)
            max = defaultMax;
        if (min > max)
            max = min;
        setMinAndMax(imp, min, max);
        if (min == max)
            setThreshold(ip);
        if (RGBImage) doMasking(imp, ip);
        updateScrollBars(minSlider, false);
    }

    private void adjustMax(ImagePlus imp, ImageProcessor ip, double maxvalue) {
        max = defaultMin + maxvalue * (defaultMax - defaultMin) / (sliderRange - 1.0);
        if (min < defaultMin)
            min = defaultMin;
        if (max < min)
            min = max;
        setMinAndMax(imp, min, max);
        if (min == max)
            setThreshold(ip);
        if (RGBImage) doMasking(imp, ip);
        updateScrollBars(maxSlider, false);
    }

    private void adjustBrightness(ImagePlus imp, ImageProcessor ip, double bvalue) {
        double center = defaultMin + (defaultMax - defaultMin) * ((sliderRange - bvalue) / sliderRange);
        double width = max - min;
        min = center - width / 2.0;
        max = center + width / 2.0;
        setMinAndMax(imp, min, max);
        if (min == max)
            setThreshold(ip);
        if (RGBImage) doMasking(imp, ip);
        updateScrollBars(brightnessSlider, false);
    }

    private void adjustContrast(ImagePlus imp, ImageProcessor ip, int cvalue) {
        double slope;
        double center = min + (max - min) / 2.0;
        double range = defaultMax - defaultMin;
        double mid = sliderRange / 2;
        if (cvalue <= mid)
            slope = cvalue / mid;
        else
            slope = mid / (sliderRange - cvalue);
        if (slope > 0.0) {
            min = center - (0.5 * range) / slope;
            max = center + (0.5 * range) / slope;
        }
        setMinAndMax(imp, min, max);
        if (RGBImage) doMasking(imp, ip);
        updateScrollBars(contrastSlider, false);
    }

    private void reset(ImagePlus imp, ImageProcessor ip) {
        if (RGBImage)
            ip.reset();
        int bitDepth = imp.getBitDepth();
        if (bitDepth == 16 || bitDepth == 32) {
            imp.resetDisplayRange();
            defaultMin = imp.getDisplayRangeMin();
            defaultMax = imp.getDisplayRangeMax();
        }
        min = defaultMin;
        max = defaultMax;
        setMinAndMax(imp, min, max);
        updateScrollBars(null, false);
        autoThreshold = 0;
    }


    /**
     * @param idx       index of ImageProcessor(in Stack) starting from 0
     * @param fullStack
     */
    public void apply(int idx, boolean fullStack) {
        ImagePlus imp = wrapper.getImage();
        ImageProcessor ip = wrapper.getEntryList().get(idx).getProcessor();

        if (imp.isComposite())
            return;
        int bitDepth = imp.getBitDepth();
        if (bitDepth == 32) {
            return;
        }
        if (RGBImage)
            imp.unlock();
        if (!imp.lock())
            return;

        if (RGBImage) {
            if (imp.getStackSize() > 1)
                applyRGBStack(wrapper);
            else
                applyRGB(wrapper, idx);
            return;
        }

        int range = 256;
        if (bitDepth == 16) {
            range = 65536;
            int defaultRange = ImagePlus.getDefault16bitRange();
            if (defaultRange > 0)
                range = (int) Math.pow(2, defaultRange) - 1;
        }
        int tableSize = bitDepth == 16 ? 65536 : 256;
        int[] table = new int[tableSize];
        int min = (int) imp.getDisplayRangeMin();
        int max = (int) imp.getDisplayRangeMax();

        for (int i = 0; i < tableSize; i++) {
            if (i <= min)
                table[i] = 0;
            else if (i >= max)
                table[i] = range - 1;
            else
                table[i] = (int) (((double) (i - min) / (max - min)) * range);
        }
        ip.setRoi(imp.getRoi());
        if (imp.getStackSize() > 1 && !imp.isComposite()) {
            if (fullStack) {
                if (imp.getStack().isVirtual()) {
                    imp.unlock();
                    throw new RuntimeException("Apply does not work with virtual stacks. Use\nImage>Duplicate to convert to a normal stack.");
                }
                int current = imp.getCurrentSlice();
                ImageProcessor mask = imp.getMask();
                for (int i = 1; i <= imp.getStackSize(); i++) {
                    imp.setSlice(i);
                    ip = imp.getProcessor();
                    if (mask != null) ip.snapshot();
                    ip.applyTable(table);
                    ip.reset(mask);
                }
                imp.setSlice(current);

            } else {
                ip.snapshot();
                ip.applyTable(table);
                ip.reset(ip.getMask());

            }
        } else {
            ip.snapshot();
            ip.applyTable(table);
            ip.reset(ip.getMask());
        }
        reset(imp, ip);
        imp.changes = true;
        imp.unlock();
    }

    /**
     * @param wrapper
     * @param idx     index for the ImageProcessor starting from 0
     */
    private void applyRGB(ImageWrapper wrapper, int idx) {
        ImagePlus imp = wrapper.getImage();
        ImageProcessor ip = wrapper.getEntryList().get(idx).getProcessor();
        double min = imp.getDisplayRangeMin();
        double max = imp.getDisplayRangeMax();
        ip.setRoi(imp.getRoi());
        ip.reset();
        if (channels != 7)
            ((ColorProcessor) ip).setMinAndMax(min, max, channels);
        else
            ip.setMinAndMax(min, max);
        ip.reset(ip.getMask());
        imp.changes = true;
        previousImageID = 0;
        ((ColorProcessor) ip).caSnapshot(false);
        setup(wrapper);
        imp.deleteRoi();
    }

    private void applyRGBStack(ImageWrapper wrapper) {
        ImagePlus imp = wrapper.getImage();
        double min = imp.getDisplayRangeMin();
        double max = imp.getDisplayRangeMax();

        int current = imp.getCurrentSlice();
        int n = imp.getStackSize();

        ImageProcessor mask = imp.getMask();
        Rectangle roi = imp.getRoi() != null ? imp.getRoi().getBounds() : null;
        ImageStack stack = imp.getStack();
        for (int i = 1; i <= n; i++) {
            if (i != current) {
                ImageProcessor ip = stack.getProcessor(i);
                ip.setRoi(roi);
                if (mask != null) ip.snapshot();
                if (channels != 7)
                    ((ColorProcessor) ip).setMinAndMax(min, max, channels);
                else
                    ip.setMinAndMax(min, max);
                if (mask != null) ip.reset(mask);
            }
        }
        imp.setStack(null, stack);
        imp.setSlice(current);
        imp.changes = true;
        previousImageID = 0;
        setup(wrapper);
    }

    private void setThreshold(ImageProcessor ip) {
        if (!(ip instanceof ByteProcessor))
            return;
        if (ip.isInvertedLut())
            ip.setThreshold(max, 255, ImageProcessor.NO_LUT_UPDATE);
        else
            ip.setThreshold(0, max, ImageProcessor.NO_LUT_UPDATE);
    }

    private void autoAdjust(ImagePlus imp, ImageProcessor ip) {
        if (RGBImage)
            ip.reset();
        ImageStatistics stats = imp.getRawStatistics();
        int limit = stats.pixelCount / 10;
        int[] histogram = stats.histogram;
        if (autoThreshold < 10)
            autoThreshold = AUTO_THRESHOLD;
        else
            autoThreshold /= 2;
        int threshold = stats.pixelCount / autoThreshold;
        int i = -1;
        boolean found = false;
        int count;
        do {
            i++;
            count = histogram[i];
            if (count > limit) count = 0;
            found = count > threshold;
        } while (!found && i < 255);
        int hmin = i;
        i = 256;
        do {
            i--;
            count = histogram[i];
            if (count > limit) count = 0;
            found = count > threshold;
        } while (!found && i > 0);
        int hmax = i;
        Roi roi = imp.getRoi();
        if (hmax >= hmin) {
            if (RGBImage) imp.deleteRoi();
            min = stats.histMin + hmin * stats.binSize;
            max = stats.histMin + hmax * stats.binSize;
            if (min == max) {
                min = stats.min;
                max = stats.max;
            }
            setMinAndMax(imp, min, max);
            if (RGBImage && roi != null) imp.setRoi(roi);
        } else {
            reset(imp, ip);
            return;
        }
        updateScrollBars(null, false);
    }

    private void setMinAndMax(ImagePlus imp, ImageProcessor ip) {
        min = imp.getDisplayRangeMin();
        max = imp.getDisplayRangeMax();
        Calibration cal = imp.getCalibration();
        double minValue = cal.getCValue(min);
        double maxValue = cal.getCValue(max);

        minValue = cal.getRawValue(minValue);
        maxValue = cal.getRawValue(maxValue);

        if (imp.getType() == ImagePlus.GRAY16 && !cal.isSigned16Bit()) {
            reset(imp, ip);
            minValue = imp.getDisplayRangeMin();
            maxValue = imp.getDisplayRangeMax();
        }

        if (maxValue >= minValue) {
            min = minValue;
            max = maxValue;
            setMinAndMax(imp, min, max);
            updateScrollBars(null, false);
            if (RGBImage) doMasking(imp, ip);
        }
    }

    /**
     * @param idx       index of ImageProcessor(in stack) starting from 0
     * @param fullStack should the whole stack be processed
     * @param action    action which should be done
     */
    public void doUpdate(int idx, boolean fullStack, AdjusterAction action) {
        doUpdate(idx, fullStack, action, null);
    }


    /**
     * @param idx       index of ImageProcessor(in stack) starting from 0
     * @param fullStack should the whole stack be processed
     * @param action    action which should be done
     * @param callback  callback after finished processing
     */
    public void doUpdate(int idx, boolean fullStack, @NotNull AdjusterAction action, @Nullable Consumer<AdjusterAction> callback) {
        Objects.requireNonNull(action, "ContrastAdjuster::doUpdate() --- action == null");
        Objects.requireNonNull(wrapper, "ContrastAdjuster::doUpdate() --- wrapper==null");

        ImagePlus imp = wrapper.getImage();
        ImageProcessor ip = wrapper.getEntryList().get(idx).getProcessor();

        int minvalue = minSlider.get();
        int maxvalue = maxSlider.get();
        int bvalue = brightnessSlider.get();
        int cvalue = contrastSlider.get();

        minSlider.set(-1);
        maxSlider.set(-1);
        brightnessSlider.set(-1);
        contrastSlider.set(-1);

        if (RGBImage && !imp.lock()) {
            imp = null;
            return;
        }
        switch (action) {
            case reset:
                reset(imp, ip);
                break;
            case auto:
                autoAdjust(imp, ip);
                break;
            case set:
                setMinAndMax(imp, ip);
                break;
            case min:
                adjustMin(imp, ip, minvalue);
                break;
            case max:
                adjustMax(imp, ip, maxvalue);
                break;
            case brightness:
                adjustBrightness(imp, ip, bvalue);
                break;
            case contrast:
                adjustContrast(imp, ip, cvalue);
                break;
            default:
                throw new UnsupportedOperationException("ContrastAdjuster::doUpdate() --- No valid choice");
        }
        imp.updateChannelAndDraw();
        if (RGBImage)
            imp.unlock();
        if (callback != null) {
            System.out.println("ContrastAdjuster::doUpdate() --- action=" + action.name());
            callback.accept(action);
        }
    }

    public int getMinSlider() {
        return minSlider.get();
    }

    public IntegerProperty minSliderProperty() {
        return minSlider;
    }

    public int getMaxSlider() {
        return maxSlider.get();
    }

    public IntegerProperty maxSliderProperty() {
        return maxSlider;
    }

    public int getContrastSlider() {
        return contrastSlider.get();
    }

    public IntegerProperty contrastSliderProperty() {
        return contrastSlider;
    }

    public int getBrightnessSlider() {
        return brightnessSlider.get();
    }

    public IntegerProperty brightnessSliderProperty() {
        return brightnessSlider;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getBrightness() {
        return brightness;
    }

    public double getContrast() {
        return contrast;
    }


    public enum AdjusterAction {
        reset, auto, set, min, max, brightness, contrast;
    }


}



