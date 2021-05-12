package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;
import util.Annotations;

@Annotations.FilterUI
public class FilterDilate implements IImageFilter {

    private transient Binary binary;

    public FilterDilate() {
        binary = new Binary();
        binary.setup("dilate", null);
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
