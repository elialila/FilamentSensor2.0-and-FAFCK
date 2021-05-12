package thopt.core;

import core.settings.Settings;
import filters.FilterManualThreshold;
import ij.process.AutoThresholder;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Pair;
import core.FilterQueue;
import evaluation.EvaluationData;
import evaluation.Evaluator;
import core.image.BinaryImage;
import core.image.ImageWrapper;
import thopt.model.ResultModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FAThresholdOptimizer {
    public static final double ThreshStart = 0.5;//range half


    private double calcScore(double[] last, double[] currentVal) {
        double score = 0;
        score -= (last[0] - currentVal[0]);//if hitRate gets better increase score
        score += (last[1] - currentVal[1]); //if fpRate gets better increase score
        score += (last[2] - currentVal[2]);//if fnRate gets better increase score
        //fnRate probably needs a weight
        return score;
    }


    public List<ResultModel> run(List<Pair<File, File>> sampleData, AutoThresholder.Method checkAgainst, int manualCheckAgainst) {
        Settings dp = new Settings();

        Evaluator evaluator = new Evaluator();
        List<ResultModel> thresholds = new ArrayList<>();

        sampleData.forEach(pair -> {
            File truth = pair.getValue();
            File sample = pair.getKey();
            if (sample.getName().contains("17Fcell2")) {
                System.out.println("debug");
            }
            BinaryImage binTruth = new BinaryImage(truth);
            ImageWrapper wrapper = new ImageWrapper(sample, dp);
            if (wrapper.getWorker() != null) {
                try {
                    wrapper.getWorker().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            FilterQueue queue = new FilterQueue();
            FilterManualThreshold threshold = new FilterManualThreshold();
            queue.add(threshold);
            int depth = wrapper.getImage().getBitDepth();
            double val = Math.pow(2, depth);
            boolean running = true;
            //start with "black image" params
            double[] last = new double[]{0, 0, 1};//hitrate,fprate,fnrate
            double lastVal = val;

            int cnt = 1;
            double step;
            while (running) {
                step = Math.pow(2, depth - cnt);
                cnt++;
                threshold.setThreshold((int) val);

                ImageWrapper current = wrapper.clone();
                queue.run(current, (f) -> {
                });
                EvaluationData result = evaluator.onlyPixel(new BinaryImage(current.getEntryList().get(0).getProcessor().convertToByteProcessor()), binTruth);
                /*
                ImageWindow win1 = new ImageWindow(current.getImage());
                win1.setVisible(true);
                ImageWindow win2 = new ImageWindow(new ImagePlus("Truth", binTruth.getByteProcessor()));
                win2.setVisible(true);*/

                //black image = hitRate=0;fpRate=0;fnRate=1; 2 of 3 Parameters bad
                //white image = hitRate=1;fpRate=high;fnRate=0; 2 of 3 Parameters good -> should be counted as bad

                double[] currentVal = new double[]{result.getHitRate(), result.getFpRate(), result.getFnRate()};
                double score = calcScore(last, currentVal);
                if (score != 0) {
                    lastVal = val;
                    //change val to left if high fnRate
                    //change val to right if high fpRate
                    if (last[2] < currentVal[2] || currentVal[0] < 0.7) {//prioritize hitRate > 70% higher than other parameters
                        val -= step;
                    } else if (last[1] < currentVal[1]) {
                        val += step;
                    }
                    if (val < 0 || val >= Math.pow(2, depth)) {
                        running = false;
                        val = lastVal;
                    } else {
                        last = currentVal;
                    }
                } else {
                    running = false;
                }
            }

            final int[] histogram = wrapper.getEntryList().get(0).getProcessor().getHistogram();
            final int endVal = (int) val;
            IntegerProperty nearest = new SimpleIntegerProperty(10000000);
            ObjectProperty<AutoThresholder.Method> nearestMethod = new SimpleObjectProperty<>(null);
            Arrays.stream(AutoThresholder.Method.values()).forEach(method -> {
                AutoThresholder tmp = new AutoThresholder();
                int thresh = tmp.getThreshold(method, histogram);
                if (Math.abs((thresh - endVal)) < Math.abs((nearest.get() - endVal))) {
                    nearest.set(thresh);
                    nearestMethod.setValue(method);
                }
            });

            ResultModel model = new ResultModel(pair.getKey().getAbsolutePath(), (int) val, last[0], last[1], last[2],
                    nearestMethod.get(), nearest.get());

            AutoThresholder tmp = new AutoThresholder();
            int iCheckAgainst = tmp.getThreshold(checkAgainst, histogram);
            ImageWrapper checkAgainstWrapper = wrapper.clone();
            threshold.setThreshold(iCheckAgainst);
            queue.run(checkAgainstWrapper, f -> {
            });
            EvaluationData result = evaluator.onlyPixel(new BinaryImage(checkAgainstWrapper.getEntryList().get(0).getProcessor().convertToByteProcessor()), binTruth);
            model.setHitChk(result.getHitRate());
            model.setfPChk(result.getFpRate());
            model.setCheckAgainst(iCheckAgainst);

            if (manualCheckAgainst >= 0) {
                ImageWrapper checkAgainstWrapper2 = wrapper.clone();
                threshold.setThreshold(manualCheckAgainst);
                queue.run(checkAgainstWrapper2, f -> {
                });
                EvaluationData result2 = evaluator.onlyPixel(new BinaryImage(checkAgainstWrapper2.getEntryList().get(0).getProcessor().convertToByteProcessor()), binTruth);
                model.setfPChkMan(result2.getFpRate());
                model.setHitChkMan(result2.getHitRate());

            }


            thresholds.add(model);
            //calculate manual-optimal threshold for image pair
        });
        return thresholds;
    }


}
