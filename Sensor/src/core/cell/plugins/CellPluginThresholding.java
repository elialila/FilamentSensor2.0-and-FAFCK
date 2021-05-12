package core.cell.plugins;

import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import javafx.beans.property.*;
import core.image.IBinaryImage;

import java.util.List;

/**
 * @todo Test this class' behaviour (true false in the binary image), if it produces correct results and integrate it
 */
public class CellPluginThresholding extends CellPluginSimple {

    private AutoThresholder autoThresholder;
    private IntegerProperty thresholdShape;
    private IntegerProperty thresholdExtendedShape;

    private ObjectProperty<AutoThresholder.Method> thresholdMethod;
    private ObjectProperty<AutoThresholder.Method> thresholdMethodExt;

    private BooleanProperty manualThresholding;//true => manual thresholding will be used, false => automatic will be used

    //@todo pro bild manueller threshold wird auch gewünscht, lösung überlegen irgendwo muss die bild<->threshold relation herkommen
    //@todo und zwischengespeichert werden

    //prinzipiell die bilder im stack sind ja numerisch zugeordnet (bild1 = 1, etc.) dh es reicht eine arraylist mit thresholds


    public CellPluginThresholding() {
        super();
        autoThresholder = new AutoThresholder();
        thresholdShape = new SimpleIntegerProperty();
        thresholdExtendedShape = new SimpleIntegerProperty();
        thresholdMethod = new SimpleObjectProperty<>();
        thresholdMethodExt = new SimpleObjectProperty<>();
        manualThresholding = new SimpleBooleanProperty();
        //the threshold value is taken from the area plugin

    }

    public AutoThresholder getAutoThresholder() {
        return autoThresholder;
    }

    public boolean isManualThresholding() {
        return manualThresholding.get();
    }

    public BooleanProperty manualThresholdingProperty() {
        return manualThresholding;
    }

    public AutoThresholder.Method getThresholdMethod() {
        return thresholdMethod.get();
    }

    public ObjectProperty<AutoThresholder.Method> thresholdMethodProperty() {
        return thresholdMethod;
    }

    public int getThresholdShape() {
        return thresholdShape.get();
    }

    public IntegerProperty thresholdShapeProperty() {
        return thresholdShape;
    }

    public int getThresholdExtendedShape() {
        return thresholdExtendedShape.get();
    }

    public IntegerProperty thresholdExtendedShapeProperty() {
        return thresholdExtendedShape;
    }

    public AutoThresholder.Method getThresholdMethodExt() {
        return thresholdMethodExt.get();
    }

    public ObjectProperty<AutoThresholder.Method> thresholdMethodExtProperty() {
        return thresholdMethodExt;
    }


    //@todo since those plugins are called in static context think about a change in method call to
    //@todo provide an object of the class (this plugin for example needs threshold parameters
    //@todo either just select one value for the actual shape and add a fixed value for the extended shape
    //@todo or skip extended shape in this case (the whole filament-sensor has to use shape when there is no extended shape
    //@todo in some calculations)


    //@todo plan: implement this plugin, create the adapted brightness-adjuster from imageJ
    //@todo if its done, integrate
    //@todo if its done, switch area calculation before pre-processing
    //@todo maybe store shape in bounding box size not in full image size


    //@todo assumption -> for now it is assumed that the object of this class will be used (currently a new object is used via reflection)


    @Override
    protected List<IBinaryImage> getShapes(ImageProcessor image, IBinaryImage mask, int minArea) {
        IBinaryImage binaryImage = (isManualThresholding()) ?
                getCellImage(image, thresholdShape.getValue()) : getCellImage(image, getThresholdMethod());
        return preProcess(binaryImage, mask, image.getWidth(), image.getHeight(), 3, minArea);
    }

    @Override
    protected List<IBinaryImage> getExtendedShapes(ImageProcessor image, IBinaryImage mask, int minArea) {
        IBinaryImage binaryImage = (isManualThresholding()) ?
                getCellImage(image, thresholdExtendedShape.getValue()) : getCellImage(image, getThresholdMethodExt());
        return preProcess(binaryImage, mask, image.getWidth(), image.getHeight(), 7, minArea);
    }


}
