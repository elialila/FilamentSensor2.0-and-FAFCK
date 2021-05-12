package core.cell.plugins;

import core.Calc;
import filters.*;
import ij.process.ImageProcessor;
import core.cell.CellPlugin;
import core.image.BinaryImage;
import core.image.IBinaryImage;

import java.util.List;


public class CellPluginBenjamin extends CellPlugin {

    @Override
    protected List<IBinaryImage> getShapes(ImageProcessor image, IBinaryImage mask, int minArea) {

        final int width = image.getWidth();
        final int height = image.getHeight();

        image.blurGaussian(2.1);

        int[][] tmp = image.getIntArray();

        //   final FilterPreprocessing gauss1 = new FilterGauss(0.1);
        //   final FilterPreprocessing gauss2 = new FilterGauss(2.0);
        //   tmp = gauss2.apply(gauss1.apply(tmp)); // Strangely, this gives a 1.5 second speed up.

        HistogramMethodsLegacy li = new HistogramMethodsLegacy(tmp, true, HistogramMethodsLegacy.LI);
        double ratio = 0.5;
        while (tooWhite(li.binaryImage(), ratio) && li.threshold() > 0) {
            EnhanceContrastLegacy enhance = new EnhanceContrastLegacy(li.threshold(), 255);
            tmp = enhance.apply(tmp);
            li = new HistogramMethodsLegacy(tmp, true, HistogramMethodsLegacy.LI);
            ratio += 0.02;
        }

        IBinaryImage binary_image = new BinaryImage(Calc.close(li.binaryImage(), Calc.circleMask(20)));
        if (mask != null && mask.getWidth() == width && mask.getHeight() == height) {
            if (mask.isInMemoryState()) {
                mask = mask.clone();
                mask.exitMemoryState();
            }
            binary_image.and(mask);
        }

        List<IBinaryImage> shapes = Calc.largestObject(binary_image, minArea);
        shapes.parallelStream().forEach(bin -> {
            CloseHolesLegacy.apply(bin, 0.01);
        });
        return shapes;
        //binary_image = FilterCloseHoles.apply(largestObject(binary_image), 0.01);
        //return new PluginCellShape(li.binaryImage(), mask);
    }

    @Override
    protected List<IBinaryImage> getExtendedShapes(ImageProcessor image, IBinaryImage mask, int minArea) {

        image.blurGaussian(2.1);


        int[][] tmp = image.getIntArray();
        //final FilterPreprocessing gauss1 = new FilterGauss(0.1);
        //final FilterPreprocessing gauss2 = new FilterGauss(2.0);
        //tmp = gauss2.apply(gauss1.apply(tmp)); // Strangely, this gives a 1.5 second speed up.

        double ratio = 0.5;
        HistogramMethodsLegacy triangle = new HistogramMethodsLegacy(tmp, true, HistogramMethodsLegacy.TRIANGLE);
        while (tooWhite(triangle.binaryImage(), ratio) && triangle.threshold() > 0) {
            EnhanceContrastLegacy enhance = new EnhanceContrastLegacy(triangle.threshold(), 255);
            tmp = enhance.apply(tmp);
            triangle = new HistogramMethodsLegacy(tmp, true, HistogramMethodsLegacy.TRIANGLE);
            ratio += 0.02;
        }
        return Calc.largestObject(new BinaryImage(Calc.close(triangle.binaryImage(), Calc.circleMask(20))), minArea);
        //return largestObject(Calc.close(triangle.binaryImage(), Calc.circleMask(20)));
    }


    private boolean tooWhite(final boolean[][] binary_image, final double ratio) {
        final int width = binary_image.length;
        final int height = binary_image[0].length;
        final int white_threshold = (int) (width * height * ratio);
        final int black_threshold = width * height - white_threshold;
        int white_pixels = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (binary_image[x][y]) {
                    white_pixels++;
                }
            }
            if (white_pixels > white_threshold) {
                return true;
            }
            if (x * height - white_pixels > black_threshold) {
                return false;
            }
        }
        return false;
    }


}
