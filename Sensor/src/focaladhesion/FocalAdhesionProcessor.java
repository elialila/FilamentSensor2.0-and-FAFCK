package focaladhesion;


import core.image.CorrelationData;
import util.Annotations.Nullable;
import core.filaments.AbstractFilament;
import javafx.util.Pair;
import core.cell.DataFilaments;
import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.Settings;
import core.settings.FocAdh;
import util.LineIterator;
import util.MixedUtils;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FocalAdhesionProcessor {

    public enum FAVerificationMethod {
        ellipse, convexHull, pixel
    }


    /**
     * @param filaments
     * @param focalAdhesion thresholded focal adhesion stack
     * @param dp
     * @throws RuntimeException if correlationData was not found
     */
    public void run(ImageWrapper filaments, ImageWrapper focalAdhesion, Settings dp, FAVerificationMethod method, @Nullable Consumer<Integer> reporter) throws Exception {
        List<Entry> entryList = focalAdhesion.getEntryList();
        List<Entry> entryList2 = filaments.getEntryList();
        final int imageAmount = entryList.size();
        final boolean bothEnds = dp.getValueAsBoolean(FocAdh.bothEnds);
        final int neighborHoodSize = dp.getValue(FocAdh.neighborHoodSize);

        System.out.println("Start focal adhesion processing(" + neighborHoodSize + ")");

        //create a third list which is a List<Pair<Entry,Entry>>
        int min = Math.min(entryList.size(), entryList2.size());
        List<Pair<Entry, Entry>> combined = new ArrayList<>();//pair.key = focalAdhesion; pair.value = filaments
        for (int i = 0; i < min; i++) {
            combined.add(new Pair<>(entryList.get(i), entryList2.get(i)));
        }
        CorrelationData chkData = combined.stream().map(pair -> pair.getKey().getCorrelationData()).filter(cd -> cd != null && (cd instanceof FocalAdhesionContainer && ((FocalAdhesionContainer) cd).getData().size() > 0)).findAny().orElse(null);
        if (chkData == null) {
            //no data found
            throw new RuntimeException("No focal adhesion data");
        }


        //now its possible to stream over the combined
        //takes forever on the notebook(8GB RAM)
        //for now -> forceParallel true, to see if it has acceptable times with parallel computing
        AtomicInteger cnt = new AtomicInteger(0);
        System.out.println("FocalAdhesionStart Focal Adhesion Processing(" + neighborHoodSize + ")");
        MixedUtils.getStream(combined, false).forEach((pair) -> {
            long time = System.currentTimeMillis();
            this.compareNew(((FocalAdhesionContainer) pair.getKey().getCorrelationData()), pair.getValue().getDataFilament(), neighborHoodSize, bothEnds, method);
            System.out.println("CompareNew taken:" + ((double) (System.currentTimeMillis() - time) / 1000) + "s");
            //the FocalAdhesion data should contain the cluster-data from the focalAdhesion's
            if (reporter != null) {
                cnt.incrementAndGet();
                double progressTmp = cnt.get();
                progressTmp /= imageAmount;
                System.out.println("Test::" + progressTmp);
                int progress = (int) Math.floor(progressTmp);//status stays 0
                reporter.accept(progress);
            }
        });
        System.out.println("End Focal Adhesion Processing");
    }


    private boolean verifyShapeForeachCheck(Iterator<Point2D> itPoint, final int neighborHoodSize, Shape shape, AtomicBoolean current) {
        Point2D cPoint = itPoint.next();
        Rectangle2D rectangle2D = new Rectangle2D.Double(cPoint.getX() - neighborHoodSize / 2, cPoint.getY() - neighborHoodSize / 2, neighborHoodSize, neighborHoodSize);
        //intersects does only check the hull
        boolean inside = shape.contains(rectangle2D) | shape.intersects(rectangle2D);
        current.set(current.get() | inside);
        //if it gets a true value it can terminate the loop
        if (inside) return true;
        return false;
    }

    /**
     * @param focalAdhesion
     * @param filament
     * @param neighborHoodSize
     * @param verifier
     * @param atomicBoolean
     * @param shape
     * @param longestFA        length of the longest focal adhesion, to limit the scanned path on the filament
     */
    private void verifyShapeForeach(FocalAdhesion focalAdhesion, AbstractFilament filament, final int neighborHoodSize, HashSet<Integer> verifier, AtomicBoolean atomicBoolean, Shape shape, int longestFA) {
        //iterate the path over the whole filament
        AtomicBoolean current = new AtomicBoolean(false);
        List<Point> points = filament.getPoints();
        boolean finish = false;//variable for terminating loop early (as soon as true is returned on inside-check there is no need for another iteration)
        //cases:
        //case1: longestFA > filament.getLength => scan filament length 1 time
        //case2: longestFA < filament.getLength => scan length of longestFA from filament starting point and another time from end point (could be done in the same loop)
        //case3: longestFA == filament.getLength => handle it like case1 (less calculation effort)
        if (longestFA >= filament.getLength()) {
            for (int iP = 0; iP < points.size() - 1 && !finish; iP++) {
                Line2D line2D = new Line2D.Double(points.get(iP).x, points.get(iP).y, points.get(iP + 1).x, points.get(iP + 1).y);
                for (Iterator<Point2D> itPoint = new LineIterator(line2D); itPoint.hasNext() && !finish; ) {
                    if (verifyShapeForeachCheck(itPoint, neighborHoodSize, shape, current)) {
                        finish = true;
                    }
                }
            }
        } else {
            int cnt = 0;//count the checked length
            for (int iP = 0; iP < points.size() - 1 && !finish && cnt < longestFA; iP++) {
                Iterator<Point2D> itStart = new LineIterator(new Line2D.Double(points.get(iP), points.get(iP + 1)));
                Iterator<Point2D> itEnd = new LineIterator(new Line2D.Double(points.get(points.size() - 1 - iP), points.get(points.size() - 1 - (iP + 1))));
                for (; cnt < longestFA && !finish && (itStart.hasNext() || itEnd.hasNext()); cnt++) {
                    if ((itStart.hasNext() && verifyShapeForeachCheck(itStart, neighborHoodSize, shape, current)) ||
                            (itEnd.hasNext() && verifyShapeForeachCheck(itEnd, neighborHoodSize, shape, current))) {
                        finish = true;
                    }
                }
            }
        }
        if (current.get()) {
            //add verifier
            verifier.add(focalAdhesion.getNumber());
        }
        atomicBoolean.set(atomicBoolean.get() | current.get());
    }


    /**
     * Create a shape and return it, the type of shape is determined by the boolean parameter
     * data for the shape is retrieved from the focal adhesion object
     *
     * @param focalAdhesion
     * @param method
     * @return
     */
    private Shape getShapeFromFocalAdhesion(FocalAdhesion focalAdhesion, FAVerificationMethod method) {

        if (method.equals(FAVerificationMethod.convexHull)) {
            int[] xPoints = focalAdhesion.getConvexHull().stream().mapToInt(p -> (int) p.getX()).toArray();
            int[] yPoints = focalAdhesion.getConvexHull().stream().mapToInt(p -> (int) p.getY()).toArray();
            return new Polygon(xPoints, yPoints, focalAdhesion.getConvexHull().size());
        } else {
            Ellipse2D ellipse = new Ellipse2D.Double((int) (0 - focalAdhesion.getLengthMainAxis() / 2),
                    (int) (0 - focalAdhesion.getLengthSideAxis() / 2),
                    (int) (focalAdhesion.getLengthMainAxis()),
                    (int) (focalAdhesion.getLengthSideAxis())
            );
            AffineTransform transform = new AffineTransform();
            transform.translate(focalAdhesion.getCenter().getX(), focalAdhesion.getCenter().getY());
            transform.rotate(focalAdhesion.getOrientation());
            return transform.createTransformedShape(ellipse);
        }

    }

    /**
     * This method verifies a filament by a focal adhesion given as a shape, the type of shape is determined
     * by the FAVerificationMethod enum (@param method)
     *
     * @param dataContainer
     * @param filament
     * @param neighborHoodSize
     * @param bothEnds
     * @param method
     * @param longestFA        length of the longest focal adhesion, to limit the scanned path on the filament
     */
    private void verifyFilamentByShape(FocalAdhesionContainer dataContainer, AbstractFilament filament, final int neighborHoodSize, boolean bothEnds, FAVerificationMethod method, int longestFA) {


        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        HashSet<Integer> verifier = new HashSet<>();

        //uses iterator to terminate loop early in case of bothEnds==false, as soon as one match is found, terminate
        boolean terminateLoop = false;
        for (Iterator<FocalAdhesion> it = dataContainer.getData().iterator(); it.hasNext() && !terminateLoop; ) {
            FocalAdhesion focalAdhesion = it.next();
            //create a shape in a method and return it
            Shape shape = getShapeFromFocalAdhesion(focalAdhesion, method);
            verifyShapeForeach(focalAdhesion, filament, neighborHoodSize, verifier, atomicBoolean, shape, longestFA);

            //if (!bothEnds && atomicBoolean.get()) terminateLoop = true;
            //let it run through all

            //if bothEnds is false it terminates after finding one solution
        }

        //initialise verifier and verified
        filament.setVerifier(null);
        filament.setVerified(false);
        if (atomicBoolean.get()) {
            //this if can be modified for "bothEnds" --> bothEnds=true means more than 1 focal adhesion has to verify the filament

            filament.setVerified(!bothEnds || verifier.size() > 1);
            Integer[] params = new Integer[verifier.size()];
            //2020-10-12 set verifier always (test if still works)
            //if (filament.isVerified()) {
            filament.setVerifier(Verifier.focalAdhesion(verifier.toArray(params)));
            //}
        }

    }


    /**
     * Compares focalAdhesion's with filaments and set the verified and verifier attribute
     *
     * @param dataContainer
     * @param dataFilament
     * @param neighborHoodSize
     * @param bothEnds
     * @param method
     */
    public void compareNew(FocalAdhesionContainer dataContainer, DataFilaments dataFilament,
                           int neighborHoodSize, boolean bothEnds, FAVerificationMethod method) {
        final int longestFA = dataContainer.getData().stream().mapToInt(f -> (int) f.getLengthMainAxis()).max().orElse(0);


        dataFilament.getFilaments().forEach(filament -> {
            verifyFilamentByShape(dataContainer, filament, neighborHoodSize, bothEnds, method, longestFA);
        });
    }


}
