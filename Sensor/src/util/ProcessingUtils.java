package util;

import core.*;
import filters.*;
import javafx.beans.property.DoubleProperty;
import core.calculation.MeanBrightnessCalc;
import core.cell.CellPlugin;
import core.cell.ShapeContainer;
import core.image.IBinaryImage;
import core.image.ImageDependency;
import core.image.ImageWrapper;
import core.settings.*;
import core.tracers.CurveTracer;
import core.tracers.LineSensor;
import core.tracers.Tracer;
import core.FilterQueue;
import util.Annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 Wraps up the basic processing methods for convenience and easier use
 */
public class ProcessingUtils {


    public static void initializePreprocessing(ImageWrapper orig, double scale, ImageWrapper target) throws ExecutionException, InterruptedException {
        orig.scale(scale);
        //if it was scaled, wait for worker
        if (orig.getWorker() != null && !orig.getWorker().isDone()) {
            orig.getWorker().get();//block until done otherwise the next step will result in an error or other unexpected behaviour
        }
        orig.cloneData(target);
    }

    public static void preProcess(ImageWrapper pre, FilterQueue filterQueue, Consumer<Float> reporter) {
        filterQueue.run(pre, reporter);
        pre.notifyListeners();
    }

    public static void lineSensor(ImageWrapper pre, ImageWrapper line, FilterQueue filterQueue, Consumer<Float> reporter) {
        if (pre != line) pre.cloneData(line);//check hash if its the same object do not clone
        filterQueue.run(line, reporter);
        line.notifyListeners();
    }


    /**
     * This method is just for showing the filaments, there is no interior calculation, no order parameters and so on.
     *
     * @param line
     * @param parameters
     */
    public static void filamentSensor(ImageWrapper line, Settings parameters) {
        //for debugging stream
        MixedUtils.getStream(line.getEntryList(), false).forEach(entry -> {
            try {
                Tracer tracer = null;
                if (parameters.getValueAsBoolean(Trace.curve)) {
                    tracer = new CurveTracer();
                } else {
                    tracer = new LineSensor();
                }
                //System.out.println("ImageWrapper::scanFilaments --- start " + entry.getPath());
                ShapeContainer shape = entry.getShape();
                IBinaryImage tmpBin = null;
                if (shape != null && shape.getSelectedArea() != null) tmpBin = shape.getSelectedArea().getBinaryImage();
                IBinaryImage interior = entry.getDataFilament().scanFilaments(entry.getProcessor(), tmpBin, parameters, tracer);
                entry.getOrientationFieldContainer().setOrientationField(tracer.getOrientationField());
                if (parameters.getValue(Trace.split) * parameters.getValue(Trace.curve) > 0) {
                    entry.getDataFilament().splitToLinear(parameters.getValue(Trace.step)); //10 fixed value before, now depending on parameter
                }

                if (tmpBin != null && interior != null) {
                    entry.getInteriorContainer().setInteriorData(MeanBrightnessCalc.areaAndMeanBrightness(interior, tmpBin, entry.getProcessor()));
                    if (!interior.isInMemoryState()) interior.enterMemoryState();
                    entry.getInteriorContainer().setInterior(interior);
                }

                //currently only for selected area orientation field is calculated
                //foreach shape calculate orientation field
                //entry.getShape().getAreas().forEach(cellShape->
                //        CellPlugin.calculateOrderParameter(entry.getDataFilament().getTracer().getOrientationField(),cellShape)
                //);
                if (entry.getShape().getSelectedArea() != null) {
                    CellPlugin.calculateOrderParameter(entry.getOrientationFieldContainer().getOrientationField(), entry.getShape().getSelectedArea());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public static FilterQueue getDefaultPreprocessingFilterQueue(Settings dp) {
        //gauss-contrast-laplace-linegauss-gause-contrast =benjamin default

        FilterGauss gauss = new FilterGauss();
        gauss.setSigma((double) dp.getValue(Pre.fsigma) / 10);

        FilterGauss gauss2 = new FilterGauss();
        gauss2.setSigma((double) dp.getValue(Pre.fsigma) / 10);


        FilterLaPlace laPlace = new FilterLaPlace();
        laPlace.setMaskType(Const.makeNeighborHoodMap().get("8 neighbor"));
        laPlace.setFactor((double) dp.getValue(Pre.lpfac) / 10);

        FilterEnhanceContrast enhanceContrast = new FilterEnhanceContrast();
        enhanceContrast.setAutoWhite(255);
        enhanceContrast.setAutoBlack(0);

        FilterEnhanceContrast enhanceContrast2 = new FilterEnhanceContrast();
        enhanceContrast2.setAutoWhite(255);
        enhanceContrast2.setAutoBlack(0);


        FilterLineGauss lgauss = new FilterLineGauss();
        lgauss.setSigma((double) dp.getValue(Pre.line_sigma) / 10);

        FilterCrossCorrelation filterCrossCorrelation = new FilterCrossCorrelation();
        filterCrossCorrelation.setMaskSize((double) dp.getValue(Pre.corr_mask_size));
        filterCrossCorrelation.setZeroCrossing((double) dp.getValue(Pre.corr_zero));

        FilterQueue queue = new FilterQueue();
        queue.add(gauss);
        queue.add(enhanceContrast);
        queue.add(laPlace);
        queue.add(lgauss);
        queue.add(gauss2);
        queue.add(enhanceContrast2);
        return queue;
    }

    public static FilterQueue getSimpleFilterQueue(Settings dp) {
        //gauss-laplace-linegauss-contrast

        FilterGauss gauss = new FilterGauss();
        gauss.setSigma((double) dp.getValue(Pre.fsigma) / 10);

        FilterLaPlace laPlace = new FilterLaPlace();
        laPlace.setMaskType(Const.makeNeighborHoodMap().get("8 neighbor"));
        laPlace.setFactor((double) dp.getValue(Pre.lpfac) / 10);

        FilterEnhanceContrast enhanceContrast = new FilterEnhanceContrast();
        enhanceContrast.setAutoWhite(255);
        enhanceContrast.setAutoBlack(0);

        FilterLineGauss lgauss = new FilterLineGauss();
        lgauss.setSigma((double) dp.getValue(Pre.line_sigma) / 10);

        FilterQueue queue = new FilterQueue();
        queue.add(gauss);
        queue.add(laPlace);
        queue.add(lgauss);
        queue.add(enhanceContrast);
        return queue;
    }


    public static FilterQueue getDefaultLineSensorQueue(Settings dp, boolean addInvert) {
        int minMean = dp.getValue(Bin.minmean);
        double sigma = dp.getValue(Bin.sigma) / 10.0;
        double sign = dp.getValue(Bin.area_significance) / 10.0;
        FilterBinarization filterBinarization = new FilterBinarization(minMean, sigma, sign, FilterBinarization.methodArea);
        FilterQueue filters = new FilterQueue();
        filters.add(filterBinarization);
        if (dp.getValue(Bin.restrict) != 0) {
            FilterAreaMask filterAreaMask = new FilterAreaMask();
            filterAreaMask.setAreaOrExtArea(dp.getValueAsBoolean(Bin.is_area_or_ext));
            filters.add(filterAreaMask);
        }
        if (dp.getValue(Bin.thicken) != 0) {
            filters.add(new FilterClosing());
        } else {
            filters.add(new FilterRemovePixels());
        }
        if (addInvert) filters.add(new FilterInvert());
        return filters;
    }


    /**
     * Establishes dependency between images orig->pre->line->fil.
     * For it to work correctly the references should not be changed(should be effectively final).
     * For example if you change the lineQueue do not create a new one, just switch the filters inside the queue
     *
     * @param orig
     * @param pre
     * @param line
     * @param fil
     * @param preQueue
     * @param lineQueue
     * @param parameters
     * @param scale      wrapped with DoubleProperty for "reference like behaviour"
     */
    public static void establishImageDependency(@NotNull final ImageWrapper orig,
                                                @NotNull final ImageWrapper pre,
                                                @NotNull final ImageWrapper line,
                                                @NotNull final ImageWrapper fil,
                                                @NotNull final FilterQueue preQueue,
                                                @NotNull final FilterQueue lineQueue,
                                                @NotNull final Settings parameters,
                                                DoubleProperty scale) {
        Objects.requireNonNull(orig, "ProcessingUtils::() orig-ImageWrapper is null");
        Objects.requireNonNull(pre, "ProcessingUtils::() pre-ImageWrapper is null");
        Objects.requireNonNull(line, "ProcessingUtils::() line-ImageWrapper is null");
        Objects.requireNonNull(fil, "ProcessingUtils::() fil-ImageWrapper is null");

        ImageDependency origDependency = new ImageDependency(pre,
                (src, tgt) -> {
                    try {
                        initializePreprocessing(src, scale.get(), tgt);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }, (v) -> v,
                (wrapper) -> preQueue.run(pre, null)
                , true);

        ImageDependency preDependency = new ImageDependency(line,
                (src, tgt) -> {
                    if (src != tgt) src.cloneData(tgt);//check hash if its the same object do not clone
                }, (v) -> v,
                (wrapper) -> lineQueue.run(wrapper, null),
                true);

        //think about what this dependency needs, it only needs line sensor and only changes lineSensor (since the data-elements are shared among all wrappers here)
        ImageDependency lineDependency = new ImageDependency(fil,
                (src, tgt) -> {//src = line, tgt= fil
                    //no need for copy or anything else
                }, (v) -> v,
                (wrapper) -> {
                    //this part is tricky, since it does not process wrapper (which is in this case "fil"), it should process line
                    filamentSensor(line, parameters);
                },
                true);
        orig.addImageDependency(origDependency);//this creates the pre processed image
        pre.addImageDependency(preDependency);//this creates the line image
        line.addImageDependency(lineDependency);//this processes the line image and retrieves filament objects

        //orig -> pre -> line -> fil  :since orig==fil this creates a circle -> endless loop
        //countered by circle detection

    }


}
