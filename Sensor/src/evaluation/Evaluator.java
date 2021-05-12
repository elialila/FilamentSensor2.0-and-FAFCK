package evaluation;


import core.Calc;
import ij.ImagePlus;
import ij.Prefs;

import ij.gui.ImageWindow;
import ij.plugin.filter.Binary;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import core.image.BinaryImage;
import core.image.IBinaryImage;
import core.settings.Settings;
import core.settings.Eval;
import util.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains the evaluation logic,
 * it gets two binary images, one "ground truth" and one which should be evaluated.
 * The result is a container with relevant evaluation-metrics.
 * It does pixel to pixel comparison and object comparison.
 */
public class Evaluator {
    public enum ShapeType {dotLike, lineLike}

    public static final int legendHeightOffset = 80;

    private void pixelComparison(BinaryImage toEvaluate, BinaryImage groundTruth, EvaluationData data, boolean createImage) {
        //white pixel match
        BitSet bitsTruth = (BitSet) groundTruth.getBits().clone();
        bitsTruth.and(toEvaluate.getBits());//Pixels that are set in toEvaluate&groundTruth -> white pixel match
        data.setWhiteMatches(bitsTruth.cardinality());
        data.setHitRate(MixedUtils.round(((double) data.getWhiteMatches()) / groundTruth.getPixelSetCount(), 4));
        data.setMissRate(1 - data.getHitRate());


        //false-positive (white in toEvaluate, black in groundTruth)
        BitSet bitsEvaluate = (BitSet) toEvaluate.getBits().clone();
        bitsEvaluate.andNot(groundTruth.getBits());
        data.setFpMatches(bitsEvaluate.cardinality());
        data.setFpRate(MixedUtils.round(((double) data.getFpMatches()) / toEvaluate.getPixelSetCount(), 4));
        //rate of false-positive related to white pixels in evaluate

        //false-negative (black in toEvaluate, white in groundTruth)
        BitSet bitsTruthFN = (BitSet) groundTruth.getBits().clone();
        bitsTruthFN.andNot(toEvaluate.getBits());
        data.setFnMatches(bitsTruthFN.cardinality());
        data.setFnRate(MixedUtils.round(((double) data.getFnMatches()) / ((toEvaluate.getWidth() * toEvaluate.getHeight()) - toEvaluate.getPixelSetCount()), 4));

        if (createImage) data.setDiffImagePixels(createPixelToPixelImage(toEvaluate, groundTruth));
        //rate of false-negative related to black pixels in evaluate, "x% of black pixels are false-negative" -> should actually be white pixels
    }


    private Pair<List<IBinaryImage>, List<IBinaryImage>> objectComparisonLineLike(BinaryImage toEvaluate, BinaryImage groundTruth) {
        //https://pdfs.semanticscholar.org/48a1/2fb85ad6f1f5f0f43f8e48a8ee5f836892f6.pdf
        //probably a good hint for improvement?
        Collection<Collection<Point2D>> linesGT = PointUtils.getClusters(PointUtils.getNeighborsMap(groundTruth.getPoints()));
        Collection<Collection<Point2D>> linesEval = PointUtils.getClusters(PointUtils.getNeighborsMap(toEvaluate.getPoints()));

        List<IBinaryImage> clustersGT = linesGT.stream().map(line -> {
            BinaryImage tmp = new BinaryImage(groundTruth.getWidth(), groundTruth.getHeight());
            line.forEach(p -> tmp.setPixel((int) p.getX(), (int) p.getY()));
            return tmp;
        }).collect(Collectors.toList());

        List<IBinaryImage> clustersEval = linesEval.stream().map(line -> {
            BinaryImage tmp = new BinaryImage(toEvaluate.getWidth(), toEvaluate.getHeight());
            line.forEach(p -> tmp.setPixel((int) p.getX(), (int) p.getY()));
            return tmp;
        }).collect(Collectors.toList());

        return new Pair<>(clustersEval, clustersGT);
    }

    private void objectComparisonDotLike(List<IBinaryImage> clustersEval, List<IBinaryImage> clustersTruth, Settings dp, EvaluationData data) {
        List<Pair<Integer, Pair<Integer, Integer>>> matchAreas = new ArrayList<>();
        List<Pair<Integer, Pair<Integer, Integer>>> nonMatchEval = new ArrayList<>();
        List<Pair<Integer, Pair<Integer, Integer>>> nonMatchTruth = new ArrayList<>();


        final double matchMinPixels = dp.getValueAsDouble(Eval.matchMinPixels);


        int amountFound = 0;
        int amountMissed = 0;
        int multiMatchOneToN = 0;//one object of truth matched more than one of eval
        int multiMatchNToOne = 0;

        for (int i = 0; i < clustersTruth.size(); i++) {
            BinaryImage truth = (BinaryImage) clustersTruth.get(i);
            int truthThresh = (int) Math.ceil((double) truth.getPixelSetCount() * matchMinPixels);
            boolean found = false;
            int currentMatches = 0;

            for (int j = 0; j < clustersEval.size(); j++) {
                BinaryImage eval = (BinaryImage) clustersEval.get(j);
                BitSet bitsTruth = (BitSet) truth.getBits().clone();
                bitsTruth.and(eval.getBits());
                if (bitsTruth.cardinality() >= truthThresh) {//its a match
                    if (!found) {
                        //just do this part once
                        matchAreas.add(new Pair<>(truth.getPixelSetCount(), new Pair<>(eval.getPixelSetCount(), bitsTruth.cardinality())));
                        //j=clustersEval.size();//exit inner loop
                        found = true;
                    }
                    currentMatches++;
                }
            }
            if (currentMatches > 1) multiMatchOneToN++;

            if (found) amountFound++;
            else {
                amountMissed++;
                nonMatchTruth.add(new Pair<>(truth.getPixelSetCount(), new Pair<>(0, 0)));
            }
        }

        data.setObjectsEval(clustersEval.size());
        data.setObjectsTruth(clustersTruth.size());
        data.setMultiMatchesOneToN(multiMatchOneToN);
        data.setMatchAreas(matchAreas);
        data.setObjectsFound(amountFound);
        data.setObjectsMissed(amountMissed);


        //fp-object is contained in clustersEval and no match in clustersTruth ---> for this the for loop has to be inverted (inner loop on the outside etc.)
        //fn-object is contained in clustersTruth and no match in clustersEval ---> this is amountMissed
        int amountFP = 0;//false-positive loop
        for (int i = 0; i < clustersEval.size(); i++) {
            BinaryImage eval = (BinaryImage) clustersEval.get(i);
            boolean found = false;
            int currentMatch = 0;

            for (int j = 0; j < clustersTruth.size(); j++) {
                BinaryImage truth = (BinaryImage) clustersTruth.get(j);
                int truthThresh = (int) Math.ceil((double) truth.getPixelSetCount() * matchMinPixels);
                BitSet bitsTruth = (BitSet) truth.getBits().clone();
                bitsTruth.and(eval.getBits());
                if (bitsTruth.cardinality() >= truthThresh) {//its a match
                    found = true;
                    currentMatch++;
                }
            }
            if (currentMatch > 1) multiMatchNToOne++;
            if (!found) {
                amountFP++;
                nonMatchEval.add(new Pair<>(0, new Pair<>(eval.getPixelSetCount(), 0)));
            }
        }

        data.setObjectsFP(amountFP);
        data.setMultiMatchesNToOne(multiMatchNToOne);
        data.setNonMatchEval(nonMatchEval);
        data.setNonMatchTruth(nonMatchTruth);

    }


    private void objectComparison(BinaryImage toEvaluate, BinaryImage groundTruth, ShapeType shapeType, EvaluationData data, Settings dp) throws Exception {
        //cluster both images, get every cluster of white pixels and compare them

        //dot-like shapes -> any non line shapes are ok with Calc::largestObject
        //use different "recognition algorithm" for lines

        //lines should be described by points and no binaryimage used
        //there should be some kind of tolerance for shifting lines a few pixels (only 1 pixel width lines on groundtruth and eval
        //which means it could be easily not get counted as match even if the line exists, just because its shifted by a few pixels

        //this line matching could be done by using the filament sensor? just scan both images for filaments and see what happens?
        //probably not the fastest way (long computation time), but the easiest and pretty robust?
        List<IBinaryImage> clustersEval;
        List<IBinaryImage> clustersTruth;

        if (shapeType.equals(ShapeType.lineLike)) {
            Pair<List<IBinaryImage>, List<IBinaryImage>> listListPair = objectComparisonLineLike(toEvaluate, groundTruth);
            clustersEval = listListPair.getKey();
            clustersTruth = listListPair.getValue();

        } else {
            clustersEval = Calc.largestObject(toEvaluate, 1);
            clustersTruth = Calc.largestObject(groundTruth, 1);
        }
        if (clustersEval.size() == 0) {
            System.out.println("DEBUG");

        }
        objectComparisonDotLike(clustersEval, clustersTruth, dp, data);
        //metrics: found objects(%), missed objects(%), fp-objects, fn-objects
        if (dp.getValueAsBoolean(Eval.createImage))
            data.setDiffImageObjects(createObjectToObjectImage(clustersEval, clustersTruth, dp));


        //second one, compare fibers/adhesion's found on a more abstract level;
        //check if fiber/adhesion are about the same length, position
        //metrics: found object(%), size deviations,...
    }


    /**
     * Method for only Evaluating Pixel-Data
     *
     * @param toEvaluate
     * @param groundTruth
     * @return
     */
    public EvaluationData onlyPixel(BinaryImage toEvaluate, BinaryImage groundTruth) {
        if (toEvaluate.getWidth() != groundTruth.getWidth() || toEvaluate.getHeight() != groundTruth.getHeight()) {
            throw new IllegalArgumentException("Evaluator::evaluate()  --- toEvaluate and groundTruth are not the same size(" +
                    toEvaluate.getWidth() + " x " + toEvaluate.getHeight() + ";" + groundTruth.getWidth() + " x " + groundTruth.getHeight() + ")");
        }
        EvaluationData data = new EvaluationData();

        toEvaluate.flush();//flush all changes
        groundTruth.flush();
        toEvaluate.close();//close byte processors
        groundTruth.close();

        toEvaluate = toEvaluate.clone();
        groundTruth = groundTruth.clone();

        data.setWidthEval(toEvaluate.getWidth());
        data.setHeightEval(toEvaluate.getHeight());
        data.setWidthTruth(groundTruth.getWidth());
        data.setHeightTruth(groundTruth.getHeight());

        data.setWhiteEval(toEvaluate.getPixelSetCount());
        data.setWhiteTruth(groundTruth.getPixelSetCount());

        pixelComparison(toEvaluate, groundTruth, data, false);
        return data;

    }


    /**
     * Should also produce a image result with colors
     *
     * @param toEvaluate
     * @param groundTruth
     * @return
     */
    public EvaluationData evaluate(BinaryImage toEvaluate, BinaryImage groundTruth, ShapeType shapeType, Settings dp, int debugIdentifier) throws Exception {
        if (toEvaluate.getWidth() != groundTruth.getWidth() || toEvaluate.getHeight() != groundTruth.getHeight()) {
            throw new IllegalArgumentException("Evaluator::evaluate()  --- toEvaluate and groundTruth are not the same size(" +
                    toEvaluate.getWidth() + " x " + toEvaluate.getHeight() + ";" + groundTruth.getWidth() + " x " + groundTruth.getHeight() + ")");
        }
        EvaluationData data = new EvaluationData();

        toEvaluate.flush();//flush all changes
        groundTruth.flush();
        toEvaluate.close();//close byte processors
        groundTruth.close();

        toEvaluate = toEvaluate.clone();
        groundTruth = groundTruth.clone();
        //System.out.println("Evaluator::evaluate() -- blackBackground=" + Prefs.blackBackground);

        if (dp.getValueAsBoolean(Eval.thickenLines)) {
            System.out.println("Evaluator::evaluate() -- thicken lines");
            //the images get inverted if !blackBackground set so erode is actually a dilate
            Binary binary = new Binary();

            if (Prefs.blackBackground) binary.setup("dilate", null);
            else binary.setup("erode", null);

            binary.run(toEvaluate.getByteProcessor());
            binary.run(groundTruth.getByteProcessor());

            ImageWindow window = new ImageWindow(new ImagePlus("afterDilateEval", toEvaluate.getByteProcessor().duplicate()));
            window.setVisible(true);
            ImageWindow window2 = new ImageWindow(new ImagePlus("afterDilateGT", groundTruth.getByteProcessor().duplicate()));
            window2.setVisible(true);

            toEvaluate.flush();
            groundTruth.flush();
            toEvaluate.close();//close byte processors
            groundTruth.close();
        }


        data.setWidthEval(toEvaluate.getWidth());
        data.setHeightEval(toEvaluate.getHeight());
        data.setWidthTruth(groundTruth.getWidth());
        data.setHeightTruth(groundTruth.getHeight());

        data.setWhiteEval(toEvaluate.getPixelSetCount());
        data.setWhiteTruth(groundTruth.getPixelSetCount());

        //if nothing to evaluate just return


        pixelComparison(toEvaluate, groundTruth, data, dp.getValueAsBoolean(Eval.createImage));
        objectComparison(toEvaluate, groundTruth, shapeType, data, dp);
        //pixel comparison, match% only match white pixels direct comparison of toEvaluate and groundTruth white pixels
        System.out.println("Evaluator::evaluate() --- finished(" + debugIdentifier + ")");

        return data;

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

    /**
     * @param toEvaluate
     * @param groundTruth
     * Pixel order: groundTruth, toEvaluate, intersection
     * color: gT=orange,toEvaluate=red,intersection=blue
     */
    private ImagePlus createPixelToPixelImage(@Annotations.NotNull BinaryImage toEvaluate, @Annotations.NotNull BinaryImage groundTruth) {
        Objects.requireNonNull(toEvaluate, "toEvaluate is null");
        Objects.requireNonNull(groundTruth, "groundTruth is null");

        BinaryImage tmpAnd = toEvaluate.clone();
        tmpAnd.and(groundTruth);

        final int width = toEvaluate.getWidth(), height = toEvaluate.getHeight();
        final int red = Color.red.getRGB(), orange = Color.orange.getRGB(), blue = Color.cyan.getRGB();

        ImageProcessor ip = new ColorProcessor(width, height + legendHeightOffset);
        ip.setColor(Color.black);
        ip.fillRect(0, 0, width, height);

        int[][] arr = ip.getIntArray();
        for (int w = 0; w < width; w++)
            for (int h = 0; h < height; h++) {
                if (groundTruth.getPixel(w, h)) arr[w][h] = orange;
                if (toEvaluate.getPixel(w, h)) arr[w][h] = red;
                if (tmpAnd.getPixel(w, h)) arr[w][h] = blue;
            }

        ip.setIntArray(arr);
        printLegendOnImage(ip, red, orange, blue, height);

        return new ImagePlus("Comparison(Pixel) Eval-GroundTruth", ip);
    }

    private ImagePlus createObjectToObjectImage(List<IBinaryImage> clustersEval, List<IBinaryImage> clustersTruth, Settings dp) throws Exception {
        Objects.requireNonNull(clustersEval, "clustersEval is null");
        Objects.requireNonNull(clustersTruth, "clustersEval is null");
        //if (clustersEval.size() == 0) throw new IllegalArgumentException("clustersEval has no content");
        //if (clustersTruth.size() == 0) throw new IllegalArgumentException("clustersTruth has no content");

        final double matchMinPixels = dp.getValueAsDouble(Eval.matchMinPixels);
        if (clustersEval.size() == 0 && clustersTruth.size() == 0) {
            throw new Exception("No Content Found");
        }


        final int width = (clustersEval.size() != 0) ? clustersEval.get(0).getWidth() : clustersTruth.get(0).getWidth(),
                height = (clustersEval.size() != 0) ? clustersEval.get(0).getHeight() : clustersTruth.get(0).getHeight();
        final int red = Color.red.getRGB(), orange = Color.orange.getRGB(), blue = Color.cyan.getRGB();

        ImageProcessor ip = new ColorProcessor(width, height + legendHeightOffset);
        ip.setColor(Color.black);
        ip.fillRect(0, 0, width, height);
        int[][] arr = ip.getIntArray();

        for (int i = 0; i < clustersTruth.size(); i++) {
            BinaryImage truth = (BinaryImage) clustersTruth.get(i);
            boolean found = false;
            truth.getPoints().forEach(point -> arr[(int) point.getX()][(int) point.getY()] = orange);//paint GT
            int truthThresh = (int) Math.ceil((double) truth.getPixelSetCount() * matchMinPixels);
            for (int j = 0; j < clustersEval.size(); j++) {
                BinaryImage eval = (BinaryImage) clustersEval.get(j);
                BitSet bitsTruth = (BitSet) truth.getBits().clone();
                bitsTruth.and(eval.getBits());
                if (bitsTruth.cardinality() >= truthThresh) {//its a match
                    if (!found) {
                        //just do this part once
                        truth.getPoints().forEach(point -> arr[(int) point.getX()][(int) point.getY()] = blue);//is a match mark the whole object
                        //j=clustersEval.size();//exit inner loop
                        found = true;
                    }
                }
            }
        }
        for (int i = 0; i < clustersEval.size(); i++) {
            BinaryImage eval = (BinaryImage) clustersEval.get(i);
            boolean found = false;
            for (int j = 0; j < clustersTruth.size(); j++) {
                BinaryImage truth = (BinaryImage) clustersTruth.get(j);
                BitSet bitsTruth = (BitSet) truth.getBits().clone();
                int truthThresh = (int) Math.ceil((double) truth.getPixelSetCount() * matchMinPixels);
                bitsTruth.and(eval.getBits());
                if (bitsTruth.cardinality() >= truthThresh) {//its a match
                    found = true;
                }
            }
            if (!found) {
                eval.getPoints().forEach(point -> arr[(int) point.getX()][(int) point.getY()] = red);
            }
        }


        ip.setIntArray(arr);

        printLegendOnImage(ip, red, orange, blue, height);

        return new ImagePlus("Comparison(Object Eval-GroundTruth", ip);
    }


}
