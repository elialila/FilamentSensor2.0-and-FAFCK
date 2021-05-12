package core.tracers;

import core.Const;
import core.FilamentSensor;
import core.filaments.AbstractFilament;
import core.image.BinaryImage;
import core.image.IBinaryImage;
import core.settings.Settings;
import core.settings.Trace;
import ij.process.ImageProcessor;
import util.ImageExporter;
import util.LineIterator;
import util.PointUtils;
import util.fuzzy.matrix.Boolean2D;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

public abstract class AbstractTracer implements Tracer {

    protected int[][] m_orientation_field;
    protected int[][] m_width_map;
    protected List<AbstractFilament> m_filament_list;

    protected boolean[][][] m_circle_masks;

    /**
     * Measures line width in start point
     *
     * @param start        point at which the width is measured
     * @param end          point for calculating line angle
     * @param assumedWidth assumed width of the line
     * @param binImage     boolean image(matrix) the width should be measured from
     * @param listWidth    list of measured width's
     */
    protected void measureInPoint(Point2D start, Point2D end, double assumedWidth, boolean[][] binImage, List<Double> listWidth) {
        double angleRAD = PointUtils.getAngleRAD(start, end);
        //create 2 lines in 90 degree angle to the filament line
        double scanAngleOne = (Math.toRadians(90) + angleRAD);
        if (scanAngleOne > Math.PI * 2) scanAngleOne -= Math.PI * 2;
        double scanAngleTwo = (angleRAD - Math.toRadians(90));
        if (scanAngleTwo < 0) scanAngleTwo += Math.PI * 2;

        Boolean2D fuzzyBinImage = new Boolean2D(binImage);

        boolean interruptOne = false, interruptTwo = false;
        int distance = 1;

        double width = 0;
        //xx = x + (d * cos(alpha))
        //yy = y + (d * sin(alpha))
        //limit iterations to assumedWidth
        while ((!interruptOne || !interruptTwo) && distance <= assumedWidth) {
            //calculate two scanpoints in 90° angle to the filament line
            Point2D nextOne = new Point2D.Double(start.getX() + distance * Math.cos(scanAngleOne), start.getY() + distance * Math.sin(scanAngleOne));
            Point2D nextTwo = new Point2D.Double(start.getX() + distance * Math.cos(scanAngleTwo), start.getY() + distance * Math.sin(scanAngleTwo));
            //if one of the lines was interrupted don't increase that value anymore, just scan the other side further
            if (!interruptOne) {
                double isSet = fuzzyBinImage.get(nextOne.getX(), nextOne.getY());
                width += (1 - isSet);//since false is filament pixel, the correct probability of filament pixel is 1-isSet
                if (isSet > 0.5) {    //if more than 50% chance for true pixel, quit scanning here
                    interruptOne = true;
                    width -= (1 - isSet);//remove the added value
                }
            }
            if (!interruptTwo) {
                double isSet = fuzzyBinImage.get(nextTwo.getX(), nextTwo.getY());
                width += (1 - isSet);//since false is filament pixel, the correct probability of filament pixel is 1-isSet
                if (isSet > 0.5) {  //if more than 50% chance for true pixel, quit scanning here
                    interruptTwo = true;
                    width -= (1 - isSet);//remove the added value
                }
            }
            //increase the distance of the scan line with every loop iteration
            distance++;
        }
        //add the original point to width
        //original width is at some point already partly in the width calculation because of the fuzzy array, weight it
        width += (1 - fuzzyBinImage.get(start.getX(), start.getY())) * 0.7d;
        listWidth.add(width);

    }

    protected List<Double> getMeasurements(AbstractFilament filament, boolean[][] binImage) {

        //calculate for every filament the measured width in all points and average it
        double assumedWidth = ((double) filament.getWidth() / Const.MF);
        //width-formula used in Gatherer:Math.round(m_score * Const.MF / (double) m_points.size())
        List<Double> listWidth = new ArrayList<>();

        List<Point> points = filament.getPoints();


        for (int i = 0; i < points.size(); i++) {
            if (i + 1 < points.size()) {
                //create a line between two points of the filament
                LineIterator iterator = new LineIterator(new Line2D.Double(points.get(i), points.get(i + 1)));
                while (iterator.hasNext()) {
                    Point2D current = iterator.next();
                    measureInPoint(current, points.get(i + 1), assumedWidth, binImage, listWidth);
                }
                //measureInPoint(points.get(i),points.get(i+1),assumedWidth,binImage,listWidth,debug);
            } else {
                LineIterator iterator = new LineIterator(new Line2D.Double(points.get(i), points.get(i - 1)));
                while (iterator.hasNext()) {
                    Point2D current = iterator.next();
                    measureInPoint(current, points.get(i - 1), assumedWidth, binImage, listWidth);
                }
                //measureInPoint(points.get(i),points.get(i-1),assumedWidth,binImage,listWidth,debug);
            }
        }
        return listWidth;
    }


    /**
     * @param binImage resulting image from line sensor as boolean[][]
     */
    public void updateFilamentWidth(boolean[][] binImage) {
        //false = white pixel, true = black pixel

        m_filament_list.forEach(filament -> {
            List<Double> listWidth = getMeasurements(filament, binImage);

            double[] values = listWidth.stream().filter(s -> s > 0).mapToDouble(s -> s).sorted().toArray();
            if (values.length > 0) {//if no values found keep original width?

                //create customized avg, remove the extreme values and avg rest
                double max = listWidth.stream().mapToDouble(s -> s).max().orElse(0);
                double min = listWidth.stream().mapToDouble(s -> s).min().orElse(0);
                listWidth.remove(max);
                listWidth.remove(min);
                long customizedAvg = Math.round(listWidth.stream().filter(s -> s > 0).mapToDouble(s -> s).average().orElse(1) * Const.MF);
                filament.setWidth(customizedAvg);
            } else {
                FilamentSensor.debugMessage("Filament of Error case:length=" + filament.getLength() + ",points=" + filament.getPoints() + ",width=" + filament.getWidth());
                filament.setPossibleError(true);
            }
        });
    }


    protected void initOrientationField(int x_size, int y_size) {
        if (m_orientation_field == null) {
            // initialize orientation field
            m_orientation_field = new int[x_size][y_size];
            for (int x = 0; x < x_size; x++) {
                for (int y = 0; y < y_size; y++) {
                    m_orientation_field[x][y] = -1;
                }
            }
        }
    }

    public IBinaryImage getPixelMask() {
        return new BinaryImage(m_orientation_field, -1);
    }


}
