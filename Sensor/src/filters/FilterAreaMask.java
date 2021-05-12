package filters;


import ij.Prefs;
import ij.process.ImageProcessor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import core.image.IBinaryImage;
import util.Annotations;

import java.util.Objects;

@Annotations.FilterUI
public class FilterAreaMask implements IImageFilterAreaDependant {

    private transient IntegerProperty background;
    @Annotations.FilterUIField(type = Annotations.FilterUIType.checkbox, label = "Area(set) or ExtArea(not set)")
    private transient BooleanProperty areaOrExtArea;


    public FilterAreaMask() {
        background = new SimpleIntegerProperty(-1);
        areaOrExtArea = new SimpleBooleanProperty(false);
    }

    public BooleanProperty areaOrExtAreaProperty() {
        return areaOrExtArea;
    }

    public void setAreaOrExtArea(boolean areaOrExtArea) {
        this.areaOrExtArea.set(areaOrExtArea);
    }

    public int getBackground() {
        return background.get();
    }

    public IntegerProperty backgroundProperty() {
        return background;
    }

    public void setBackground(int background) {
        this.background.set(background);
    }

    @Override
    public void run(ImageProcessor image, IBinaryImage cellArea) {
        Objects.requireNonNull(cellArea, "FilterAreaMask:run() --- cellArea is null");
        double white = image.getMax();
        double black = image.getMin();

        double background;
        if (Prefs.blackBackground) {
            background = black;
        } else {
            background = white;
        }
        if (image.isInvertedLut()) {
            background = white - background;
        }

        if (cellArea.isInMemoryState()) {
            cellArea = cellArea.clone();
            cellArea.exitMemoryState();
        }

        if (image.getWidth() == cellArea.getWidth() && image.getHeight() == cellArea.getHeight()) {
            int[][] imageData = image.getIntArray();
            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    if (!cellArea.getPixel(i, j)) {
                        imageData[i][j] = (int) background;
                    }
                }
            }
            image.setIntArray(imageData);
        }
    }

    @Override
    public boolean isAreaOrExtArea() {
        return areaOrExtArea.get();
    }
}
