package filters;

import util.Annotations.Nullable;
import ij.process.ImageProcessor;
import core.image.IBinaryImage;

public interface IImageFilterAreaDependant extends IFilter {

    /**
     * Changes the input image
     * apply's the filter which is implemented
     *
     * @param image
     */
    void run(ImageProcessor image, @Nullable IBinaryImage cellArea);

    /**
     * Returns a boolean value which tells if the area should be the extended area or the actual area
     * true = actual area; false = extended area
     *
     * @return
     */
    boolean isAreaOrExtArea();

}
