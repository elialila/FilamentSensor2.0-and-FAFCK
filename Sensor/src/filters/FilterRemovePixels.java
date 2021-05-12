package filters;

import ij.process.ImageProcessor;

import static filters.FilterBinarization.HIGH;
import static filters.FilterBinarization.LOW;

public class FilterRemovePixels implements IImageFilter {

    public FilterRemovePixels() {
    }

    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor image) {
        int[][] imageData = image.getIntArray();
        for (int i = 1; i < image.getWidth() - 1; i++) {
            for (int j = 1; j < image.getHeight() - 1; j++) {
                if (imageData[i][j] == HIGH && imageData[i - 1][j] != HIGH && imageData[i][j - 1] != HIGH
                        && imageData[i + 1][j] != HIGH && imageData[i][j + 1] != HIGH) {
                    imageData[i][j] = LOW;
                }
            }
        }
        image.setIntArray(imageData);
    }

}
