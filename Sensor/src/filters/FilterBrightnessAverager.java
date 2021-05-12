package filters;

import core.FilamentSensor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Averages the brightness(of a gray scale image-stack).
 */
public class FilterBrightnessAverager implements IStackFilter {


    @Override
    public void run(ImagePlus image) {

        if (image.getStackSize() <= 1) return;

        final int pixels = image.getWidth() * image.getHeight();

        long time = System.currentTimeMillis();

        ImageStack stack = image.getStack();

        double max = image.getProcessor().getMax();
        double min = image.getProcessor().getMin();

        System.out.println("FilterBrightnessAverager::run() --- min,max=" + min + "," + max);


        //calc "brightness" of stack, for each image sum all pixels and divide by image size
        List<Double> avgs = new ArrayList<>();
        IntStream.range(1, stack.getSize() + 1).forEach(i ->
                avgs.add((double) (Arrays.stream(stack.getProcessor(i).getIntArray()).
                        flatMapToInt(Arrays::stream).sum()) / pixels)
        );
        //calculate the average of the whole stack
        final double stackAvg = avgs.stream().mapToDouble(d -> d).average().orElse(-1);
        FilamentSensor.debugMessage("FilterBrightnessAverager::run() --- stackAVG=" + stackAvg);


        //a future improvement could be if "foreground" pixels could be treated a bit different to take them even further into foreground
        //and set the background more into background

        for (int i = 1; i <= stack.getSize(); i++) {
            ImageProcessor ip = stack.getProcessor(i);
            double avg = avgs.get(i - 1);
            FilamentSensor.debugMessage("FilterBrightnessAverager::run() --- imgAVG=" + avg);
            double diff = stackAvg - avg;//calculate difference between image brightness and stack brightness
            int[][] img = ip.getIntArray();
            for (int x = 0; x < ip.getWidth(); x++)
                for (int y = 0; y < ip.getHeight(); y++) {
                    //add difference to each pixel to "normalize" brightness over the whole stack
                    int val = img[x][y] + (int) Math.ceil(diff);
                    if (val > max) val = (int) max;
                    if (val < 0) val = 0;
                    img[x][y] = val;
                }
            ip.setIntArray(img);
        }
        FilamentSensor.debugPerformance("Time taken:", time);
        //get brightness each image, calc avg per image (sum(processor.getIntArray))/(width*height)
        //average over whole stack sum(avgImg)/sliceCount

        //get diff between img-avg and stack-avg; add diff to each pixel in image(add or substract)


    }
}
