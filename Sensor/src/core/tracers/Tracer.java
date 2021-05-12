package core.tracers;

import core.filaments.AbstractFilament;
import ij.process.ImageProcessor;
import util.NotImplementedException;
import core.image.IBinaryImage;

import java.util.List;

//it only defines methods ---> interface
public interface Tracer {
    int[][] getOrientationField();

    IBinaryImage getPixelMask();

    int[][] getWidthMap();

    List<AbstractFilament> scanFilaments(ImageProcessor bin, double tolerance, int min_length, int min_angle, int step);

    int calcWidthMap(ImageProcessor bin, double tolerance);

    static int[][] makeFingerprint(double axis, int[][] orientationField) {
        throw new NotImplementedException();
    }

    /**
     * @param binImage resulting image from line sensor as boolean[][]
     */
    void updateFilamentWidth(boolean[][] binImage);


}
