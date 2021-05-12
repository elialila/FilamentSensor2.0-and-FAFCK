package filters;

import ij.process.ImageProcessor;

public interface IImageFilter extends IFilter {

    /**
     * In combination with FilterQueue, it forces the queue to work the current filter on parallel mode
     *
     * @return
     */
    boolean forceParallel();

    /**
     * Changes the input image
     * apply's the filter which is implemented
     *
     * @param image
     */
    void run(ImageProcessor image);
}
