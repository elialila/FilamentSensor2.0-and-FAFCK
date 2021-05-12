package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;
import util.Annotations;

@Annotations.FilterUI
public class FilterClosing implements IImageFilter {

    private transient Binary binary;


    public FilterClosing() {
        binary = new Binary();
        binary.setup("close", null);
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
