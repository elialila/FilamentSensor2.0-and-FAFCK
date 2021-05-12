package filters;

import ij.process.ImageProcessor;

public class FilterInvert implements IImageFilter {
    @Override
    public boolean forceParallel() {
        return true;
    }

    @Override
    public void run(ImageProcessor image) {
        image.invert();
    }
}
