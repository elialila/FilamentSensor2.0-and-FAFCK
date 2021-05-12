package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;
import util.Annotations;

@Annotations.FilterUI
public class FilterOpening implements IImageFilter {

    private transient Binary binary;

    public FilterOpening() {
        binary = new Binary();
        binary.setup("open", null);
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
