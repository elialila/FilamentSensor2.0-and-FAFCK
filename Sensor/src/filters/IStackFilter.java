package filters;

import ij.ImagePlus;

public interface IStackFilter extends IFilter {

    /**
     * Changes the input image
     * apply's the filter which is implemented
     *
     * @param image
     */
    void run(ImagePlus image);


}
