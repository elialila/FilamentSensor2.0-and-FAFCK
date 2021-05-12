package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;

public class FilterSkeletonize implements IImageFilter {

    private transient Binary binary;

    public FilterSkeletonize() {
        binary = new Binary();
        binary.setup("skel", null);
    }


    @Override
    public boolean forceParallel() {
        return false;
    }

    @Override
    public void run(ImageProcessor image) {
        binary.run(image);
    }
}
