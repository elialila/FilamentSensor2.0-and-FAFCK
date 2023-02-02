package evaluation;
/*
 * This file is part of FilamentSensor2 - A tool for filament tracking from cell images
 *
 * Copyright (C) 2018-2022 Andreas Primeßnig
 *
 * FilamentSensor2 is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * or see <http://www.gnu.org/licenses/>.
 */

import core.Const;
import core.FilamentSensor;
import core.filaments.AbstractFilament;
import core.image.BinaryImage;
import core.settings.Eval;
import core.settings.Settings;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.plugin.RGBStackConverter;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import util.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FilamentEvaluator {

    //these colors are set by "hand" -> do not change this values, you have to determine the fp-,fn-,matchColors again
    private final int filamentColorEval = 180;
    private final int filamentColorTruth = 130;
    //these colors are a result from setting the colors above
    private final int fpColor = 60;
    private final int fnColor = 43;
    private final int matchColor = 103;


    //orange = truth, blue = match, red = eval
    final int red = Color.red.getRGB(), orange = Color.orange.getRGB(), blue = Color.cyan.getRGB();


    /*
    todo: this class should contain filament evaluation (special case, with metadata)
    here we have the images and the filament-data to evaluate (compare two different parameter sets)
    the pixel evaluation is the same just the object evaluation differs from normal evaluator


    */

    /**
     * This Class is used for bundling the temporary data during evaluation (also for debugging)
     */
    private class FilamentEvalDataContainer {
        private int whiteEval;
        private int whiteTruth;
        private int whiteMatches;

        //private BinaryImage eval;
        //private BinaryImage truth;
        //private BinaryImage match;

        private AbstractFilament filamentEval;
        private AbstractFilament filamentTruth;

        private Shape shapeEval;
        private Shape shapeTruth;
        private Shape shapeIntersection;


        public FilamentEvalDataContainer(AbstractFilament filamentEval, AbstractFilament filamentTruth, Shape shapeEval, Shape shapeTruth, Shape shapeIntersection) {
            this.filamentEval = filamentEval;
            this.filamentTruth = filamentTruth;
            this.shapeEval = shapeEval;
            this.shapeTruth = shapeTruth;
            this.shapeIntersection = shapeIntersection;
        }

        public Shape getShapeEval() {
            return shapeEval;
        }

        public Shape getShapeTruth() {
            return shapeTruth;
        }

        public Shape getShapeIntersection() {
            return shapeIntersection;
        }

        public AbstractFilament getFilamentEval() {
            return filamentEval;
        }

        public AbstractFilament getFilamentTruth() {
            return filamentTruth;
        }

        public void setWhiteEval(int whiteEval) {
            this.whiteEval = whiteEval;
        }

        public void setWhiteTruth(int whiteTruth) {
            this.whiteTruth = whiteTruth;
        }

        public void setWhiteMatches(int whiteMatches) {
            this.whiteMatches = whiteMatches;
        }

        public int getWhiteEval() {
            return whiteEval;
        }

        public int getWhiteTruth() {
            return whiteTruth;
        }

        public int getWhiteMatches() {
            return whiteMatches;
        }

        @Override
        public String toString() {
            return "FilamentEvalDataContainer{" +
                    "whiteEval=" + whiteEval +
                    ", whiteTruth=" + whiteTruth +
                    ", whiteMatches=" + whiteMatches +
                    '}';
        }
    }


    private List<Polyline> mapToPolyline(List<AbstractFilament> filaments, Paint paint) {
        return filaments.stream().map(fil -> {
            Polyline polyline = new Polyline(fil.getPoints().stream().map(p -> new double[]{p.x, p.y}).flatMapToDouble(Arrays::stream).toArray());
            //p.setFill(paint);
            polyline.setStroke(paint);
            polyline.setStrokeWidth(fil.getWidth() / Const.MF);
            return polyline;
        }).collect(Collectors.toList());
    }


    /**
     * Intersects the two given shapes, if there is an intersection return it, otherwise null is returned
     *
     * @param toEval
     * @param groundTruth
     * @return
     */
    private Shape getIntersection(Shape toEval, Shape groundTruth) {
        Shape intersection = Shape.intersect(toEval, groundTruth);

        if (intersection instanceof Path && ((Path) intersection).getElements().size() > 0) {
            return intersection;
        }
        return null;
    }


    /**
     * Draws a filament in given Color (Simplified version of ImageExporter::addFilaments
     *
     * @param graphics2d
     * @param awtColor
     * @param singleFilament
     */
    private void addFilament(Graphics2D graphics2d, Color awtColor, AbstractFilament singleFilament) {
        graphics2d.setColor(awtColor);
        graphics2d.setStroke(new BasicStroke((float) (singleFilament.getWidth() / Const.MF),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        graphics2d.drawPolyline(singleFilament.getPoints().stream().mapToInt(p -> p.x).toArray(), singleFilament.getPoints().stream().mapToInt(p -> p.y).toArray(), singleFilament.getPoints().size());
    }

    /**
     * Adds filaments on ByteProcessor(more precise than graphics2d)
     *
     * @param processor
     * @param filament
     * @param colorValue
     */
    private void addFilament(ImageProcessor processor, AbstractFilament filament, int colorValue) {
        List<Point> points = filament.getPoints();
        double width = (filament.getWidth() / Const.MF);
        processor.setColor(colorValue);
        processor.setLineWidth((int) width);
        for (int i = 0; i < points.size() - 1; i++) {
            processor.drawLine(points.get(i).x, points.get(i).y,
                    points.get(i + 1).x, points.get(i + 1).y);
        }
    }

    /**
     * Intersects two Filaments via IJ Composite Image
     *
     * @param dataContainer
     * @param sizeEval
     * @param sizeTruth
     */
    private void intersectIJ(FilamentEvalDataContainer dataContainer, Dimension sizeEval, Dimension sizeTruth) {
        AbstractFilament filamentEval = dataContainer.getFilamentEval();
        AbstractFilament filamentTruth = dataContainer.getFilamentTruth();
        //create ByteProcessor for calculation
        ByteProcessor processorEval = new ByteProcessor(sizeEval.width, sizeEval.height);
        addFilament(processorEval, filamentEval, filamentColorEval);
        dataContainer.setWhiteEval(processorEval.getHistogram()[filamentColorEval]);


        ByteProcessor processorTruth = new ByteProcessor(sizeTruth.width, sizeTruth.height);
        addFilament(processorTruth, filamentTruth, filamentColorTruth);
        dataContainer.setWhiteTruth(processorTruth.getHistogram()[filamentColorTruth]);

        ImagePlus gray = getComposite(processorEval, processorTruth, false);
        dataContainer.setWhiteMatches(gray.getProcessor().getHistogram()[matchColor]);
        //there can be 3 values, 170 is the matched value
    }


    private ImagePlus getComposite(ByteProcessor processorEval, ByteProcessor processorTruth, boolean keepInColor) {

        //prepare for creating composite image
        ImagePlus[] images = new ImagePlus[]{new ImagePlus("Eval", processorEval), new ImagePlus("Truth", processorTruth)};

        ImagePlus imp = RGBStackMerge.mergeChannels(images, false);
        //convert composite stack to rgb image
        RGBStackConverter.convertToRGB(imp);

        if (keepInColor) return imp;
        //convert to gray scale
        return ImageConversionUtils.convertToGray(imp, false, false);
    }

    /**
     * Initialize Result-Data-Container with pixel data
     *
     * @param resultList
     * @param toEval
     * @param groundTruth
     * @param sizeEval
     * @param sizeTruth
     * @return
     */
    private EvaluationData initData(List<FilamentEvalDataContainer> resultList, List<AbstractFilament> toEval, List<AbstractFilament> groundTruth, Dimension sizeEval, Dimension sizeTruth) {

        ByteProcessor processorEval = new ByteProcessor(sizeEval.width, sizeEval.height);
        ByteProcessor processorTruth = new ByteProcessor(sizeTruth.width, sizeTruth.height);

        //draw all filaments on processor
        toEval.forEach(filament -> addFilament(processorEval, filament, filamentColorEval));
        groundTruth.forEach(filament -> addFilament(processorTruth, filament, filamentColorTruth));

        EvaluationData result = new EvaluationData();
        result.setWhiteEval(processorEval.getHistogram()[filamentColorEval]);
        result.setWhiteTruth(processorTruth.getHistogram()[filamentColorTruth]);

        //create composite
        ImagePlus compGray = getComposite(processorEval, processorTruth, false);

        int[] hist = compGray.getProcessor().getHistogram();

        result.setWhiteMatches(hist[matchColor]);
        result.setDiffImagePixels(getPixelImage(compGray));
        result.setWidthEval(sizeEval.width);
        result.setHeightEval(sizeEval.height);
        result.setWidthTruth(sizeTruth.width);
        result.setHeightTruth(sizeTruth.height);
        result.setHitRate(MixedUtils.round(((double) result.getWhiteMatches()) / result.getWhiteTruth(), 4));
        result.setMissRate(1 - result.getHitRate());
        //fp in this case are white pixels in eval but not white in truth - could be done easily with drawing another image with all filaments
        result.setFpMatches(hist[fpColor]);
        //fp rate is the relation of fp-matches divided by the amount of white pixels in eval
        result.setFpRate(MixedUtils.round(((double) result.getFpMatches()) / result.getWhiteEval(), 4));
        //fn in this case are black pixels in eval but white in truth
        result.setFnMatches(hist[fnColor]);
        //fn rate is the relation of fn-matches divided by the amount of black pixels in eval
        result.setFnRate(MixedUtils.round(((double) result.getFnMatches()) / ((sizeEval.width * sizeEval.height) - result.getWhiteEval()), 4));

        return result;
    }

    private void calculateObjectData(List<FilamentEvalDataContainer> resultList, List<AbstractFilament> toEval, List<AbstractFilament> groundTruth, Dimension sizeEval, Dimension sizeTruth, EvaluationData data, Settings settings) {
        List<Pair<Integer, Pair<Integer, Integer>>> matchAreas = new ArrayList<>();
        List<Pair<Integer, Pair<Integer, Integer>>> nonMatchEval;
        List<Pair<Integer, Pair<Integer, Integer>>> nonMatchTruth;

        final double matchMinPixels = settings.getValueAsDouble(Eval.matchMinPixels);

        Map<AbstractFilament, List<AbstractFilament>> matchingMap = new HashMap<>();
        Map<AbstractFilament,List<AbstractFilament>> invertedMatchingMap=new HashMap<>();
        //compute matching map
        resultList.forEach(dataContainer -> {
            int truthThresh = (int) Math.ceil((double) dataContainer.getWhiteTruth() * matchMinPixels);
            if (dataContainer.getWhiteMatches() >= truthThresh) {
                //object match
                matchingMap.computeIfAbsent(dataContainer.getFilamentEval(), k -> new ArrayList<>());
                matchingMap.get(dataContainer.getFilamentEval()).add(dataContainer.getFilamentTruth());

                invertedMatchingMap.computeIfAbsent(dataContainer.getFilamentTruth(),k->new ArrayList<>());
                invertedMatchingMap.get(dataContainer.getFilamentTruth()).add(dataContainer.getFilamentEval());

                matchAreas.add(new Pair<>(dataContainer.getWhiteTruth(),new Pair<>(dataContainer.getWhiteEval(),dataContainer.getWhiteMatches())));

            }
        });

        nonMatchTruth=groundTruth.stream().filter(filament->!invertedMatchingMap.containsKey(filament)).map(filament->new Pair<>(resultList.stream().filter(tmp->tmp.filamentTruth.equals(filament)).mapToInt(FilamentEvalDataContainer::getWhiteTruth).findAny().orElse(0),new Pair<>(0,0))).collect(Collectors.toList());
        nonMatchEval=toEval.stream().filter(filament->!matchingMap.containsKey(filament)).map(filament->new Pair<>(resultList.stream().filter(tmp->tmp.filamentEval.equals(filament)).mapToInt(FilamentEvalDataContainer::getWhiteEval).findAny().orElse(0),new Pair<>(0,0))).collect(Collectors.toList());
        data.setNonMatchEval(nonMatchEval);
        data.setNonMatchTruth(nonMatchTruth);
        data.setMultiMatchesOneToN((int)invertedMatchingMap.entrySet().stream().filter(entry->entry.getValue().size()>1).count());
        data.setMultiMatchesNToOne((int)matchingMap.entrySet().stream().filter(entry->entry.getValue().size()>1).count());
        data.setObjectsFound(invertedMatchingMap.size());//objects of truth found in eval
        data.setObjectsMissed(groundTruth.size()-data.getObjectsFound());//objects of truth missed in eval
        data.setObjectsEval(toEval.size());
        data.setObjectsTruth(groundTruth.size());
        data.setMatchAreas(matchAreas);
        data.setObjectsFP(nonMatchEval.size());
        data.setDiffImageObjects(getObjectImage(sizeEval,matchingMap,invertedMatchingMap,toEval,groundTruth));
    }

    private ImagePlus getPixelImage(ImagePlus grayComposite){

        ImagePlus imp=new ImagePlus("Pixel Comparison",ImageFactory.getRGBImage(grayComposite.getWidth(),grayComposite.getHeight()+Evaluator.legendHeightOffset));
        ImageProcessor processor=imp.getProcessor();
        int[][] arrResult=processor.getIntArray();
        int[][] arrSource=grayComposite.getProcessor().getIntArray();

        final int width=grayComposite.getWidth(),height=grayComposite.getHeight();

        for(int x=0;x<width;x++){
            for(int y=0;y<height;y++){

                if(arrSource[x][y]==fpColor){
                    //fpColor = eval set and no match
                    arrResult[x][y]=red;
                }else if(arrSource[x][y]==fnColor){
                    arrResult[x][y]=orange;
                }else if(arrSource[x][y]==matchColor){
                    arrResult[x][y]=blue;
                }
            }
        }
        processor.setIntArray(arrResult);
        printLegendOnImage(processor,red,orange,blue, height);
        return imp;
    }


    private ImagePlus getObjectImage(Dimension sizeEval,Map<AbstractFilament,List<AbstractFilament>> matchingMap,Map<AbstractFilament,List<AbstractFilament>> invertedMatchingMap,List<AbstractFilament> toEval, List<AbstractFilament> groundTruth){


        ImagePlus imp=new ImagePlus("Objects Comparison",ImageFactory.getRGBImage(sizeEval.width, sizeEval.height+Evaluator.legendHeightOffset));
        ImageProcessor processor=imp.getProcessor();
        //blue = matched pixels, red = evaluation pixels, orange = truth pixels


        matchingMap.keySet().forEach(filament->addFilament(processor,filament,blue));

        groundTruth.stream().filter(filament->!invertedMatchingMap.containsKey(filament)).forEach(filament->addFilament(processor,filament,orange));
        toEval.stream().filter(filament->!matchingMap.containsKey(filament)).forEach(filament->addFilament(processor,filament,red));

        printLegendOnImage(processor,red,orange,blue,sizeEval.height);


        return imp;
    }




    /**
     * @param toEval
     * @param groundTruth
     * @param sizeEval
     * @param sizeTruth
     */
    public EvaluationData compare(List<AbstractFilament> toEval, List<AbstractFilament> groundTruth, Dimension sizeEval, Dimension sizeTruth,Settings settings) {
        List<Polyline> toEvalLines = mapToPolyline(toEval, Paint.valueOf("Red"));
        List<Polyline> groundTruthLines = mapToPolyline(groundTruth, Paint.valueOf("Yellow"));

        List<FilamentEvalDataContainer> resultList = new ArrayList<>();
        long time = System.currentTimeMillis();
        for (int i = 0; i < toEvalLines.size(); i++) {
            for (int j = 0; j < groundTruthLines.size(); j++) {
                Shape intersection = getIntersection(toEvalLines.get(i), groundTruthLines.get(j));
                if (intersection != null) {
                    //there is an intersection, process that information
                    FilamentEvalDataContainer result = new FilamentEvalDataContainer(toEval.get(i), groundTruth.get(j), toEvalLines.get(i), groundTruthLines.get(j), intersection);
                    intersectIJ(result, sizeEval, sizeTruth);
                    if (result.getWhiteMatches() > Math.max(result.getWhiteEval(), result.getWhiteTruth())) {
                        System.err.println("Matches > max(eval,truth):(" + i + "," + j + ")");
                    } else {
                        resultList.add(result);
                    }
                }
            }
        }
        FilamentSensor.debugPerformance("FilamentEvaluation time:", time);

        EvaluationData result = initData(resultList, toEval, groundTruth, sizeEval, sizeTruth);
        calculateObjectData(resultList,toEval,groundTruth,sizeEval,sizeTruth,result,settings);

        return result;
    }


    private void printLegendOnImage(ImageProcessor ip, int red, int orange, int blue, int height) {
        int x = 20;
        ip.setFont(ip.getFont().deriveFont(24f));

        ip.setColor(orange);
        ip.fillRect(x, height, 15, 15);//coordinates start top-left
        ip.drawString("Ground-Truth-Pixels", x + 20, height + 20);//coordinates start bottom-left

        ip.setColor(red);
        ip.fillRect(x, height + 30, 15, 15);
        ip.drawString("Evaluation-Pixels", x + 20, height + 20 + 30); //25);

        ip.setColor(blue);
        ip.fillRect(x, height + 60, 15, 15);
        ip.drawString("Matched-Pixels", x + 20, height + 20 + 60); //40);
    }


}
