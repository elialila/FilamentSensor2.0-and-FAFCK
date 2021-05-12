package filters;

import ij.plugin.filter.Binary;
import ij.process.ImageProcessor;
import util.Annotations;

@Annotations.FilterUI
public class FilterErode implements IImageFilter {

    private transient Binary binary;

    public FilterErode() {
        binary = new Binary();
        binary.setup("erode", null);
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
