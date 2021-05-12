package core.cell.plugins;

import core.Calc;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import core.cell.CellPlugin;
import filters.FilterClosing;
import filters.FilterFillHoles;
import core.image.IBinaryImage;

import java.util.List;


/**
 * Removed circular area extension(dilate or if black/white swapped erode) error-prone, but more precise (if the image quality is high enough)
 * This class was for testing other area calculation methods
 * To improve area calculation as a whole there should be a test for other algorithms (not only Li and Triangle)
 */
public class CellPluginSimple extends CellPlugin {


    /**
     * This is the last method where the binary images should be changed
     *
     * @param binary_image
     * @param mask
     * @param width
     * @param height
     * @param circleDiameter
     * @param minArea
     * @return
     */
    protected static List<IBinaryImage> preProcess(IBinaryImage binary_image, IBinaryImage mask, final int width, final int height, int circleDiameter, final int minArea) {
        IBinaryImage maskBinaryImage = Calc.circleMaskBinary(circleDiameter);//30 was used for binImage and 20 for tri image


        binary_image.flush();
        if (circleDiameter > 0) {
            //check if dilate is done correctly
            binary_image.dilate(maskBinaryImage);
        } else {
            new FilterClosing().run(binary_image.getByteProcessor());
        }
        if (mask != null && mask.getWidth() == width && mask.getHeight() == height) {
            if (mask.isInMemoryState()) {
                mask = mask.clone();
                mask.exitMemoryState();
            }
            binary_image.and(mask);
        }
        new FilterFillHoles().run(binary_image.getByteProcessor());

        return Calc.largestObject(binary_image, minArea);
    }

    @Override
    protected List<IBinaryImage> getShapes(ImageProcessor image, IBinaryImage mask, int minArea) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        image.blurGaussian(2.0);
        //adjust Li threshold a bit for more tolerance?
        IBinaryImage img = getCellImage(image, AutoThresholder.Method.Li);
        return preProcess(img, mask, width, height, 0, minArea);
    }

    @Override
    protected List<IBinaryImage> getExtendedShapes(ImageProcessor image, IBinaryImage mask, int minArea) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        image.blurGaussian(2.0);
        IBinaryImage img = getCellImage(image, AutoThresholder.Method.Triangle);
        return preProcess(img, mask, width, height, 20, minArea);
    }
}
