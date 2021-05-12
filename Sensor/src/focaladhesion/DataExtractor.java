package focaladhesion;

import javafx.util.Pair;
import core.FilterQueue;
import filters.FilterInvert;
import util.Annotations.Nullable;
import core.Calc;

import ij.Prefs;
import ij.plugin.filter.Binary;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import core.image.*;
import core.settings.Settings;
import core.settings.FocAdh;
import util.MixedUtils;
import util.PointUtils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * This class contains the focal adhesion extraction from thresholded ImageWrapper
 * <p>
 * current filters:
 * min-max size focal adhesion's -> to sort out too big or too small ones
 * max number of focal adhesion's -> render images as "noisy" if there are too many clusters -> no processing should be done for those images
 */
public class DataExtractor {

    /**
     * Process binary image (clean up a bit and outline for easier convex hull calculation)
     *
     * @param binaryImage
     * @param histWhite   index in the histogram-array which represents white value
     * @return
     */
    private static FocalAdhesion preProcessBinary(IBinaryImage binaryImage, int histWhite, Settings dp) {
        if (Prefs.blackBackground) binaryImage.setTrueValue(255);
        ByteProcessor processor = binaryImage.getByteProcessor();
        //keep the original amount of pixels set
        FocalAdhesion focalAdhesion = new FocalAdhesion();
        int[] hist = processor.getHistogram();
        focalAdhesion.setArea(hist[histWhite]);
        focalAdhesion.setPixelArea(binaryImage.clone());

        //do some processing, like closing gaps
        //outline reduces the number of points used to get the convex hull
        Binary binary = new Binary();
        binary.setup("outline", null);
        binary.run(processor);
        binaryImage.flush();
        binaryImage.close();
        return focalAdhesion;
    }


    /**
     * Calculate the Y-Axis by getting the farthest point to the x-axis and create a symmetric point(symmetric axis is the x-axis)
     * this way, the ellipse should contain all of the focal adhesion, downside is that it's area is far larger
     * than the actual focal adhesion.
     *
     * @param a             start point of x-axis
     * @param b             end point of x-axis
     * @param convexHull    the convex hull of the object
     * @param focalAdhesion focalAdhesion object which represents the underlying binary image
     * @return success status true: succeeded, false: some "error" has occured for example there is no Y-Axis(is the case if all points are on a line)
     */
    private static boolean calcYAxis(Point2D a, Point2D b, List<Point2D> convexHull, FocalAdhesion focalAdhesion) {
        //Ellipse Y axis: furthest point on convex hull from xAxis
        Line2D lineX = new Line2D.Double(a, b);
        double distMax = 0;
        Point2D ptMax = null;
        for (Point2D ptHull : convexHull) {
            double tmpDist = lineX.ptLineDistSq(ptHull);
            if (tmpDist > distMax) {
                distMax = tmpDist;
                ptMax = ptHull;
            }
        }
        if (distMax == 0) return false;
        //get a virtual point symmetric to ptMax (symmetric axis is the xAxis of the ellipse)
        //this is for the purpose of containing the whole object in the ellipse, since the widest point
        //is not in the center of the ellipse and the distance is only from central line to one side
        //we have to create the symmetric point

        double k = (b.getY() - a.getY()) / (b.getX() - a.getX());
        double d = a.getY() - k * a.getX();
        double kN = -1 / k;
        double dN = ptMax.getY() - kN * ptMax.getX();
        double xM = (d - dN) / (kN - k);
        double yM = k * xM + d;
        Point2D ptMiddle = new Point2D.Double(xM, yM);
        double xSym = 2 * xM - ptMax.getX();
        double ySym = 2 * yM - ptMax.getY();
        Point2D ptSym = new Point2D.Double(xSym, ySym);

        focalAdhesion.setSideAxisStart(ptMax);
        focalAdhesion.setSideAxisEnd(ptSym);
        focalAdhesion.setLengthSideAxis(ptMax.distance(ptSym));
        return true;
    }


    /**
     * @param wrapper    ImageWrapper from which the FocalAdhesion's should be processed (already as binary image)
     * @param dp         parameters for processing
     * @param separators map
     * @param reporter   Consumer which gets fed with percent value of completion (natural number, ceil())
     *                   Calculate all focal adhesion's for the ImageWrapper
     *                   Should not be used without asynchronous call (very long run-times)
     */
    public static void extractFocalAdhesionData(ImageWrapper wrapper, Settings dp, @Nullable Map<Integer, List<List<Point2D>>> separators, @Nullable Consumer<Integer> reporter) {
        AtomicInteger current = new AtomicInteger(1);
        final int size = wrapper.getEntryList().size();
        long time = System.nanoTime();
        AtomicInteger state = new AtomicInteger(1);

        FilterQueue invert = new FilterQueue();
        invert.add(new FilterInvert());
        if (Prefs.blackBackground) {//invert so that the images are in IJ format
            //for displaying on ui the image was inverted
            invert.run(wrapper, (f) -> {
            });
        }

        //match correct "image-number" otherwise separators will not get used correctly
        MixedUtils.getStream(wrapper.getEntryList().stream().map(e -> new Pair<>(current.getAndIncrement(), e)).collect(Collectors.toList())
                , false).forEach(pair -> {
            Entry entry = pair.getValue();
            List<List<Point2D>> listSeparators = null;
            if (separators != null) {
                listSeparators = separators.get(pair.getKey());
            }
            CorrelationData result = processSingleEntry(entry.getProcessor(), dp, listSeparators);
            entry.setCorrelationData(result);

            double percent = state.getAndIncrement();
            percent /= size;
            percent *= 100;
            if (reporter != null) {
                reporter.accept((int) Math.ceil(percent));
            }
        });
        if (Prefs.blackBackground) {//invert so that the images are in IJ format
            //for calculation the image was inverted
            //invert back for ui
            invert.run(wrapper, (f) -> {
            });
        }
        System.out.println("Time(extract):" + ((System.nanoTime() - time) / 1000000000) + "s");


    }

    /**
     * Does some pre processing before clustering (closing and fill holes)
     *
     * @param processor
     */
    private static void prepare(ByteProcessor processor, Settings dp) {
        Binary binary = new Binary();
        //System.out.println("doClosing=" + dp.getValueAsBoolean(FocAdh.doClosing) + ",doFillHoles=" + dp.getValueAsBoolean(FocAdh.doFillHoles));
        if (dp.getValueAsBoolean(FocAdh.doClosing)) {
            binary.setup("close", null);
            binary.run(processor);
        }

        if (dp.getValueAsBoolean(FocAdh.doFillHoles)) {
            binary.setup("fill", null);//just a test - seems to work without strange "fill all" behaviour -> benefit no empty pixels inside the adhesion's
            binary.run(processor);
        }

    }

    /**
     * Applies separators if available
     *
     * @param processor    ByteProcessor which represents the image
     * @param separators   list of lines(which are represented as list of points)
     * @param tmpHistWhite foreground color value
     */
    private static void applySeparators(ByteProcessor processor, @Nullable List<List<Point2D>> separators, int tmpHistWhite) {
        //if separators are found use them, to separate clusters
        if (separators != null) {
            final ByteProcessor tmp = processor;
            processor.setColor(255 - tmpHistWhite);//background color
            separators.forEach(points -> {
                for (int i = 0; i + 1 < points.size(); i++) {
                    tmp.drawLine((int) points.get(i).getX(), (int) points.get(i).getY(), (int) points.get(i + 1).getX(), (int) points.get(i + 1).getY());
                }
            });
        }
    }


    /**
     * Processes list of binary image and fills up focal adhesion data into FocalAdhesionContainer
     *
     * @param correlationData data wrapper to be filled
     * @param listAdhesion    list of FA clusters
     * @param histWhite       foreground color value
     * @param dp              settings container
     */
    private static void processCorrelationData(FocalAdhesionContainer correlationData, List<IBinaryImage> listAdhesion, final int histWhite, Settings dp) {
        correlationData.setData(new ArrayList<>(
                //MixedUtils.getStream(listAdhesion, false).//when extractAdhesionData uses MixedUtils.getStream, this method shouldn't
                listAdhesion.stream().
                        map(binaryImage -> {
                            FocalAdhesion focalAdhesion = preProcessBinary(binaryImage, histWhite, dp);
                            List<Point2D> convexHull = PointUtils.getConvexHull(binaryImage.getPoints());
                            focalAdhesion.setConvexHull(convexHull);
                            if (convexHull == null)
                                return null;//catch no convex hull, if there is no hull, there is no focal adhesion ...
                            //get the longest distance between

                            util.Pair<Point2D, Point2D> result = PointUtils.getMaxDist(convexHull);
                            Point2D a = result.getKey(), b = result.getValue();
                            focalAdhesion.setLengthMainAxis(a.distance(b));
                            focalAdhesion.setMainAxisStart(result.getKey());
                            focalAdhesion.setMainAxisEnd(result.getValue());

                            if (Double.isNaN(focalAdhesion.getLengthMainAxis()))
                                return null;//if main axis has non numeric value drop focal adhesion
                            //in case of a non existing Y-Axis return null
                            if (!calcYAxis(a, b, convexHull, focalAdhesion)) return null;
                            if (Double.isNaN(focalAdhesion.getLengthSideAxis()))
                                return null;//if side axis has non numeric value, drop FA


                            focalAdhesion.setAspectRatio(focalAdhesion.getLengthMainAxis() / focalAdhesion.getLengthSideAxis());
                            focalAdhesion.setOrientation(PointUtils.getAngleRAD(a, b));
                            focalAdhesion.setCenter(new Point(
                                    (int) ((result.getKey().getX() + result.getValue().getX()) / 2),
                                    (int) ((result.getKey().getY() + result.getValue().getY()) / 2)));
                            return focalAdhesion;
                        }).filter(Objects::nonNull).collect(Collectors.toList())));
    }

    /**
     * Processes a single Entry of a ImageWrapper and returns a CorrelationData (in this case a FocalAdhesionContainer)
     *
     * @param inProcessor processor which contains the thresholded image of focal adhesion's
     * @param dp          settings container
     * @param separators  a list of lines(which are represented by a list of points)
     * @return CorrelationData which contains the focal adhesion's
     */
    private static CorrelationData processSingleEntry(ImageProcessor inProcessor, Settings dp, @Nullable List<List<Point2D>> separators) {
        //first step filter adhesion's. Too small adhesion's will be skipped/dropped
        //should be done pre measurement (skipped in this method)

        int tmpHistWhite = 255;
        if (!Prefs.blackBackground) tmpHistWhite = 0;

        ByteProcessor processor = null;
        if (inProcessor.isGrayscale() && !(inProcessor instanceof ByteProcessor))
            processor = inProcessor.convertToByteProcessor();
        else processor = (ByteProcessor) inProcessor;//.duplicate();//remove duplicate to test something
        if (processor.isInvertedLut()) {
            tmpHistWhite = 255 - tmpHistWhite;
        }
        prepare(processor, dp);
        applySeparators(processor, separators, tmpHistWhite);//is also called

        final int minArea = dp.getValue(FocAdh.minSize);
        final int maxArea = dp.getValue(FocAdh.maxSize);

        List<IBinaryImage> listAdhesion = Calc.largestObject(new BinaryImage(processor), minArea).stream().filter(i -> i.getPixelSetCount() <= maxArea).collect(Collectors.toList());

        //adhesion's with too small and too large areas are discarded
        FocalAdhesionContainer correlationData = new FocalAdhesionContainer();
        if (listAdhesion.size() > dp.getValue(FocAdh.maxClusterAmount))
            return correlationData;//too many clusters(raw focal adhesion's) return because the image is too noisy
        //"too noisy" is defined by the user (setting parameter maxClusterAmount)
        processCorrelationData(correlationData, listAdhesion, tmpHistWhite, dp);
        AtomicInteger focalAdhesionNumber = new AtomicInteger(0);
        correlationData.getData().forEach(f -> f.setNumber(focalAdhesionNumber.getAndIncrement()));


        return correlationData;
    }


}
