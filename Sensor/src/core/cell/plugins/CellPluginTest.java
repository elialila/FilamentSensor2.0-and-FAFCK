package core.cell.plugins;

import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import core.image.IBinaryImage;

import java.util.List;

public class CellPluginTest extends CellPluginSimple {
    @Override
    protected List<IBinaryImage> getShapes(ImageProcessor image, IBinaryImage mask, int minArea) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        image.blurGaussian(2.0);
        //adjust Li threshold a bit for more tolerance?


        AutoThresholder autoThresholder = new AutoThresholder();
        int liThresh = autoThresholder.getThreshold(AutoThresholder.Method.Li, image.getHistogram());
        int triThresh = autoThresholder.getThreshold(AutoThresholder.Method.Triangle, image.getHistogram());

        //this could be extended by weighting the thresholds, for example 75% weight to li and only 25% to triangle
        int thresh = (liThresh + triThresh) / 2;


        IBinaryImage img = getCellImage(image, thresh);

        return preProcess(img, mask, width, height, 7, minArea);
    }
}
