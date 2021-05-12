package core.calculation;

import core.Const;
import ij.process.ImageProcessor;
import core.image.IBinaryImage;


public class MeanBrightnessCalc {

    private MeanBrightnessCalc() {
    }

    /**
     * Calculates the average brightness of interior- and boundary-area
     *
     * @param mask      interior mask
     * @param cell      cell area shape
     * @param processor processor containing the actual image
     * @return long[]{interiorArea,interiorAverageBrightness,boundaryAverageBrightness}
     */
    public static long[] areaAndMeanBrightness(IBinaryImage mask, IBinaryImage cell, ImageProcessor processor) {
        if (mask.isInMemoryState()) {
            mask = mask.clone();
            mask.exitMemoryState();
        }
        if (cell.isInMemoryState()) {
            cell = cell.clone();
            cell.exitMemoryState();
        }

        int[][] image = processor.getIntArray();
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        long[] out = new long[3];
        long count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (cell.getPixel(x, y)) {
                    if (mask.getPixel(x, y)) {
                        out[0]++;//count how many pixels are inside cellArea&&interior
                        out[1] += image[x][y];//sum pixels inside cellArea&&interior
                    } else {
                        count++;//count pixels boundary
                        out[2] += image[x][y];//sum pixels boundary
                    }
                }
            }
        }
        //calculate average values(Const.M is just for preventing floating points)
        out[1] = (out[1] * Const.M) / Math.max(1, out[0]);//prevent division by 0
        out[2] = (out[2] * Const.M) / Math.max(1, count);
        //do not create image here
        /*if (count > 0) {
            m_buffered_image_red_green = ImageFactory.makeTwoLevelRedGreenImage(image, mask, cell);
        }*/
        return out;
    }
}
