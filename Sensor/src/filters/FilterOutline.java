package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;

public class FilterOutline implements IImageFilter {

    private transient Binary binary;

    public FilterOutline() {
        binary = new Binary();
        binary.setup("outline", null);
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
